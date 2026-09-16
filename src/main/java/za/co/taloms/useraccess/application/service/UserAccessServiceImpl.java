package za.co.taloms.useraccess.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.taloms.common.BusinessValidationException;
import za.co.taloms.common.ResourceNotFoundException;
import za.co.taloms.pto.domain.repository.PTORepositoryPort;
import za.co.taloms.security.domain.entity.User;
import za.co.taloms.security.domain.repository.UserRepositoryPort;
import za.co.taloms.useraccess.application.dto.PTOAccessResponse;
import za.co.taloms.useraccess.application.dto.UserAccessDto;
import za.co.taloms.useraccess.domain.entity.UserPTOAccess;
import za.co.taloms.useraccess.domain.repository.UserPTOAccessRepositoryPort;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enforces the ownership model for proof-of-residence access.
 *
 * Ownership is derived from the SA ID-number match between the user and the PTO
 * holder. Only an owner may manage who is linked to a PTO, and only owners or
 * explicitly-linked users may download proofs. This prevents a user from
 * modifying or accessing PTOs they do not own.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserAccessServiceImpl implements UserAccessService {

    private static final String ROLE_USER = "ROLE_USER";

    private final UserRepositoryPort          userRepository;
    private final PTORepositoryPort           ptoRepository;
    private final UserPTOAccessRepositoryPort accessRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Long> findOwnedPtoIds(Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (user.getIdNumber() == null || user.getIdNumber().isBlank()) {
            return List.of();
        }
        return ptoRepository.findByIdNumber(user.getIdNumber()).stream()
                .filter(p -> !p.isDeleted())
                .map(p -> p.getId())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findAccessiblePtoIds(Long userId) {
        Set<Long> ids = new LinkedHashSet<>(findOwnedPtoIds(userId));

        accessRepository.findAllByUserId(userId).forEach(access -> {
            ptoRepository.findById(access.getPtoId())
                    .filter(p -> !p.isDeleted())
                    .ifPresent(p -> ids.add(p.getId()));
        });

        return new ArrayList<>(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean ownsPto(Long userId, Long ptoId) {
        var user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getIdNumber() == null || user.getIdNumber().isBlank()) {
            return false;
        }
        var pto = ptoRepository.findById(ptoId).orElse(null);
        return pto != null && !pto.isDeleted()
                && user.getIdNumber().equals(pto.getIdNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAccess(Long userId, Long ptoId) {
        return ownsPto(userId, ptoId)
                || accessRepository.existsByUserIdAndPtoId(userId, ptoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PTOAccessResponse> listLinkedUsers(Long ptoId, Long actorId) {
        requireOwner(actorId, ptoId);

        List<PTOAccessResponse> result = new ArrayList<>();

        // The owner always appears first (marked as owner).
        userRepository.findById(actorId).ifPresent(owner ->
                result.add(PTOAccessResponse.builder()
                        .userId(owner.getId())
                        .fullName(owner.getFullName())
                        .email(owner.getEmail())
                        .idNumber(owner.getIdNumber())
                        .owner(true)
                        .build()));

        for (var access : accessRepository.findAllByPtoId(ptoId)) {
            userRepository.findById(access.getUserId()).ifPresent(u ->
                    result.add(PTOAccessResponse.builder()
                            .userId(u.getId())
                            .fullName(u.getFullName())
                            .email(u.getEmail())
                            .idNumber(u.getIdNumber())
                            .owner(false)
                            .build()));
        }
        return result;
    }

    @Override
    public void addLinkedUser(Long ptoId, Long targetUserId, Long actorId, String actorUsername) {
        requireOwner(actorId, ptoId);

        var target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", targetUserId));

        if (ownsPto(targetUserId, ptoId)) {
            throw new BusinessValidationException(
                    target.getFullName() + " already owns this PTO.");
        }
        if (accessRepository.existsByUserIdAndPtoId(targetUserId, ptoId)) {
            throw new BusinessValidationException(
                    target.getFullName() + " is already linked to this PTO.");
        }

        accessRepository.save(UserPTOAccess.builder()
                .userId(targetUserId)
                .ptoId(ptoId)
                .createdBy(actorUsername)
                .build());

        log.info("User {} granted access to PTO {} by owner {}",
                targetUserId, ptoId, actorUsername);
    }
@Override
    public void removeLinkedUser(Long ptoId, Long targetUserId, Long actorId) {
        requireOwner(actorId, ptoId);

        if (ownsPto(targetUserId, ptoId)) {
            throw new BusinessValidationException(
                    "The owner cannot be removed from their own PTO.");
        }
        if (!accessRepository.existsByUserIdAndPtoId(targetUserId, ptoId)) {
            throw new BusinessValidationException(
                    "This user is not linked to the PTO.");
        }

        accessRepository.deleteByUserIdAndPtoId(targetUserId, ptoId);
        log.info("User {} access removed from PTO {} by owner {}",
                targetUserId, ptoId, actorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAccessDto> searchUsers(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return userRepository.searchByNameOrEmail(query.trim()).stream()
                .filter(User::getEnabled)
                .filter(u -> u.getRoles() != null && u.getRoles().stream()
                        .anyMatch(r -> ROLE_USER.equals(r.getName()) || "ROLE_ADMIN".equals(r.getName())))
                .limit(20)
                .map(u -> UserAccessDto.builder()
                        .id(u.getId())
                        .fullName(u.getFullName())
                        .email(u.getEmail())
                        .idNumber(u.getIdNumber())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAccessDto> searchLinkableUsers(String query, Long ptoId, Long actorId) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String q = query.trim();
        // Resolve the selected PTO's holder ID once (no per-candidate DB calls).
        String ptoIdNumber = null;
        if (ptoId != null) {
            try {
                var pto = ptoRepository.findById(ptoId).orElse(null);
                if (pto != null) {
                    ptoIdNumber = pto.getIdNumber();
                }
            } catch (Exception ignored) { /* treat as unknown */ }
        }
        final String holderId = ptoIdNumber;
        return userRepository.searchByNameOrEmail(q).stream()
                .filter(User::getEnabled)
                .filter(u -> u.getRoles() != null && u.getRoles().stream()
                        .anyMatch(r -> ROLE_USER.equals(r.getName()) || "ROLE_ADMIN".equals(r.getName())))
                // Never offer yourself — you already have access as the owner.
                .filter(u -> actorId == null || !u.getId().equals(actorId))
                // Hide anyone already linked to this PTO.
                .filter(u -> ptoId == null || !accessRepository.existsByUserIdAndPtoId(u.getId(), ptoId))
                // Hide anyone who already owns this PTO (ID-number match).
                .filter(u -> holderId == null || holderId.isBlank()
                        || u.getIdNumber() == null || u.getIdNumber().isBlank()
                        || !u.getIdNumber().equals(holderId))
                .limit(20)
                .map(u -> UserAccessDto.builder()
                        .id(u.getId())
                        .fullName(u.getFullName())
                        .email(u.getEmail())
                        .idNumber(u.getIdNumber())
                        .build())
                .collect(Collectors.toList());
    }

    private void requireOwner(Long actorId, Long ptoId) {
        if (!ownsPto(actorId, ptoId)) {
            throw new SecurityException(
                    "You are not the owner of this PTO and may not modify its access.");
        }
    }
}