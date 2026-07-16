package za.co.taloms.audit.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import za.co.taloms.audit.application.service.AuditService;
import za.co.taloms.audit.domain.entity.AuditAction;

@Slf4j
@Controller
@RequestMapping("/audit")
@RequiredArgsConstructor
public class AuditPageController {

    private final AuditService auditService;

    @GetMapping
    public String index(Model model) {
        try {
            var logs = auditService.findAll();
            model.addAttribute("logs", logs);
            model.addAttribute("totalCount", auditService.countAll());
            model.addAttribute("actions", AuditAction.values());
            model.addAttribute("pageTitle", "Audit Trail");
            model.addAttribute("currentPage", "audit");
            return "audit/index";
        } catch (Exception e) {
            log.error("Error loading audit page: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading audit logs: " + e.getMessage());
            model.addAttribute("logs", java.util.Collections.emptyList());
            model.addAttribute("totalCount", 0L);
            model.addAttribute("actions", AuditAction.values());
            model.addAttribute("pageTitle", "Audit Trail");
            model.addAttribute("currentPage", "audit");
            return "audit/index";
        }
    }
}