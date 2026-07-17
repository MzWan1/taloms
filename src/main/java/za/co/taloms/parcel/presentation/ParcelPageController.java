package za.co.taloms.parcel.presentation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import za.co.taloms.parcel.application.dto.BoundaryPointDto;
import za.co.taloms.parcel.application.dto.ParcelRequest;
import za.co.taloms.parcel.application.dto.ParcelResponse;
import za.co.taloms.parcel.application.service.ParcelService;
import za.co.taloms.parcel.domain.entity.ParcelStatus;
import za.co.taloms.parcel.domain.entity.ParcelType;
import za.co.taloms.traditionalauthority.application.service.TraditionalAuthorityService;
import za.co.taloms.traditionalauthority.application.service.VillageService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/parcels")
@RequiredArgsConstructor
public class ParcelPageController {

    private final ParcelService parcelService;
    private final TraditionalAuthorityService authorityService;
    private final VillageService villageService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public String list(Model model) {
        try {
            var parcels = parcelService.findAll();
            model.addAttribute("parcels", parcels);
            model.addAttribute("statuses", ParcelStatus.values());
            model.addAttribute("types", ParcelType.values());
            model.addAttribute("totalCount", parcelService.countAll());
            model.addAttribute("availableCount", parcelService.countByStatus(ParcelStatus.AVAILABLE));
            model.addAttribute("allocatedCount", parcelService.countByStatus(ParcelStatus.ALLOCATED));
            model.addAttribute("disputedCount", parcelService.countByStatus(ParcelStatus.DISPUTED));
            model.addAttribute("pageTitle", "Parcel Management");
            model.addAttribute("currentPage", "parcels");
            return "parcels/list";
        } catch (Exception e) {
            log.error("Error loading parcel list: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading parcels: " + e.getMessage());
            model.addAttribute("parcels", Collections.emptyList());
            model.addAttribute("statuses", ParcelStatus.values());
            model.addAttribute("types", ParcelType.values());
            model.addAttribute("totalCount", 0L);
            model.addAttribute("availableCount", 0L);
            model.addAttribute("allocatedCount", 0L);
            model.addAttribute("disputedCount", 0L);
            model.addAttribute("pageTitle", "Parcel Management");
            model.addAttribute("currentPage", "parcels");
            return "parcels/list";
        }
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        try {
            var authorities = authorityService.findAllActive();
            log.info("Loaded {} active authorities for parcel create form", authorities.size());

            if (!model.containsAttribute("form")) {
                model.addAttribute("form", ParcelRequest.builder().build());
            }

            model.addAttribute("authorities", authorities);
            model.addAttribute("types", ParcelType.values());
            model.addAttribute("statuses", ParcelStatus.values());
            model.addAttribute("pageTitle", "Create Parcel");
            model.addAttribute("currentPage", "parcels");
            return "parcels/create";
        } catch (Exception e) {
            log.error("Error loading create parcel form: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading form: " + e.getMessage());
            model.addAttribute("authorities", Collections.emptyList());
            model.addAttribute("types", ParcelType.values());
            model.addAttribute("statuses", ParcelStatus.values());
            model.addAttribute("pageTitle", "Create Parcel");
            model.addAttribute("currentPage", "parcels");
            return "parcels/create";
        }
    }

    @PostMapping("/create")
    public String create(
            @RequestParam(value = "boundariesJson", required = false, defaultValue = "[]") String boundariesJson,
            @ModelAttribute ParcelRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes ra) {

        log.info("Creating parcel - Stand: {}, Type: {}, Village: {}, Boundaries JSON length: {}",
                request.getStandNumber(), request.getParcelType(), request.getVillageId(),
                boundariesJson != null ? boundariesJson.length() : 0);

        try {
            // Parse the JSON boundaries
            List<BoundaryPointDto> boundaries = parseBoundariesJson(boundariesJson);

            log.info("Parsed {} boundary points from JSON", boundaries != null ? boundaries.size() : 0);

            // Validate boundaries
            if (boundaries == null || boundaries.size() < 3) {
                String errorMsg = "❌ Please capture at least 3 GPS boundary points. Currently: " +
                        (boundaries != null ? boundaries.size() : 0) + " points.";
                log.warn(errorMsg);
                ra.addFlashAttribute("errorMessage", errorMsg);
                ra.addFlashAttribute("form", request);
                return "redirect:/parcels/create";
            }

            // Set boundaries on the request
            request.setBoundaries(boundaries);

            var response = parcelService.createParcel(request, userDetails.getUsername());
            ra.addFlashAttribute("successMessage",
                    "✅ Parcel " + response.getParcelNumber() +
                            " created successfully for stand " + response.getStandNumber() + ".");
            return "redirect:/parcels";

        } catch (JsonProcessingException e) {
            log.error("Error parsing boundaries JSON: {}", e.getMessage(), e);
            ra.addFlashAttribute("errorMessage", "❌ Invalid boundary data format: " + e.getMessage());
            ra.addFlashAttribute("form", request);
            return "redirect:/parcels/create";
        } catch (Exception e) {
            log.error("Error creating parcel: {}", e.getMessage(), e);
            ra.addFlashAttribute("errorMessage", "❌ Error creating parcel: " + e.getMessage());
            ra.addFlashAttribute("form", request);
            return "redirect:/parcels/create";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        try {
            var parcel = parcelService.findById(id);
            model.addAttribute("parcel", parcel);
            model.addAttribute("pageTitle", "Parcel " + parcel.getParcelNumber());
            model.addAttribute("currentPage", "parcels");
            return "parcels/detail";
        } catch (Exception e) {
            log.error("Error loading parcel detail: {}", e.getMessage(), e);
            return "redirect:/parcels";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        try {
            var parcel = parcelService.findById(id);

            var form = ParcelRequest.builder()
                    .standNumber(parcel.getStandNumber())
                    .parcelType(parcel.getParcelType().name())
                    .villageId(parcel.getVillageId())
                    .notes(parcel.getNotes())
                    .boundaries(parcel.getBoundaries())
                    .build();

            var authorities = authorityService.findAllActive();
            var villages = villageService.findByAuthority(parcel.getVillageId());

            model.addAttribute("parcel", parcel);
            model.addAttribute("form", form);
            model.addAttribute("authorities", authorities);
            model.addAttribute("villages", villages);
            model.addAttribute("types", ParcelType.values());
            model.addAttribute("statuses", ParcelStatus.values());
            model.addAttribute("pageTitle", "Edit Parcel " + parcel.getParcelNumber());
            model.addAttribute("currentPage", "parcels");
            return "parcels/edit";
        } catch (Exception e) {
            log.error("Error loading edit form: {}", e.getMessage(), e);
            return "redirect:/parcels/" + id;
        }
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @RequestParam(value = "boundariesJson", required = false, defaultValue = "[]") String boundariesJson,
            @ModelAttribute ParcelRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes ra) {

        try {
            // Parse the JSON boundaries
            List<BoundaryPointDto> boundaries = parseBoundariesJson(boundariesJson);

            // Validate boundaries
            if (boundaries == null || boundaries.size() < 3) {
                ra.addFlashAttribute("errorMessage", "❌ Please provide at least 3 GPS boundary points.");
                ra.addFlashAttribute("form", request);
                return "redirect:/parcels/" + id + "/edit";
            }

            request.setBoundaries(boundaries);

            var response = parcelService.updateParcel(id, request, userDetails.getUsername());
            ra.addFlashAttribute("successMessage",
                    "✅ Parcel " + response.getParcelNumber() + " updated successfully.");
            return "redirect:/parcels/" + id;
        } catch (JsonProcessingException e) {
            log.error("Error parsing boundaries JSON: {}", e.getMessage(), e);
            ra.addFlashAttribute("errorMessage", "❌ Invalid boundary data format: " + e.getMessage());
            return "redirect:/parcels/" + id + "/edit";
        } catch (Exception e) {
            log.error("Error updating parcel: {}", e.getMessage(), e);
            ra.addFlashAttribute("errorMessage", "❌ Error updating parcel: " + e.getMessage());
            return "redirect:/parcels/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/status")
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam ParcelStatus status,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes ra) {

        try {
            var response = parcelService.updateStatus(id, status, userDetails.getUsername());
            ra.addFlashAttribute("successMessage",
                    "✅ Parcel status updated to " + status.getDisplayName() + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/parcels/" + id;
    }

    @GetMapping("/villages/{authorityId}")
    @ResponseBody
    public Object getVillagesByAuthority(@PathVariable Long authorityId) {
        try {
            log.info("Loading villages for authority ID: {}", authorityId);
            var villages = villageService.findByAuthority(authorityId);
            log.info("Found {} villages for authority {}", villages.size(), authorityId);
            return villages;
        } catch (Exception e) {
            log.error("Error loading villages for authority {}: {}", authorityId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Parse the boundaries JSON string into a list of BoundaryPointDto objects
     */
    private List<BoundaryPointDto> parseBoundariesJson(String boundariesJson) throws JsonProcessingException {
        if (boundariesJson == null || boundariesJson.trim().isEmpty() || "[]".equals(boundariesJson.trim())) {
            return new ArrayList<>();
        }

        // Try to parse as List<BoundaryPointDto>
        try {
            return objectMapper.readValue(boundariesJson, new TypeReference<List<BoundaryPointDto>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse boundaries as BoundaryPointDto list", e);
            throw e;
        }
    }
}