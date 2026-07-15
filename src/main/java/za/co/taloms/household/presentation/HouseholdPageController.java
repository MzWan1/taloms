package za.co.taloms.household.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import za.co.taloms.household.application.dto.HouseholdRequest;
import za.co.taloms.household.application.service.HouseholdService;
import za.co.taloms.parcel.application.service.ParcelService;
import za.co.taloms.parcel.domain.entity.ParcelStatus;
import za.co.taloms.pto.application.service.PTOService;
import za.co.taloms.traditionalauthority.application.service.TraditionalAuthorityService;
import za.co.taloms.traditionalauthority.application.service.VillageService;
import java.time.LocalDate;
import java.util.Collections;

@Slf4j
@Controller
@RequestMapping("/households")
@RequiredArgsConstructor
public class HouseholdPageController {

    private final HouseholdService householdService;
    private final ParcelService parcelService;
    private final PTOService ptoService;
    private final TraditionalAuthorityService authorityService;
    private final VillageService villageService;

    @GetMapping
    public String list(Model model) {
        try {
            var households = householdService.findAll();
            model.addAttribute("households", households);
            model.addAttribute("totalCount", householdService.countAll());
            model.addAttribute("activeCount", householdService.countActive());
            model.addAttribute("pageTitle", "Household Management");
            model.addAttribute("currentPage", "households");
            return "households/list";
        } catch (Exception e) {
            log.error("Error loading household list: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading households: " + e.getMessage());
            model.addAttribute("households", Collections.emptyList());
            model.addAttribute("totalCount", 0L);
            model.addAttribute("activeCount", 0L);
            model.addAttribute("pageTitle", "Household Management");
            model.addAttribute("currentPage", "households");
            return "households/list";
        }
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        try {
            var authorities = authorityService.findAllActive();
            log.info("Loaded {} active authorities for household create form", authorities.size());

            if (!model.containsAttribute("form")) {
                model.addAttribute("form", HouseholdRequest.builder()
                        .registrationDate(LocalDate.now())
                        .build());
            }

            // Get available parcels for dropdown
            var availableParcels = parcelService.findAvailable(0L); // Get all available
            model.addAttribute("authorities", authorities);
            model.addAttribute("availableParcels", availableParcels);
            model.addAttribute("pageTitle", "Create Household");
            model.addAttribute("currentPage", "households");
            return "households/create";
        } catch (Exception e) {
            log.error("Error loading create household form: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading form: " + e.getMessage());
            model.addAttribute("authorities", Collections.emptyList());
            model.addAttribute("availableParcels", Collections.emptyList());
            model.addAttribute("pageTitle", "Create Household");
            model.addAttribute("currentPage", "households");
            return "households/create";
        }
    }

    @PostMapping("/create")
    public String create(
            @ModelAttribute HouseholdRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes ra) {

        log.info("Creating household - Head: {}, Parcel: {}",
                request.getHouseholdHeadName(), request.getParcelId());

        try {
            var response = householdService.createHousehold(request, userDetails.getUsername());
            ra.addFlashAttribute("successMessage",
                    "✅ Household created successfully for " + response.getHouseholdHeadName() + ".");
            return "redirect:/households";

        } catch (Exception e) {
            log.error("Error creating household: {}", e.getMessage(), e);
            ra.addFlashAttribute("errorMessage", "❌ Error creating household: " + e.getMessage());
            ra.addFlashAttribute("form", request);
            return "redirect:/households/create";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        try {
            var household = householdService.findById(id);
            model.addAttribute("household", household);
            model.addAttribute("pageTitle", "Household - " + household.getHouseholdHeadName());
            model.addAttribute("currentPage", "households");
            return "households/detail";
        } catch (Exception e) {
            log.error("Error loading household detail: {}", e.getMessage(), e);
            return "redirect:/households";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        try {
            var household = householdService.findById(id);

            var form = HouseholdRequest.builder()
                    .householdHeadName(household.getHouseholdHeadName())
                    .householdHeadIdNumber(household.getHouseholdHeadIdNumber())
                    .contactPhone(household.getContactPhone())
                    .contactEmail(household.getContactEmail())
                    .parcelId(household.getParcelId())
                    .ptoId(household.getPtoId())
                    .registrationDate(household.getRegistrationDate())
                    .notes(household.getNotes())
                    .build();

            var authorities = authorityService.findAllActive();
            var availableParcels = parcelService.findAvailable(0L);

            model.addAttribute("household", household);
            model.addAttribute("form", form);
            model.addAttribute("authorities", authorities);
            model.addAttribute("availableParcels", availableParcels);
            model.addAttribute("pageTitle", "Edit Household");
            model.addAttribute("currentPage", "households");
            return "households/edit";
        } catch (Exception e) {
            log.error("Error loading edit form: {}", e.getMessage(), e);
            return "redirect:/households/" + id;
        }
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @ModelAttribute HouseholdRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes ra) {

        try {
            var response = householdService.updateHousehold(id, request, userDetails.getUsername());
            ra.addFlashAttribute("successMessage",
                    "✅ Household updated successfully for " + response.getHouseholdHeadName() + ".");
            return "redirect:/households/" + id;
        } catch (Exception e) {
            log.error("Error updating household: {}", e.getMessage(), e);
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
            return "redirect:/households/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes ra) {

        try {
            var response = householdService.deactivateHousehold(id, userDetails.getUsername());
            ra.addFlashAttribute("successMessage",
                    "✅ Household deactivated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/households/" + id;
    }

    @PostMapping("/{id}/activate")
    public String activate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes ra) {

        try {
            var response = householdService.activateHousehold(id, userDetails.getUsername());
            ra.addFlashAttribute("successMessage",
                    "✅ Household activated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/households/" + id;
    }

    @GetMapping("/parcel/{parcelId}/active")
    @ResponseBody
    public Object getActiveHousehold(@PathVariable Long parcelId) {
        try {
            var household = householdService.findActiveByParcelId(parcelId);
            return household != null ? household : Collections.emptyMap();
        } catch (Exception e) {
            log.error("Error loading active household: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }
}