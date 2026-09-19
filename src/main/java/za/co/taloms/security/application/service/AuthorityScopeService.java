package za.co.taloms.security.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.taloms.security.domain.entity.User;
import za.co.taloms.security.domain.repository.UserRepositoryPort;
import za.co.taloms.traditionalauthority.application.service.VillageService;
import za.co.taloms.traditionalauthority.domain.repository.TraditionalAuthorityRepositoryPort;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves which Traditional Authorities the currently authenticated user may
 * access, and enforces data-scoping rules:
 *
 *  - ADMIN     : unrestricted access to all authorities.
 *  - CHIEF     : scoped to every authority linked via the chief_authorities
 *                join table (a chief may belong to many authorities).
 *  - HEADSMAN  : scoped to their single linked authority (user.traditionalAuthorityId
 *                or authority.headmanId) — a headsman may only serve one authority.
 *  - Unlinked  : chief/headman with no authority gets access to nothing.
 *
 * All authorization checks are derived here so that a client-supplied
 * authorityId/chiefId/villageId can never widen a chief's scope.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorityScopeService {

    public static final String ROLE_ADMIN    = "ROLE_ADMIN";
    public static final String ROLE_CHIEF    = "ROLE_CHIEF";
    public static final String ROLE_HEADSMAN = "ROLE_HEADSMAN";
    public static final String ROLE_USER     = "ROLE_USER";

    private final UserRepositoryPort                  userRepository;
    private final TraditionalAuthorityRepositoryPort authorityRepository;
    private final VillageService                      villageService;

    /** Returns the currently authenticated User entity, or null if unauthenticated. */
    @Transactional(readOnly = true)
    public User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }

    public boolean isAdmin(User user) {
        return user != null && user.getRoles().stream()
                .anyMatch(r -> ROLE_ADMIN.equals(r.getName()));
    }

    /** True if the current user is an ADMIN. */
    public boolean isCurrentUserAdmin() {
        return isAdmin(getCurrentUser());
    }

    /** True if the current user holds CHIEF or HEADSMAN role. */
    public boolean isCurrentUserChiefOrHeadsman() {
        User user = getCurrentUser();
        return hasRole(user, ROLE_CHIEF) || hasRole(user, ROLE_HEADSMAN);
    }

    /** True if the current user holds the ROLE_USER (resident) role. */
    public boolean isCurrentUserUser() {
        return hasRole(getCurrentUser(), ROLE_USER);
    }

    /** True if the current user holds the ROLE_CHIEF role. */
    public boolean isCurrentUserChief() {
        return hasRole(getCurrentUser(), ROLE_CHIEF);
    }

    private boolean hasRole(User user, String roleName) {
        return user != null && user.getRoles().stream()
                .anyMatch(r -> roleName.equals(r.getName()));
    }

    /**
     * Resolves ALL Traditional Authority IDs the current user is linked to.
     * Administrators and unauthenticated users return an empty set.
     *
     * Sources (a user's scope is the union):
     *  - chief_authorities join-table rows (many-to-many chiefs);
     *  - for non-chiefs, the legacy single users.traditional_authority_id link
     *    (headsmen and any other legacy links);
     *  - for non-chiefs, the authority-side chiefId/headmanId link.
     */
    @Transactional(readOnly = true)
    public Set<Long> getCurrentUserAuthorityIds() {
        User user = getCurrentUser();
        if (user == null || isAdmin(user) || user.getId() == null) {
            return Set.of();
        }

        Set<Long> ids = new HashSet<>();
        java.util.List<Long> joined =
                userRepository.findAuthorityIdsByUserId(user.getId());
        if (joined != null) {
            ids.addAll(joined);
        }

        boolean isChief = hasRole(user, ROLE_CHIEF);
        boolean isHeadsman = hasRole(user, ROLE_HEADSMAN);

        // Legacy single-authority link (user.traditionalAuthorityId) — only for
        // headsmen and other non-chief roles; chiefs now use the join table.
        if (!isChief || isHeadsman) {
            if (user.getTraditionalAuthorityId() != null) {
                ids.add(user.getTraditionalAuthorityId());
            }
        }
        // Authority-side link (chief_id / headman_id on the authority row).
        // Always consulted so legacy chiefs (before join-table migration) and
        // headsmen remain correctly scoped.
        java.util.List<Long> authoritySide =
                authorityRepository.findIdsByChiefIdOrHeadmanId(user.getId());
        if (authoritySide != null) {
            ids.addAll(authoritySide);
        }
        return ids;
    }

    /**
     * Primary authority of the current chief/headsman (the first/most relevant
     * one), or null. Retained for callers that genuinely need a single context
     * (e.g. a redirect); authorization must use {@link #getCurrentUserAuthorityIds()}.
     */
    @Transactional(readOnly = true)
    public Long getCurrentUserAuthorityId() {
        return getCurrentUserAuthorityIds().stream().findFirst().orElse(null);
    }

    /**
     * Returns true if the current user may view data belonging to the given
     * authority. ADMINs always may; chief/headman only for their own
     * linked authority.
     */
    @Transactional(readOnly = true)
    public boolean canAccessAuthority(Long authorityId) {
        if (authorityId == null) {
            return false;
        }
        User user = getCurrentUser();
        if (user == null) {
            return false;
        }
        if (isAdmin(user)) {
            return true;
        }
        return getCurrentUserAuthorityIds().contains(authorityId);
    }

    /** Throws SecurityException if the current user may not access the authority. */
    public void requireAuthorityAccess(Long authorityId) {
        if (!canAccessAuthority(authorityId)) {
            throw new SecurityException(
                    "You are not authorized to access authority id: " + authorityId);
        }
    }

    /**
     * Returns the IDs of the villages the current chief/headman may manage,
     * or {@code null} for unrestricted users (ADMIN etc.).
     *
     * Scoping rule:
     *  - villages belonging to the user's linked authority (if any), AND
     *  - villages the user is the headman of directly (village.headmanId == user.id).
     *
     * This accommodates an authority that has multiple villages and multiple
     * headsmen, where a headsman may be linked to a village without an
     * authority-level link.
     */
    @Transactional(readOnly = true)
    public Set<Long> scopedVillageIds() {
        if (!isCurrentUserChiefOrHeadsman()) {
            return null; // unrestricted (admin etc.)
        }
        Set<Long> ids = new HashSet<>();

        for (Long authorityId : getCurrentUserAuthorityIds()) {
            ids.addAll(villageService.findByAuthority(authorityId).stream()
                    .map(v -> v.getId())
                    .collect(Collectors.toSet()));
        }

        User currentUser = getCurrentUser();
        if (currentUser != null && currentUser.getId() != null) {
            ids.addAll(villageService.findByHeadmanId(currentUser.getId()).stream()
                    .map(v -> v.getId())
                    .collect(Collectors.toSet()));
        }
        return ids;
    }

    /** True if the current user may access the given village. Admins always may. */
    @Transactional(readOnly = true)
    public boolean canAccessVillage(Long villageId) {
        if (villageId == null) {
            return false;
        }
        User user = getCurrentUser();
        if (user == null) {
            return false;
        }
        if (isAdmin(user)) {
            return true;
        }
        Set<Long> allowed = scopedVillageIds();
        return allowed != null && allowed.contains(villageId);
    }

    /** Throws SecurityException if the current user may not access the village. */
    public void requireVillageAccess(Long villageId) {
        if (!canAccessVillage(villageId)) {
            throw new SecurityException("You are not authorized to access this village.");
        }
    }

    /** Throws SecurityException if the current user is not an ADMIN. */
    public void requireAdmin() {
        if (!isCurrentUserAdmin()) {
            throw new SecurityException("Only administrators may perform this action");
        }
    }
}