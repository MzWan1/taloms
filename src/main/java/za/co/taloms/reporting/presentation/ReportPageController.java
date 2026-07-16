package za.co.taloms.reporting.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import za.co.taloms.parcel.application.service.ParcelService;
import za.co.taloms.pto.application.service.PTOService;
import za.co.taloms.reporting.domain.entity.ReportFormat;
import za.co.taloms.reporting.domain.entity.ReportType;
import za.co.taloms.traditionalauthority.application.service.TraditionalAuthorityService;
import za.co.taloms.traditionalauthority.application.service.VillageService;

@Slf4j
@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportPageController {

    private final PTOService ptoService;
    private final ParcelService parcelService;
    private final TraditionalAuthorityService authorityService;
    private final VillageService villageService;

    @GetMapping
    public String index(Model model) {
        try {
            var authorities = authorityService.findAllActive();
            var villages = villageService.findAll();

            model.addAttribute("authorities", authorities);
            model.addAttribute("villages", villages);
            model.addAttribute("reportTypes", ReportType.values());
            model.addAttribute("reportFormats", ReportFormat.values());

            // KPI Counts for dashboard
            model.addAttribute("totalPTOs", ptoService.countAll());
            model.addAttribute("activePTOs", ptoService.countByStatus(za.co.taloms.pto.domain.entity.PTOStatus.ACTIVE));
            model.addAttribute("totalParcels", parcelService.countAll());
            model.addAttribute("availableParcels", parcelService.countByStatus(za.co.taloms.parcel.domain.entity.ParcelStatus.AVAILABLE));

            model.addAttribute("pageTitle", "Reports");
            model.addAttribute("currentPage", "reports");
            return "reports/index";
        } catch (Exception e) {
            log.error("Error loading reports page: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading reports: " + e.getMessage());
            model.addAttribute("pageTitle", "Reports");
            model.addAttribute("currentPage", "reports");
            return "reports/index";
        }
    }
}