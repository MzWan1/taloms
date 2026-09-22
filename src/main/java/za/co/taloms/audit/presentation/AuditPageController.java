package za.co.taloms.audit.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import za.co.taloms.common.pagination.PageRequestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import za.co.taloms.audit.application.service.AuditService;
import za.co.taloms.audit.domain.entity.AuditAction;

@Slf4j
@Controller
@RequestMapping("/audit")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AuditPageController {

    private final AuditService auditService;

    @GetMapping
    public String index(Model model, @RequestParam(required = false, defaultValue = "1") Integer page) {
        try {

            Pageable pageable = PageRequestUtils.toPageable(page, 10);
            Page<za.co.taloms.audit.application.dto.AuditLogResponse> pageObj = auditService.findAll(pageable);
            model.addAttribute("page", pageObj);
            model.addAttribute("logs", pageObj.getContent());
            model.addAttribute("totalCount", pageObj.getTotalElements());

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
