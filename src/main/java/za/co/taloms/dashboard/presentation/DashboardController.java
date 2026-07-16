package za.co.taloms.dashboard.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import za.co.taloms.dashboard.application.service.DashboardService;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model, Authentication authentication) {
        try {
            var summary = dashboardService.getDashboardSummary();

            model.addAttribute("summary", summary);
            model.addAttribute("totalPtos", summary.getTotalPtos());
            model.addAttribute("activePtos", summary.getActivePtos());
            model.addAttribute("totalParcels", summary.getTotalParcels());
            model.addAttribute("totalResidents", summary.getTotalResidents());
            model.addAttribute("totalHouseholds", summary.getTotalHouseholds());
            model.addAttribute("activeHouseholds", summary.getActiveHouseholds());
            model.addAttribute("totalBusinesses", summary.getTotalBusinesses());
            model.addAttribute("activeBusinesses", summary.getActiveBusinesses());
            model.addAttribute("totalDocuments", summary.getTotalDocuments());
            model.addAttribute("totalUsers", summary.getTotalUsers());
            model.addAttribute("activeUsers", summary.getActiveUsers());
            model.addAttribute("totalAuditLogs", summary.getTotalAuditLogs());
            model.addAttribute("availableParcels", summary.getAvailableParcels());
            model.addAttribute("allocatedParcels", summary.getAllocatedParcels());
            model.addAttribute("recentActivity", summary.getRecentActivity());

            model.addAttribute("pageTitle", "Dashboard");
            model.addAttribute("currentPage", "dashboard");

            return "dashboard/index";
        } catch (Exception e) {
            log.error("Error loading dashboard: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading dashboard: " + e.getMessage());
            return "dashboard/index";
        }
    }

    @GetMapping("/api/dashboard/summary")
    public String getDashboardSummary(Model model) {
        var summary = dashboardService.getDashboardSummary();
        model.addAttribute("summary", summary);
        return "dashboard/summary";
    }
}