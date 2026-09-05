package za.co.taloms.security.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.taloms.security.domain.entity.User;
import za.co.taloms.security.domain.repository.UserRepositoryPort;
import za.co.taloms.traditionalauthority.application.dto.TraditionalAuthorityResponse;
import za.co.taloms.traditionalauthority.application.service.TraditionalAuthorityService;

/**
 * Resolves which Traditional Authority the currently authenticated user
 * belongs to, and enforces data-scoping rules:
 *
 *  - ADMIN      : unrestricted access to all authorities.
 *  - CHIEF      : scoped to their linked authority (either side of the link).
 *  - HEADSMAN   : scoped to their linked authority (either side of the link).
 *  - Unlinked   : chief/headman with no authority gets access to nothing.
 *
 * The user ↔ authority link is honoured in BOTH directions:
 *   - user.traditionalAuthorityId == authorityId, OR
 *   - authority.chiefId == current user's id, OR
 *   - authority.headmanId == current user's id
 * so a stale/missing user-side link cannot lock a chief or headsman out.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorityScopeService {

    public static final String ROLE_ADMIN    = "ROLE_ADMIN";
    public static final String ROLE_CHIEF    = "ROLE_CHIEF";
    public static final String ROLE_HEADSMAN = "ROLE_HEADSMAN";

    private final UserRepositoryPort          userRepository;
    private final TraditionalAuthorityService authorityService;

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
        return user != null && user.getRoles().stream()
                .anyMatch(r -> ROLE_CHIEF.equals(r.getName())
                            || ROLE_HEADSMAN.equals(r.getName()));
    }

    /**
     * Resolves the Traditional Authority ID the current chief/headman is
     * linked to. Returns null for ADMINs, unauthenticated users, or
     * chief/headman with no authority link in either direction.
     */
    @Transactional(readOnly = true)
    public Long getCurrentUserAuthorityId() {
        User user = getCurrentUser();
        if (user == null || isAdmin(user)) {
            return null;
        }

        // Direct user-side link
        if (user.getTraditionalAuthorityId() != null) {
            return user.getTraditionalAuthorityId();
        }

        // Fallback: authority whose chiefId or headmanId is this user
        return authorityService.findAll().stream()
                .filter(a -> user.getId().equals(a.getChiefId())
                          || user.getId().equals(a.getHeadmanId()))
                .map(TraditionalAuthorityResponse::getId)
                .findFirst()
                .orElse(null);
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
        Long linked = getCurrentUserAuthorityId();
        return linked != null && linked.equals(authorityId);
    }

    /** Throws SecurityException if the current user may not access the authority. */
    public void requireAuthorityAccess(Long authorityId) {
        if (!canAccessAuthority(authorityId)) {
            throw new SecurityException(
                    "You are not authorized to access authority id: " + authorityId);
        }
    }

    /** Throws SecurityException if the current user is not an ADMIN. */
    public void requireAdmin() {
        if (!isCurrentUserAdmin()) {
            throw new SecurityException("Only administrators may perform this action");
        }
    }
}