package za.co.taloms.useraccess.application.service;

import za.co.taloms.useraccess.application.dto.PTOAccessResponse;
import za.co.taloms.useraccess.application.dto.UserAccessDto;
import java.util.List;

public interface UserAccessService {

    /** IDs of PTOs this user owns (their SA ID matches the PTO holder's ID). */
    List<Long> findOwnedPtoIds(Long userId);

    /** IDs of PTOs this user can access (owned OR explicitly linked). */
    List<Long> findAccessiblePtoIds(Long userId);

    /** True only if the user owns the PTO (ID-number match). */
    boolean ownsPto(Long userId, Long ptoId);

    /** True if the user owns the PTO or is explicitly linked to it. */
    boolean hasAccess(Long userId, Long ptoId);

    /** Users linked to a PTO — actor must own the PTO. */
    List<PTOAccessResponse> listLinkedUsers(Long ptoId, Long actorId);

    /** Grant a user access to a PTO — actor must own the PTO. */
    void addLinkedUser(Long ptoId, Long targetUserId, Long actorId, String actorUsername);

    /** Revoke a user's access to a PTO — actor must own the PTO, and cannot remove the owner. */
    void removeLinkedUser(Long ptoId, Long targetUserId, Long actorId);

    /** Search linkable accounts (any enabled account with USER or ADMIN role) by name/username/email/id-number. */
    List<UserAccessDto> searchUsers(String query);

    /** Search linkable accounts, excluding the actor and users already linked to the given PTO. */
    List<UserAccessDto> searchLinkableUsers(String query, Long ptoId, Long actorId);
}