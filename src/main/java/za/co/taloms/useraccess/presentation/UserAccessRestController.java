package za.co.taloms.useraccess.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import za.co.taloms.common.ApiResponse;
import za.co.taloms.security.application.service.AuthorityScopeService;
import za.co.taloms.useraccess.application.dto.PTOAccessResponse;
import za.co.taloms.useraccess.application.dto.UserAccessDto;
import za.co.taloms.useraccess.application.service.UserAccessService;

import java.util.List;

@RestController
@RequestMapping("/api/portal/users")
@RequiredArgsConstructor
public class UserAccessRestController {

    private final UserAccessService userAccessService;
    private final AuthorityScopeService scopeService;

    /** Searches linkable accounts so an owner can link a resident to a PTO.
     *  Excludes the owner and anyone already linked to the selected PTO so it is
     *  always clear WHO is being linked TO which PTO. */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ApiResponse<List<UserAccessDto>>> search(
            @RequestParam String q,
            @RequestParam(required = false) Long ptoId) {
        if (q == null || q.trim().length() < 2) {
            return ResponseEntity.ok(ApiResponse.success(
                    List.of(), "Enter at least 2 characters"));
        }
        Long actorId = null;
        try {
            var me = scopeService.getCurrentUser();
            if (me != null) {
                actorId = me.getId();
            }
        } catch (Exception ignored) { /* fall back to unfiltered */ }
        List<UserAccessDto> result = (ptoId != null)
                ? userAccessService.searchLinkableUsers(q, ptoId, actorId)
                : userAccessService.searchUsers(q);
        return ResponseEntity.ok(ApiResponse.success(result, "Search completed"));
    }

    /** Users currently linked to one owned PTO — used to refresh the manage panel in place. */
    @GetMapping("/linked")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ApiResponse<List<PTOAccessResponse>>> linked(
            @RequestParam Long ptoId) {
        Long actorId = null;
        try {
            var me = scopeService.getCurrentUser();
            if (me != null) {
                actorId = me.getId();
            }
        } catch (Exception ignored) { /* fall through to service check */ }
        return ResponseEntity.ok(ApiResponse.success(
                userAccessService.listLinkedUsers(ptoId, actorId), "Linked users fetched"));
    }
}