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
import za.co.taloms.household.application.dto.HouseholdResponse;
import za.co.taloms.household.application.service.HouseholdService;
import za.co.taloms.parcel.application.service.ParcelService;
import za.co.taloms.parcel.domain.entity.ParcelStatus;
import za.co.taloms.pto.application.service.PTOService;
import za.co.taloms.pto.domain.entity.PTOStatus;
import za.co.taloms.traditionalauthority.application.service.TraditionalAuthorityService;
import za.co.taloms.traditionalauthority.application.service.VillageService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
            log.info("Loading household list");
            List<HouseholdResponse> households = householdService.findAll();

            // Ensure we never pass null to the template
            if (households == null) {
                households = Collections.emptyList();
            }

            model.addAttribute("households", households);
            model.addAttribute("totalCount", households.size());
            model.addAttribute("activeCount", households.stream().filter(HouseholdResponse::getActive).count());
            model.addAttribute("pageTitle", "Household Management");
            model.addAttribute("currentPage", "households");

            log.info("Found {} households", households.size());
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
            log.info("Loading household create form");

            if (!model.containsAttribute("form")) {
                model.addAttribute("form", HouseholdRequest.builder()
                        .registrationDate(LocalDate.now())
                        .build());
            }

            // Get available parcels (only AVAILABLE parcels)
            List<za.co.taloms.parcel.application.dto.ParcelResponse> availableParcels = new ArrayList<>();
            try {
                var allParcels = parcelService.findAll();
                if (allParcels != null && !allParcels.isEmpty()) {
                    availableParcels = allParcels.stream()
                            .filter(p -> p.getStatus() == ParcelStatus.AVAILABLE)
                            .collect(Collectors.toList());
                    log.info("Found {} available parcels for household creation", availableParcels.size());
                }
            } catch (Exception e) {
                log.error("Error loading available parcels: {}", e.getMessage(), e);
                availableParcels = Collections.emptyList();
            }

            // Get active PTOs to link to households
            List<za.co.taloms.pto.application.dto.PTOResponse> activePtos = new ArrayList<>();
            try {
                activePtos = ptoService.findByStatus(PTOStatus.ACTIVE);
                if (activePtos == null) {
                    activePtos = Collections.emptyList();
                }
                log.info("Found {} active PTOs for household creation", activePtos.size());
            } catch (Exception e) {
                log.error("Error loading active PTOs: {}", e.getMessage(), e);
                activePtos = Collections.emptyList();
            }

            var authorities = authorityService.findAllActive();

            model.addAttribute("authorities", authorities);
            model.addAttribute("availableParcels", availableParcels);
            model.addAttribute("activePtos", activePtos);
            model.addAttribute("pageTitle", "Create Household");
            model.addAttribute("currentPage", "households");
            return "households/create";
        } catch (Exception e) {
            log.error("Error loading create household form: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading form: " + e.getMessage());
            model.addAttribute("availableParcels", Collections.emptyList());
            model.addAttribute("activePtos", Collections.emptyList());
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
            if (request.getParcelId() == null) {
                ra.addFlashAttribute("errorMessage", "❌ Please select a parcel for this household.");
                ra.addFlashAttribute("form", request);
                return "redirect:/households/create";
            }

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
            log.info("Loading household edit form for ID: {}", id);

            var household = householdService.findById(id);
            if (household == null) {
                return "redirect:/households";
            }

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

            // Get available parcels (including the current one)
            List<za.co.taloms.parcel.application.dto.ParcelResponse> availableParcels = new ArrayList<>();
            try {
                var allParcels = parcelService.findAll();
                if (allParcels != null && !allParcels.isEmpty()) {
                    availableParcels = allParcels.stream()
                            .filter(p -> p.getStatus() == ParcelStatus.AVAILABLE ||
                                    p.getId().equals(household.getParcelId()))
                            .collect(Collectors.toList());
                    log.info("Found {} available parcels for household edit", availableParcels.size());
                }
            } catch (Exception e) {
                log.error("Error loading available parcels: {}", e.getMessage(), e);
                availableParcels = Collections.emptyList();
            }

            List<za.co.taloms.pto.application.dto.PTOResponse> activePtos = new ArrayList<>();
            try {
                activePtos = ptoService.findByStatus(PTOStatus.ACTIVE);
                if (activePtos == null) {
                    activePtos = Collections.emptyList();
                }
            } catch (Exception e) {
                log.error("Error loading active PTOs: {}", e.getMessage(), e);
                activePtos = Collections.emptyList();
            }

            model.addAttribute("household", household);
            model.addAttribute("form", form);
            model.addAttribute("availableParcels", availableParcels);
            model.addAttribute("activePtos", activePtos);
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
            if (request.getParcelId() == null) {
                ra.addFlashAttribute("errorMessage", "❌ Please select a parcel for this household.");
                ra.addFlashAttribute("form", request);
                return "redirect:/households/" + id + "/edit";
            }

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
            ra.addFlashAttribute("successMessage", "✅ Household deactivated successfully.");
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
            ra.addFlashAttribute("successMessage", "✅ Household activated successfully.");
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

    @GetMapping("/debug")
    @ResponseBody
    public String debug() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Household Module Debug ===\n");

            // Check if service is working
            try {
                var households = householdService.findAll();
                sb.append("Service: OK\n");
                sb.append("Household count: ").append(households != null ? households.size() : 0).append("\n");
            } catch (Exception e) {
                sb.append("Service Error: ").append(e.getMessage()).append("\n");
            }

            // Check template availability
            sb.append("\n=== Template Check ===\n");
            try {
                // This will force template resolution
                return "households/list";
            } catch (Exception e) {
                sb.append("Template Error: ").append(e.getMessage()).append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            return "Debug Error: " + e.getMessage();
        }
    }

    @GetMapping("/test")
    @ResponseBody
    public String test() {
        try {
            return "Household module is working! Time: " + java.time.LocalDateTime.now();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}