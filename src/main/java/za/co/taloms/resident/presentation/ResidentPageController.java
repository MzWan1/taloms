package za.co.taloms.resident.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import za.co.taloms.household.application.service.HouseholdService;
import za.co.taloms.resident.application.dto.ResidentRequest;
import za.co.taloms.resident.application.service.ResidentService;
import za.co.taloms.resident.domain.entity.Gender;
import za.co.taloms.resident.domain.entity.RelationshipType;
import java.time.LocalDate;
import java.util.Collections;

@Slf4j
@Controller
@RequestMapping("/residents")
@RequiredArgsConstructor
public class ResidentPageController {

    private final ResidentService residentService;
    private final HouseholdService householdService;

    @GetMapping
    public String list(Model model) {
        try {
            var residents = residentService.findAll();
            model.addAttribute("residents", residents);
            model.addAttribute("totalCount", residentService.countAll());
            model.addAttribute("activeCount", residentService.countActive());
            model.addAttribute("pageTitle", "Resident Management");
            model.addAttribute("currentPage", "residents");
            return "residents/list";
        } catch (Exception e) {
            log.error("Error loading resident list: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading residents: " + e.getMessage());
            model.addAttribute("residents", Collections.emptyList());
            model.addAttribute("totalCount", 0L);
            model.addAttribute("activeCount", 0L);
            model.addAttribute("pageTitle", "Resident Management");
            model.addAttribute("currentPage", "residents");
            return "residents/list";
        }
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        try {
            if (!model.containsAttribute("form")) {
                model.addAttribute("form", ResidentRequest.builder()
                        .dateOfBirth(LocalDate.now().minusYears(18))
                        .build());
            }

            // Get households for dropdown
            var households = householdService.findAll();
            model.addAttribute("households", households);
            model.addAttribute("genders", Gender.values());
            model.addAttribute("relationshipTypes", RelationshipType.values());
            model.addAttribute("pageTitle", "Create Resident");
            model.addAttribute("currentPage", "residents");
            return "residents/create";
        } catch (Exception e) {
            log.error("Error loading create resident form: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading form: " + e.getMessage());
            model.addAttribute("households", Collections.emptyList());
            model.addAttribute("genders", Gender.values());
            model.addAttribute("relationshipTypes", RelationshipType.values());
            model.addAttribute("pageTitle", "Create Resident");
            model.addAttribute("currentPage", "residents");
            return "residents/create";
        }
    }

    @PostMapping("/create")
    public String create(
            @ModelAttribute ResidentRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes ra) {

        log.info("Creating resident - Name: {}, Household: {}",
                request.getFullName(), request.getHouseholdId());

        try {
            var response = residentService.createResident(request, userDetails.getUsername());
            ra.addFlashAttribute("successMessage",
                    "✅ Resident " + response.getFullName() + " created successfully.");
            return "redirect:/residents";

        } catch (Exception e) {
            log.error("Error creating resident: {}", e.getMessage(), e);
            ra.addFlashAttribute("errorMessage", "❌ Error creating resident: " + e.getMessage());
            ra.addFlashAttribute("form", request);
            return "redirect:/residents/create";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        try {
            var resident = residentService.findById(id);
            model.addAttribute("resident", resident);
            model.addAttribute("pageTitle", "Resident - " + resident.getFullName());
            model.addAttribute("currentPage", "residents");
            return "residents/detail";
        } catch (Exception e) {
            log.error("Error loading resident detail: {}", e.getMessage(), e);
            return "redirect:/residents";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        try {
            var resident = residentService.findById(id);

            var form = ResidentRequest.builder()
                    .fullName(resident.getFullName())
                    .idNumber(resident.getIdNumber())
                    .dateOfBirth(resident.getDateOfBirth())
                    .gender(resident.getGender().name())
                    .relationshipType(resident.getRelationshipType().name())
                    .occupation(resident.getOccupation())
                    .contactPhone(resident.getContactPhone())
                    .contactEmail(resident.getContactEmail())
                    .householdId(resident.getHouseholdId())
                    .notes(resident.getNotes())
                    .build();

            var households = householdService.findAll();

            model.addAttribute("resident", resident);
            model.addAttribute("form", form);
            model.addAttribute("households", households);
            model.addAttribute("genders", Gender.values());
            model.addAttribute("relationshipTypes", RelationshipType.values());
            model.addAttribute("pageTitle", "Edit Resident");
            model.addAttribute("currentPage", "residents");
            return "residents/edit";
        } catch (Exception e) {
            log.error("Error loading edit form: {}", e.getMessage(), e);
            return "redirect:/residents/" + id;
        }
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @ModelAttribute ResidentRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes ra) {

        try {
            var response = residentService.updateResident(id, request, userDetails.getUsername());
            ra.addFlashAttribute("successMessage",
                    "✅ Resident " + response.getFullName() + " updated successfully.");
            return "redirect:/residents/" + id;
        } catch (Exception e) {
            log.error("Error updating resident: {}", e.getMessage(), e);
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
            return "redirect:/residents/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes ra) {

        try {
            var response = residentService.deactivateResident(id, userDetails.getUsername());
            ra.addFlashAttribute("successMessage",
                    "✅ Resident deactivated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/residents/" + id;
    }

    @PostMapping("/{id}/activate")
    public String activate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes ra) {

        try {
            var response = residentService.activateResident(id, userDetails.getUsername());
            ra.addFlashAttribute("successMessage",
                    "✅ Resident activated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/residents/" + id;
    }
}