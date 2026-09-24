package za.co.taloms.dashboard.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import za.co.taloms.dashboard.application.service.DashboardService;
import za.co.taloms.dashboard.application.service.DashboardChartService;
import za.co.taloms.security.application.service.AuthorityScopeService;
import org.springframework.web.bind.annotation.ResponseBody;
import za.co.taloms.traditionalauthority.application.service.TraditionalAuthorityService;

import java.util.Arrays;
import java.util.Collections;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardChartService dashboardChartService;
    private final AuthorityScopeService authorityScopeService;
    private final TraditionalAuthorityService traditionalAuthorityService;

    @GetMapping({ "/", "/dashboard" })
    public String dashboard(Model model, Authentication authentication) {
        if (authentication != null) {
            boolean isCompany = authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_COMPANY".equals(a.getAuthority()));
            if (isCompany) {
                return "redirect:/company";
            }
            boolean isOnlyUser = authentication.getAuthorities().stream()
                    .allMatch(a -> "ROLE_USER".equals(a.getAuthority()));
            if (isOnlyUser) {
                return "redirect:/portal";
            }
        }

        try {
            var summary = dashboardService.getDashboardSummary();
            boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            boolean isChief = authentication != null && authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_CHIEF".equals(a.getAuthority()));
            boolean isHeadsman = authentication != null && authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_HEADSMAN".equals(a.getAuthority()));

            model.addAttribute("summary", summary);
            model.addAttribute("totalPtos", summary.getTotalPtos() != null ? summary.getTotalPtos() : 0L);
            model.addAttribute("activePtos", summary.getActivePtos() != null ? summary.getActivePtos() : 0L);
            model.addAttribute("pendingPtos", summary.getPendingPtos() != null ? summary.getPendingPtos() : 0L);
            model.addAttribute("pendingPtoSummaries",
                    summary.getPendingPtoSummaries() != null ? summary.getPendingPtoSummaries()
                            : Collections.emptyList());
            model.addAttribute("totalParcels", summary.getTotalParcels() != null ? summary.getTotalParcels() : 0L);
            model.addAttribute("totalDocuments",
                    summary.getTotalDocuments() != null ? summary.getTotalDocuments() : 0L);
            model.addAttribute("totalUsers", summary.getTotalUsers() != null ? summary.getTotalUsers() : 0L);
            model.addAttribute("activeUsers", summary.getActiveUsers() != null ? summary.getActiveUsers() : 0L);
            model.addAttribute("totalAuditLogs",
                    isAdmin && summary.getTotalAuditLogs() != null ? summary.getTotalAuditLogs() : 0L);
            
            // Add new fields
            model.addAttribute("totalVillages", summary.getTotalVillages() != null ? summary.getTotalVillages() : 0L);
            model.addAttribute("totalCompanies", summary.getTotalCompanies() != null ? summary.getTotalCompanies() : 0L);
            
            model.addAttribute("showTotalAuthorities", isAdmin);
            model.addAttribute("totalAuthorities", isAdmin ? traditionalAuthorityService.findAll().size() : null);

            var recentActivity = isAdmin ? summary.getRecentActivity() : Collections.emptyList();
            model.addAttribute("recentActivity", recentActivity != null ? recentActivity : Collections.emptyList());

            model.addAttribute("pageTitle", "Dashboard");
            model.addAttribute("currentPage", "dashboard");

            if (isAdmin) {
                return "dashboard/admin";
            } else if (isChief) {
                return "dashboard/chief";
            } else if (isHeadsman) {
                return "dashboard/headsman";
            }
            return "dashboard/index";
        } catch (Exception e) {
            log.error("Error loading dashboard: {}", e.getMessage(), e);

            model.addAttribute("totalPtos", 0L);
            model.addAttribute("activePtos", 0L);
            model.addAttribute("pendingPtos", 0L);
            model.addAttribute("pendingPtoSummaries", Collections.emptyList());
            model.addAttribute("totalParcels", 0L);
            model.addAttribute("totalDocuments", 0L);
            model.addAttribute("totalUsers", 0L);
            model.addAttribute("activeUsers", 0L);
            model.addAttribute("totalAuditLogs", 0L);
            model.addAttribute("totalVillages", 0L);
            model.addAttribute("totalCompanies", 0L);
            model.addAttribute("showTotalAuthorities", false);
            model.addAttribute("totalAuthorities", 0L);
            model.addAttribute("recentActivity", Collections.emptyList());
            model.addAttribute("errorMessage", "Error loading dashboard: " + e.getMessage());
            model.addAttribute("pageTitle", "Dashboard");
            model.addAttribute("currentPage", "dashboard");

            return "dashboard/index";
        }
    }


    @GetMapping("/api/dashboard/chart")
    @ResponseBody
    public DashboardChartDto getChartData(Authentication authentication) {
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        java.util.Set<Long> scopedVillageIds = isAdmin ? null : authorityScopeService.scopedVillageIds();
        return dashboardChartService.getChartData(isAdmin, scopedVillageIds);
    }

    @GetMapping("/api/dashboard/summary")
    public String getDashboardSummary(Model model, Authentication authentication) {
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        try {
            var summary = dashboardService.getDashboardSummary();
            if (!isAdmin && summary != null) {
                summary.setTotalAuditLogs(0L);
                summary.setRecentActivity(Collections.emptyList());
            }
            model.addAttribute("summary", summary);
        } catch (Exception e) {
            log.error("Error loading dashboard summary: {}", e.getMessage(), e);
            model.addAttribute("summary", null);
        }
        return "dashboard/summary";
    }
}
