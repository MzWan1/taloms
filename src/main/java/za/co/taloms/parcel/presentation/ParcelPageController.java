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
import za.co.taloms.parcel.domain.entity.CaptureMode;
import za.co.taloms.parcel.domain.entity.ParcelStatus;
import za.co.taloms.security.application.service.AuthorityScopeService;
import za.co.taloms.traditionalauthority.application.dto.TraditionalAuthorityResponse;
import za.co.taloms.traditionalauthority.application.dto.VillageResponse;
import za.co.taloms.traditionalauthority.application.service.TraditionalAuthorityService;
import za.co.taloms.traditionalauthority.application.service.VillageService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/parcels")
@RequiredArgsConstructor
public class ParcelPageController {

    private final ParcelService parcelService;
    private final TraditionalAuthorityService authorityService;
    private final VillageService villageService;
    private final AuthorityScopeService scopeService;
    private final ObjectMapper objectMapper;

    /** Returns the village IDs the current chief/headman may manage, or null if unrestricted (admin). */
    private Set<Long> scopedVillageIds() {
        return scopeService.scopedVillageIds();
    }

    /** Throws SecurityException if the given village is outside the current user's scope. */
    private void requireVillageAccess(Long villageId) {
        if (villageId == null) {
            throw new SecurityException("A village must be selected.");
        }
        scopeService.requireVillageAccess(villageId);
    }

    /** Throws SecurityException if the given parcel is outside the current user's scope. */
    private void requireParcelAccess(Long parcelId) {
        var parcel = parcelService.findById(parcelId);
        requireVillageAccess(parcel.getVillageId());
    }

    @GetMapping
    public String list(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "status", required = false) ParcelStatus status,
            @RequestParam(value = "villageId", required = false) Long villageId,
            Model model) {
        try {
            Set<Long> allowedVillageIds = scopedVillageIds();

            List<ParcelResponse> parcels = parcelService.findAll();
            if (q != null && !q.trim().isEmpty()) {
                parcels = parcelService.search(q.trim());
            }
            if (status != null) {
                parcels = parcels.stream()
                        .filter(p -> status.equals(p.getStatus()))
                        .toList();
            }
            if (villageId != null) {
                parcels = parcels.stream()
                        .filter(p -> villageId.equals(p.getVillageId()))
                        .toList();
            }
            if (allowedVillageIds != null) {
                // Chiefs/headsmen only see parcels in their authority's villages
                parcels = parcels.stream()
                        .filter(p -> p.getVillageId() != null
                                && allowedVillageIds.contains(p.getVillageId()))
                        .toList();
            }

            long availableCount = parcels.stream()
                    .filter(p -> p.getStatus() == ParcelStatus.AVAILABLE).count();
            long allocatedCount = parcels.stream()
                    .filter(p -> p.getStatus() == ParcelStatus.ALLOCATED).count();
            long disputedCount = parcels.stream()
                    .filter(p -> p.getStatus() == ParcelStatus.DISPUTED).count();

            model.addAttribute("parcels", parcels);
            model.addAttribute("q", q);
            model.addAttribute("selectedStatus", status);
            model.addAttribute("selectedVillageId", villageId);
            model.addAttribute("statuses", ParcelStatus.values());
            model.addAttribute("totalCount", (long) parcels.size());
            model.addAttribute("availableCount", availableCount);
            model.addAttribute("allocatedCount", allocatedCount);
            model.addAttribute("disputedCount", disputedCount);
            model.addAttribute("pageTitle", "Parcel Management");
            model.addAttribute("currentPage", "parcels");
            return "parcels/list";
        } catch (Exception e) {
            log.error("Error loading parcel list: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading parcels: " + e.getMessage());
            model.addAttribute("parcels", Collections.emptyList());
            model.addAttribute("statuses", ParcelStatus.values());
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
            List<VillageResponse> villages;
            boolean creationBlocked = false;

            if (scopeService.isCurrentUserChiefOrHeadsman()) {
                // Chiefs/headsmen choose from the villages they are scoped to
                // (via their linked authority and/or the villages they directly head).
                // The authority is not asked for; it is derived from their own links.
                Set<Long> allowed = scopedVillageIds();
                if (allowed == null || allowed.isEmpty()) {
                    // Not scoped to any village -> cannot create parcels.
                    villages = Collections.emptyList();
                    creationBlocked = true;
                } else {
                    villages = villageService.findAll().stream()
                            .filter(v -> allowed.contains(v.getId()))
                            .filter(v -> v.getActive() == null || v.getActive())
                            .collect(Collectors.toList());
                }
            } else {
                // Admins etc. -> all active villages.
                villages = villageService.findAll().stream()
                        .filter(v -> v.getActive() == null || v.getActive())
                        .collect(Collectors.toList());
            }
            log.info("Loading parcel create form with {} villages (creationBlocked={})",
                    villages.size(), creationBlocked);

            if (!model.containsAttribute("form")) {
                model.addAttribute("form", ParcelRequest.builder().build());
            }

            model.addAttribute("villages", villages);
            model.addAttribute("villageBoundaryJson", buildVillageBoundaryJson(villages));
            model.addAttribute("creationBlocked", creationBlocked);
            model.addAttribute("statuses", ParcelStatus.values());
            model.addAttribute("captureModes", CaptureMode.values());
            model.addAttribute("pageTitle", "Create Parcel");
            model.addAttribute("currentPage", "parcels");
            return "parcels/create";
        } catch (Exception e) {
            log.error("Error loading create parcel form: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading form: " + e.getMessage());
            model.addAttribute("villages", Collections.emptyList());
            model.addAttribute("creationBlocked", true);
            model.addAttribute("statuses", ParcelStatus.values());
            model.addAttribute("captureModes", CaptureMode.values());
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

        log.info("Creating parcel");

        try {
            // Validate the selected village belongs to the current user's authority
            requireVillageAccess(request.getVillageId());

            // Parse the JSON boundaries
            List<BoundaryPointDto> boundaries = parseBoundariesJson(boundariesJson);

            // Validate boundaries
            if (boundaries == null || boundaries.size() < 3) {
                String errorMsg = "Please capture at least 3 GPS boundary points.";
                log.warn("Parcel creation failed: insufficient boundary points");
                ra.addFlashAttribute("errorMessage", errorMsg);
                ra.addFlashAttribute("form", request);
                return "redirect:/parcels/create";
            }

            // Set boundaries on the request
            request.setBoundaries(boundaries);

            var response = parcelService.createParcel(request, userDetails.getUsername());
            ra.addFlashAttribute("successMessage",
                    "Parcel " + response.getParcelNumber() +
                            " created successfully for stand " + response.getStandNumber() + ".");
            log.info("Parcel created: {}", response.getParcelNumber());
            return "redirect:/parcels";

        } catch (JsonProcessingException e) {
            log.warn("Parcel creation failed: invalid boundary data");
            ra.addFlashAttribute("errorMessage", "Invalid boundary data format.");
            ra.addFlashAttribute("form", request);
            return "redirect:/parcels/create";
        } catch (Exception e) {
            log.error("Parcel creation failed", e);
            ra.addFlashAttribute("errorMessage", "Error creating parcel: " + e.getMessage());
            ra.addFlashAttribute("form", request);
            return "redirect:/parcels/create";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            // Chiefs/headsmen may only view parcels in their authority's villages
            if (scopeService.isCurrentUserChiefOrHeadsman()) {
                requireParcelAccess(id);
            }

            var parcel = parcelService.findById(id);
            model.addAttribute("parcel", parcel);
            model.addAttribute("pageTitle", "Parcel " + parcel.getParcelNumber());
            model.addAttribute("currentPage", "parcels");
            return "parcels/detail";
        } catch (Exception e) {
            log.error("Error loading parcel detail", e);
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/parcels";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            // Chiefs/headsmen may only edit parcels in their authority's villages
            if (scopeService.isCurrentUserChiefOrHeadsman()) {
                requireParcelAccess(id);
            }

            var parcel = parcelService.findById(id);

            var form = ParcelRequest.builder()
                    .standNumber(parcel.getStandNumber())
                    .villageId(parcel.getVillageId())
                    .notes(parcel.getNotes())
                    .boundaries(parcel.getBoundaries())
                    .captureMode(parcel.getCaptureMode())
                    .build();

            var parcelVillage = villageService.findById(parcel.getVillageId());

            List<TraditionalAuthorityResponse> authorities;
            if (scopeService.isCurrentUserChiefOrHeadsman()) {
                // Every authority the user may see/edit against; fall back to
                // the parcel's village authority for a village-scoped headsman.
                var parcelVillageAuth = parcelVillage != null
                        ? parcelVillage.getTraditionalAuthorityId() : null;

                java.util.Set<Long> allowed = new java.util.HashSet<>(
                        scopeService.getCurrentUserAuthorityIds());
                if (allowed.isEmpty() && parcelVillageAuth != null) {
                    allowed.add(parcelVillageAuth);
                }

                // Only allow editing if the parcel's village is within the user's scope.
                if (scopeService.isCurrentUserAdmin()
                        || scopeService.canAccessVillage(parcel.getVillageId())) {
                    authorities = allowed.isEmpty()
                            ? Collections.emptyList()
                            : authorityService.findAllActive().stream()
                                .filter(a -> allowed.contains(a.getId()))
                                .toList();
                } else {
                    authorities = Collections.emptyList();
                }
            } else {
                authorities = authorityService.findAllActive();
            }
            Long currentAuthorityId = parcelVillage != null ? parcelVillage.getTraditionalAuthorityId() : null;
            var villages = villageService.findByAuthority(currentAuthorityId);

            model.addAttribute("parcel", parcel);
            model.addAttribute("form", form);
            model.addAttribute("currentAuthorityId", currentAuthorityId);
            model.addAttribute("authorities", authorities);
            model.addAttribute("villages", villages);
            model.addAttribute("statuses", ParcelStatus.values());
            model.addAttribute("captureModes", CaptureMode.values());
            model.addAttribute("pageTitle", "Edit Parcel " + parcel.getParcelNumber());
            model.addAttribute("currentPage", "parcels");
            return "parcels/edit";
        } catch (Exception e) {
            log.error("Error loading edit form", e);
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
            // Validate the target village belongs to the current user's authority
            requireVillageAccess(request.getVillageId());

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
                    "Parcel " + response.getParcelNumber() + " updated successfully.");
            log.info("Parcel updated: {}", response.getParcelNumber());
            return "redirect:/parcels/" + id;
        } catch (JsonProcessingException e) {
            log.error("Error parsing boundaries", e);
            ra.addFlashAttribute("errorMessage", "Invalid boundary data format.");
            return "redirect:/parcels/" + id + "/edit";
        } catch (Exception e) {
            log.error("Error updating parcel", e);
            ra.addFlashAttribute("errorMessage", "Error updating parcel: " + e.getMessage());
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
            // Chiefs/headsmen may only change status of parcels in their authority
            if (scopeService.isCurrentUserChiefOrHeadsman()) {
                requireParcelAccess(id);
            }

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
            // Chiefs/headsmen may only load villages they are scoped to.
            // They may be scoped via a linked authority, or directly via the
            // villages they head. If they can access the authority directly,
            // return its villages. Otherwise return only the villages within
            // their scoped set that belong to this authority.
            if (scopeService.isCurrentUserChiefOrHeadsman()) {
                if (scopeService.canAccessAuthority(authorityId)) {
                    log.info("Loading villages for authority ID: {}", authorityId);
                    return villageService.findByAuthority(authorityId).stream()
                            .filter(v -> v.getActive() == null || v.getActive())
                            .collect(Collectors.toList());
                }
                Set<Long> scopedVillageIds = scopeService.scopedVillageIds();
                if (scopedVillageIds == null || scopedVillageIds.isEmpty()) {
                    return Collections.emptyList();
                }
                log.info("Loading scoped villages for authority ID: {}", authorityId);
                return villageService.findByAuthority(authorityId).stream()
                        .filter(v -> (v.getActive() == null || v.getActive())
                                && scopedVillageIds.contains(v.getId()))
                        .collect(Collectors.toList());
            }

            log.info("Loading villages for authority ID: {}", authorityId);
            var villages = villageService.findByAuthority(authorityId).stream()
                    .filter(v -> v.getActive() == null || v.getActive())
                    .collect(Collectors.toList());
            log.info("Found {} active villages for authority {}", villages.size(), authorityId);
            return villages;
        } catch (Exception e) {
            log.error("Error loading villages for authority {}: {}", authorityId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Serialises each village's mapped boundary to a JSON string of the form
     * [{"id":1,"boundary":[[lat,lng],...]}, ...] so the map editor can draw the
     * selected village's boundary. Only villages with >= 3 points are included.
     */
    private String buildVillageBoundaryJson(List<VillageResponse> villages) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (VillageResponse v : villages) {
            if (v.getBoundary() == null || v.getBoundary().size() < 3) {
                continue;
            }
            List<List<Double>> coords = new ArrayList<>();
            for (var c : v.getBoundary()) {
                List<Double> pt = new ArrayList<>();
                pt.add(c.getLatitude());
                pt.add(c.getLongitude());
                coords.add(pt);
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", v.getId());
            entry.put("boundary", coords);
            out.add(entry);
        }
        try {
            return objectMapper.writeValueAsString(out);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialise village boundaries: {}", e.getMessage());
            return "[]";
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

