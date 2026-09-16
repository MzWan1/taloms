package za.co.taloms.useraccess.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import za.co.taloms.pto.application.dto.PTOResponse;
import za.co.taloms.pto.application.service.PTOCertificatePdfGenerator;
import za.co.taloms.pto.application.service.PTOService;
import za.co.taloms.security.application.service.AuthorityScopeService;
import za.co.taloms.security.domain.entity.User;
import za.co.taloms.useraccess.application.service.UserAccessService;

import java.util.List;

/**
 * Self-service resident portal. Two tabs:
 *   1. "My PTOs" - PTOs the user owns (ID match) plus any they are linked to,
 *      each with a password-protected proof-of-residence download.
 *   2. "Manage Access" - for an owned PTO, add/remove linked users.
 *
 * All management actions are gated by ownership so a user can never modify
 * access to a PTO they do not own.
 */
@Slf4j
@Controller
@RequestMapping("/portal")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public class UserPortalController {

    private final UserAccessService          userAccessService;
    private final PTOService                 ptoService;
    private final AuthorityScopeService      scopeService;
    private final PasswordEncoder            passwordEncoder;
    private final PTOCertificatePdfGenerator proofGenerator;

    private User currentUser() {
        return scopeService.getCurrentUser();
    }

    @GetMapping
    public String index(Model model,
                        @RequestParam(required = false) Long ptoId,
                        @RequestParam(required = false, defaultValue = "my") String tab,
                        @RequestParam(required = false) String q) {
        User user = currentUser();
        if (user == null) {
            return "redirect:/login";
        }

        List<PTOResponse> accessible = userAccessService
                .findAccessiblePtoIds(user.getId()).stream()
                .map(ptoService::findById)
                .toList();
        List<PTOResponse> owned = userAccessService
                .findOwnedPtoIds(user.getId()).stream()
                .map(ptoService::findById)
                .toList();

        // Resolve selected PTO for the "Manage Access" tab (must be one the user owns).
        // If a ptoId is present the user clicked a PTO under Manage Access,
        // so stay on that tab instead of bouncing back to "My PTOs".
        if (ptoId != null && "my".equals(tab)) {
            tab = "manage";
        }
        Long selectedId = null;
        PTOResponse selectedPto = null;
        if (ptoId != null && owned.stream().anyMatch(p -> p.getId().equals(ptoId))) {
            selectedId = ptoId;
            selectedPto = owned.stream()
                    .filter(p -> p.getId().equals(ptoId)).findFirst().orElse(null);
        } else if (!owned.isEmpty()) {
            selectedId = owned.get(0).getId();
            selectedPto = owned.get(0);
        }

        model.addAttribute("currentUser", user);
        model.addAttribute("accessiblePtos", accessible);
        model.addAttribute("ownedPtos", owned);
        model.addAttribute("selectedPtoId", selectedId);
        model.addAttribute("selectedPto", selectedPto);
        if (selectedId != null) {
            model.addAttribute("linkedUsers",
                    userAccessService.listLinkedUsers(selectedId, user.getId()));
        } else {
            model.addAttribute("linkedUsers", List.of());
        }
        model.addAttribute("pageTitle", "My Proofs & PTOs");
        model.addAttribute("currentPage", "portal");
        model.addAttribute("activeTab", "manage".equalsIgnoreCase(tab) ? "manage" : "my");
        model.addAttribute("initialSearch", q == null ? "" : q.trim());
        return "portal/index";
    }
@PostMapping("/{ptoId}/access/add")
    public String addLinkedUser(@PathVariable Long ptoId,
                                @RequestParam Long targetUserId,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes ra) {
        User user = currentUser();
        try {
            userAccessService.addLinkedUser(ptoId, targetUserId, user.getId(),
                    userDetails.getUsername());
            String ptoLabel = "PTO #" + ptoId;
            String whoLabel = "user #" + targetUserId;
            try {
                ptoLabel = ptoService.findById(ptoId).getPtoNumber();
            } catch (Exception ignored) { /* keep fallback */ }
            // Resolve linked user's display name for a clear "who -> which PTO" message.
            try {
                whoLabel = userAccessService.listLinkedUsers(ptoId, user.getId()).stream()
                        .filter(u -> u.getUserId().equals(targetUserId))
                        .map(u -> u.getFullName() + " (" + u.getEmail() + ")")
                        .findFirst().orElse(whoLabel);
            } catch (Exception ignored) { /* keep fallback */ }
            ra.addFlashAttribute("successMessage",
                    "Linked " + whoLabel + " to " + ptoLabel + " (by you, " + user.getFullName() + ").");
        } catch (Exception e) {
            log.warn("Failed to add access to PTO {}: {}", ptoId, e.getMessage());
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/portal?ptoId=" + ptoId + "&tab=manage";
    }

    @PostMapping("/{ptoId}/access/remove/{targetUserId}")
    public String removeLinkedUser(@PathVariable Long ptoId,
                                   @PathVariable Long targetUserId,
                                   RedirectAttributes ra) {
        User user = currentUser();
        try {
            // Capture name before removal for a clear message.
            String whoLabel = "user #" + targetUserId;
            try {
                whoLabel = userAccessService.listLinkedUsers(ptoId, user.getId()).stream()
                        .filter(u -> u.getUserId().equals(targetUserId))
                        .map(u -> u.getFullName() + " (" + u.getEmail() + ")")
                        .findFirst().orElse(whoLabel);
            } catch (Exception ignored) { /* keep fallback */ }
            userAccessService.removeLinkedUser(ptoId, targetUserId, user.getId());
            String ptoLabel = "PTO #" + ptoId;
            try {
                ptoLabel = ptoService.findById(ptoId).getPtoNumber();
            } catch (Exception ignored) { /* keep fallback */ }
            ra.addFlashAttribute("successMessage",
                    "Unlinked " + whoLabel + " from " + ptoLabel + ".");
        } catch (Exception e) {
            log.warn("Failed to remove access from PTO {}: {}", ptoId, e.getMessage());
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/portal?ptoId=" + ptoId + "&tab=manage";
    }
/** Password-gate page before downloading a proof of residence. */
    @GetMapping("/proof/{ptoId}")
    public String proofPasswordPage(@PathVariable Long ptoId, Model model) {
        User user = currentUser();
        if (!userAccessService.hasAccess(user.getId(), ptoId)) {
            throw new SecurityException("You do not have access to this PTO.");
        }
        PTOResponse pto = ptoService.findById(ptoId);
        model.addAttribute("pto", pto);
        model.addAttribute("ptoId", ptoId);
        model.addAttribute("pageTitle", "Download Proof of Residence");
        model.addAttribute("currentPage", "portal");
        return "portal/proof";
    }

    /** Verifies the password, then streams the stamped proof of residence. */
    @PostMapping("/proof/{ptoId}")
    public Object downloadProof(@PathVariable Long ptoId,
                                @RequestParam String password,
                                RedirectAttributes ra) {
        User user = currentUser();
        if (!userAccessService.hasAccess(user.getId(), ptoId)) {
            throw new SecurityException("You do not have access to this PTO.");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            ra.addFlashAttribute("errorMessage", "Incorrect password. Nothing was downloaded.");
            return "redirect:/portal/proof/" + ptoId;
        }

        byte[] proof = proofGenerator.generateProofOfResidencePdf(
                ptoId, user.getFullName(), user.getIdNumber());
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition",
                        "inline; filename=\"ProofOfResidence_PTO_" + ptoId + ".pdf\"")
                .body(proof);
    }
}