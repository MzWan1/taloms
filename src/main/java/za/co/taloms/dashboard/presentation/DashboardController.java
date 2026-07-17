package za.co.taloms.dashboard.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import za.co.taloms.dashboard.application.service.DashboardService;
import java.util.Collections;

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
            model.addAttribute("totalPtos", summary.getTotalPtos() != null ? summary.getTotalPtos() : 0L);
            model.addAttribute("activePtos", summary.getActivePtos() != null ? summary.getActivePtos() : 0L);
            model.addAttribute("totalParcels", summary.getTotalParcels() != null ? summary.getTotalParcels() : 0L);
            model.addAttribute("totalResidents", summary.getTotalResidents() != null ? summary.getTotalResidents() : 0L);
            model.addAttribute("totalHouseholds", summary.getTotalHouseholds() != null ? summary.getTotalHouseholds() : 0L);
            model.addAttribute("activeHouseholds", summary.getActiveHouseholds() != null ? summary.getActiveHouseholds() : 0L);
            model.addAttribute("totalBusinesses", summary.getTotalBusinesses() != null ? summary.getTotalBusinesses() : 0L);
            model.addAttribute("activeBusinesses", summary.getActiveBusinesses() != null ? summary.getActiveBusinesses() : 0L);
            model.addAttribute("totalDocuments", summary.getTotalDocuments() != null ? summary.getTotalDocuments() : 0L);
            model.addAttribute("totalUsers", summary.getTotalUsers() != null ? summary.getTotalUsers() : 0L);
            model.addAttribute("activeUsers", summary.getActiveUsers() != null ? summary.getActiveUsers() : 0L);
            model.addAttribute("totalAuditLogs", summary.getTotalAuditLogs() != null ? summary.getTotalAuditLogs() : 0L);
            model.addAttribute("availableParcels", summary.getAvailableParcels() != null ? summary.getAvailableParcels() : 0L);
            model.addAttribute("allocatedParcels", summary.getAllocatedParcels() != null ? summary.getAllocatedParcels() : 0L);

            // Ensure recentActivity is never null
            var recentActivity = summary.getRecentActivity();
            model.addAttribute("recentActivity", recentActivity != null ? recentActivity : Collections.emptyList());

            model.addAttribute("pageTitle", "Dashboard");
            model.addAttribute("currentPage", "dashboard");

            return "dashboard/index";
        } catch (Exception e) {
            log.error("Error loading dashboard: {}", e.getMessage(), e);

            // Set default values to avoid template errors
            model.addAttribute("totalPtos", 0L);
            model.addAttribute("activePtos", 0L);
            model.addAttribute("totalParcels", 0L);
            model.addAttribute("totalResidents", 0L);
            model.addAttribute("totalHouseholds", 0L);
            model.addAttribute("activeHouseholds", 0L);
            model.addAttribute("totalBusinesses", 0L);
            model.addAttribute("activeBusinesses", 0L);
            model.addAttribute("totalDocuments", 0L);
            model.addAttribute("totalUsers", 0L);
            model.addAttribute("activeUsers", 0L);
            model.addAttribute("totalAuditLogs", 0L);
            model.addAttribute("availableParcels", 0L);
            model.addAttribute("allocatedParcels", 0L);
            model.addAttribute("recentActivity", Collections.emptyList());
            model.addAttribute("errorMessage", "Error loading dashboard: " + e.getMessage());
            model.addAttribute("pageTitle", "Dashboard");
            model.addAttribute("currentPage", "dashboard");

            return "dashboard/index";
        }
    }

    @GetMapping("/api/dashboard/summary")
    public String getDashboardSummary(Model model) {
        try {
            var summary = dashboardService.getDashboardSummary();
            model.addAttribute("summary", summary);
        } catch (Exception e) {
            log.error("Error loading dashboard summary: {}", e.getMessage(), e);
            model.addAttribute("summary", null);
        }
        return "dashboard/summary";
    }
}