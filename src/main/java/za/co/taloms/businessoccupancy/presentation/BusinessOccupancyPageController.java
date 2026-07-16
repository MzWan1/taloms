package za.co.taloms.businessoccupancy.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import za.co.taloms.businessoccupancy.application.dto.BusinessOccupancyRequest;
import za.co.taloms.businessoccupancy.application.service.BusinessOccupancyService;
import za.co.taloms.businessoccupancy.domain.entity.BusinessStatus;
import za.co.taloms.businessoccupancy.domain.entity.BusinessType;
import za.co.taloms.parcel.application.service.ParcelService;
import za.co.taloms.parcel.domain.entity.ParcelStatus;
import za.co.taloms.pto.application.service.PTOService;
import za.co.taloms.pto.domain.entity.PTOStatus;
import za.co.taloms.traditionalauthority.application.service.TraditionalAuthorityService;
import java.util.Collections;

@Slf4j
@Controller
@RequestMapping("/business-occupancies")
@RequiredArgsConstructor
public class BusinessOccupancyPageController {

    private final BusinessOccupancyService businessService;
    private final ParcelService parcelService;
    private final PTOService ptoService;
    private final TraditionalAuthorityService authorityService;

    @GetMapping
    public String list(Model model) {
        try {
            var businesses = businessService.findAll();
            model.addAttribute("businesses", businesses);
            model.addAttribute("statuses", BusinessStatus.values());
            model.addAttribute("types", BusinessType.values());
            model.addAttribute("totalCount", businessService.countAll());
            model.addAttribute("activeCount", businessService.countByStatus(BusinessStatus.ACTIVE));
            model.addAttribute("pendingCount", businessService.countByStatus(BusinessStatus.PENDING));
            model.addAttribute("pageTitle", "Business Occupancy Management");
            model.addAttribute("currentPage", "business-occupancies");
            return "business-occupancies/list";
        } catch (Exception e) {
            log.error("Error loading business list: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading businesses: " + e.getMessage());
            model.addAttribute("businesses", Collections.emptyList());
            model.addAttribute("statuses", BusinessStatus.values());
            model.addAttribute("types", BusinessType.values());
            model.addAttribute("totalCount", 0L);
            model.addAttribute("activeCount", 0L);
            model.addAttribute("pendingCount", 0L);
            model.addAttribute("pageTitle", "Business Occupancy Management");
            model.addAttribute("currentPage", "business-occupancies");
            return "business-occupancies/list";
        }
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        try {
            var authorities = authorityService.findAllActive();
            log.info("Loaded {} active authorities for business create form", authorities.size());

            if (!model.containsAttribute("form")) {
                model.addAttribute("form", BusinessOccupancyRequest.builder()
                        .employeesCount(0)
                        .build());
            }

            // Get available parcels (Available status)
            var availableParcels = parcelService.findByStatus(ParcelStatus.AVAILABLE);

            // Get active PTOs for dropdown
            var activePtos = ptoService.findByStatus(PTOStatus.ACTIVE);
            log.info("Loaded {} active PTOs for business create form", activePtos.size());

            model.addAttribute("authorities", authorities);
            model.addAttribute("availableParcels", availableParcels);
            model.addAttribute("activePtos", activePtos);
            model.addAttribute("types", BusinessType.values());
            model.addAttribute("statuses", BusinessStatus.values());
            model.addAttribute("pageTitle", "Create Business Occupancy");
            model.addAttribute("currentPage", "business-occupancies");
            return "business-occupancies/create";
        } catch (Exception e) {
            log.error("Error loading create business form: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading form: " + e.getMessage());
            model.addAttribute("authorities", Collections.emptyList());
            model.addAttribute("availableParcels", Collections.emptyList());
            model.addAttribute("activePtos", Collections.emptyList());
            model.addAttribute("types", BusinessType.values());
            model.addAttribute("statuses", BusinessStatus.values());
            model.addAttribute("pageTitle", "Create Business Occupancy");
            model.addAttribute("currentPage", "business-occupancies");
            return "business-occupancies/create";
        }
    }

    @PostMapping("/create")
    public String create(
            @ModelAttribute BusinessOccupancyRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes ra) {

        log.info("Creating business - Name: {}, Parcel: {}, PTO: {}",
                request.getBusinessName(), request.getParcelId(), request.getPtoId());

        try {
            var response = businessService.createOccupancy(request, userDetails.getUsername());
            ra.addFlashAttribute("successMessage",
                    "✅ Business " + response.getBusinessName() + " registered successfully.");
            return "redirect:/business-occupancies";

        } catch (Exception e) {
            log.error("Error creating business: {}", e.getMessage(), e);
            ra.addFlashAttribute("errorMessage", "❌ Error creating business: " + e.getMessage());
            ra.addFlashAttribute("form", request);
            return "redirect:/business-occupancies/create";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        try {
            var business = businessService.findById(id);
            model.addAttribute("business", business);
            model.addAttribute("pageTitle", "Business - " + business.getBusinessName());
            model.addAttribute("currentPage", "business-occupancies");
            return "business-occupancies/detail";
        } catch (Exception e) {
            log.error("Error loading business detail: {}", e.getMessage(), e);
            return "redirect:/business-occupancies";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        try {
            var business = businessService.findById(id);

            var form = BusinessOccupancyRequest.builder()
                    .businessName(business.getBusinessName())
                    .registrationNumber(business.getRegistrationNumber())
                    .businessType(business.getBusinessType().name())
                    .ownerName(business.getOwnerName())
                    .ownerIdNumber(business.getOwnerIdNumber())
                    .contactPhone(business.getContactPhone())
                    .contactEmail(business.getContactEmail())
                    .parcelId(business.getParcelId())
                    .ptoId(business.getPtoId())
                    .operatingHours(business.getOperatingHours())
                    .employeesCount(business.getEmployeesCount())
                    .notes(business.getNotes())
                    .build();

            var authorities = authorityService.findAllActive();
            var availableParcels = parcelService.findByStatus(ParcelStatus.AVAILABLE);
            var activePtos = ptoService.findByStatus(PTOStatus.ACTIVE);

            model.addAttribute("business", business);
            model.addAttribute("form", form);
            model.addAttribute("authorities", authorities);
            model.addAttribute("availableParcels", availableParcels);
            model.addAttribute("activePtos", activePtos);
            model.addAttribute("types", BusinessType.values());
            model.addAttribute("statuses", BusinessStatus.values());
            model.addAttribute("pageTitle", "Edit Business");
            model.addAttribute("currentPage", "business-occupancies");
            return "business-occupancies/edit";
        } catch (Exception e) {
            log.error("Error loading edit form: {}", e.getMessage(), e);
            return "redirect:/business-occupancies/" + id;
        }
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @ModelAttribute BusinessOccupancyRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes ra) {

        try {
            var response = businessService.updateOccupancy(id, request, userDetails.getUsername());
            ra.addFlashAttribute("successMessage",
                    "✅ Business " + response.getBusinessName() + " updated successfully.");
            return "redirect:/business-occupancies/" + id;
        } catch (Exception e) {
            log.error("Error updating business: {}", e.getMessage(), e);
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
            return "redirect:/business-occupancies/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/status")
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam BusinessStatus status,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes ra) {

        try {
            var response = businessService.updateStatus(id, status, userDetails.getUsername());
            ra.addFlashAttribute("successMessage",
                    "✅ Business status updated to " + status.getDisplayName() + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/business-occupancies/" + id;
    }

    @PostMapping("/{id}/activate")
    public String activate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes ra) {

        try {
            var response = businessService.activateOccupancy(id, userDetails.getUsername());
            ra.addFlashAttribute("successMessage",
                    "✅ Business activated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/business-occupancies/" + id;
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes ra) {

        try {
            var response = businessService.deactivateOccupancy(id, userDetails.getUsername());
            ra.addFlashAttribute("successMessage",
                    "✅ Business deactivated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/business-occupancies/" + id;
    }
}