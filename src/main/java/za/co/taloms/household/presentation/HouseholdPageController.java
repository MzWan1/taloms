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

    @GetMapping("/test")
    @ResponseBody
    public String test() {
        return "Household module is working! Time: " + java.time.LocalDateTime.now();
    }

    @GetMapping("/plain")
    @ResponseBody
    public String listPlain() {
        try {
            List<HouseholdResponse> households = householdService.findAll();
            long total = households != null ? households.size() : 0;
            long active = households != null ? households.stream().filter(HouseholdResponse::getActive).count() : 0;
            return "Households: total=" + total + ", active=" + active + ", time=" + java.time.LocalDateTime.now();
        } catch (Throwable e) {
            return "Error loading households: " + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
    }

    @GetMapping
    @ResponseBody
    public String list(Model model) {
        try {
            log.info("Loading household list");
            List<HouseholdResponse> households = householdService.findAll();

            if (households == null) {
                households = Collections.emptyList();
            }

            long totalCount = households.size();
            long activeCount = households.stream().filter(HouseholdResponse::getActive).count();

            StringBuilder sb = new StringBuilder();
            sb.append("<!DOCTYPE html><html lang='en'><head>");
            sb.append("<meta charset='UTF-8'>");
            sb.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
            sb.append("<title>Household Management | TALOMS</title>");
            sb.append("<link href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css' rel='stylesheet'>");
            sb.append("<link href='https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css' rel='stylesheet'>");
            sb.append("<link href='/css/taloms.css' rel='stylesheet'>");
            sb.append("</head><body class='bg-light'>");

            sb.append("<nav class='navbar navbar-expand-lg navbar-dark bg-navy shadow-sm sticky-top'>");
            sb.append("<div class='container-fluid px-4'>");
            sb.append("<a class='navbar-brand fw-bold' href='/dashboard'><i class='bi bi-geo-alt-fill me-2'></i>TALOMS</a>");
            sb.append("<button class='navbar-toggler border-0' type='button' data-bs-toggle='collapse' data-bs-target='#mainNav'>");
            sb.append("<span class='navbar-toggler-icon'></span>");
            sb.append("</button>");
            sb.append("<div class='collapse navbar-collapse' id='mainNav'>");
            sb.append("<ul class='navbar-nav me-auto gap-1'>");
            sb.append("<li class='nav-item'><a class='nav-link rounded-2 px-3' href='/dashboard'><i class='bi bi-speedometer2 me-1'></i>Dashboard</a></li>");
            sb.append("<li class='nav-item'><a class='nav-link rounded-2 px-3' href='/users'><i class='bi bi-people me-1'></i>Users</a></li>");
            sb.append("<li class='nav-item'><a class='nav-link rounded-2 px-3' href='/authorities'><i class='bi bi-building me-1'></i>Authorities</a></li>");
            sb.append("<li class='nav-item'><a class='nav-link rounded-2 px-3' href='/ptos'><i class='bi bi-file-earmark-text me-1'></i>PTOs</a></li>");
            sb.append("<li class='nav-item'><a class='nav-link rounded-2 px-3' href='/parcels'><i class='bi bi-map me-1'></i>Parcels</a></li>");
            sb.append("<li class='nav-item'><a class='nav-link rounded-2 px-3 active bg-white bg-opacity-10' href='/households'><i class='bi bi-people me-1'></i>Households</a></li>");
            sb.append("<li class='nav-item'><a class='nav-link rounded-2 px-3' href='/residents'><i class='bi bi-person-badge me-1'></i>Residents</a></li>");
            sb.append("<li class='nav-item'><a class='nav-link rounded-2 px-3' href='/reports'><i class='bi bi-bar-chart me-1'></i>Reports</a></li>");
            sb.append("<li class='nav-item'><a class='nav-link rounded-2 px-3' href='/business-occupancies'><i class='bi bi-shop me-1'></i>Business</a></li>");
            sb.append("<li class='nav-item'><a class='nav-link rounded-2 px-3' href='/documents'><i class='bi bi-files me-1'></i>Documents</a></li>");
            sb.append("<li class='nav-item'><a class='nav-link rounded-2 px-3' href='/audit'><i class='bi bi-clock-history me-1'></i>Audit</a></li>");
            sb.append("<li class='nav-item'><a class='nav-link rounded-2 px-3' href='/gis'><i class='bi bi-map me-1'></i>GIS</a></li>");
            sb.append("</ul>");
            sb.append("</div></div></nav>");

            sb.append("<div class='container-fluid px-4 py-4'>");

            sb.append("<div class='d-flex align-items-start justify-content-between mb-4 flex-wrap gap-3'>");
            sb.append("<div>");
            sb.append("<nav aria-label='breadcrumb'><ol class='breadcrumb mb-1 small'>");
            sb.append("<li class='breadcrumb-item'><a href='/dashboard' class='text-muted text-decoration-none'>Dashboard</a></li>");
            sb.append("<li class='breadcrumb-item active'>Household Management</li>");
            sb.append("</ol></nav>");
            sb.append("<h4 class='fw-bold text-navy mb-0'><i class='bi bi-people me-2'></i>Household Management</h4>");
            sb.append("<p class='text-muted small mb-0'>Manage all households linked to parcels</p>");
            sb.append("</div>");
            sb.append("<div class='d-flex gap-2'>");
            sb.append("<a href='/dashboard' class='btn btn-outline-secondary px-3'><i class='bi bi-arrow-left me-1'></i>Back</a>");
            sb.append("<a href='/households/create' class='btn btn-navy px-4 fw-semibold'><i class='bi bi-plus-circle me-2'></i>New Household</a>");
            sb.append("</div></div>");

            sb.append("<div class='row g-3 mb-4'>");
            sb.append("<div class='col-6 col-md-3'>");
            sb.append("<div class='card border-0 shadow-sm text-center py-3'>");
            sb.append("<div class='fw-bold fs-3 text-navy'>").append(totalCount).append("</div>");
            sb.append("<div class='small text-muted'>Total Households</div>");
            sb.append("</div></div>");
            sb.append("<div class='col-6 col-md-3'>");
            sb.append("<div class='card border-0 shadow-sm text-center py-3'>");
            sb.append("<div class='fw-bold fs-3 text-success'>").append(activeCount).append("</div>");
            sb.append("<div class='small text-muted'>Active</div>");
            sb.append("</div></div>");
            sb.append("</div>");

            sb.append("<div class='card border-0 shadow-sm'>");
            sb.append("<div class='card-body p-0'><div class='table-responsive'>");
            sb.append("<table class='table table-hover mb-0'>");
            sb.append("<thead><tr>");
            sb.append("<th class='px-4'>#</th><th>Household Head</th><th>ID Number</th><th>Parcel</th><th>Village</th><th>Registration Date</th><th>Status</th><th class='text-center'>Actions</th>");
            sb.append("</tr></thead><tbody>");

            if (households.isEmpty()) {
                sb.append("<tr><td colspan='8' class='text-center py-5 text-muted'>");
                sb.append("<i class='bi bi-people display-5 d-block mb-2 opacity-25'></i>");
                sb.append("<span class='fw-semibold'>No households found</span><br>");
                sb.append("<small>Click 'New Household' to create the first record</small>");
                sb.append("</td></tr>");
            } else {
                int i = 1;
                for (var h : households) {
                    String name = h.getHouseholdHeadName() != null ? h.getHouseholdHeadName() : "Unknown";
                    String idNum = h.getHouseholdHeadIdNumber() != null ? h.getHouseholdHeadIdNumber() : "";
                    String stand = h.getStandNumber() != null ? h.getStandNumber() : "";
                    String village = h.getVillageName() != null ? h.getVillageName() : "";
                    String regDate = h.getRegistrationDate() != null ? h.getRegistrationDate().toString() : "—";
                    boolean active = Boolean.TRUE.equals(h.getActive());

                    String initials = name.length() >= 2 ? name.substring(0, 2).toUpperCase() : (name.isEmpty() ? "HH" : name);

                    sb.append("<tr>");
                    sb.append("<td class='px-4 text-muted small'>").append(i++).append("</td>");
                    sb.append("<td><div class='d-flex align-items-center gap-3'>");
                    sb.append("<div class='household-avatar' style='width:36px;height:36px;background:#D6E4F0;color:#1B3A6B;border-radius:8px;display:flex;align-items:center;justify-content:center;font-size:0.75rem;font-weight:700;flex-shrink:0;'>").append(initials).append("</div>");
                    sb.append("<div>");
                    sb.append("<a href='/households/").append(h.getId()).append("' class='fw-semibold text-navy text-decoration-none small'>").append(name).append("</a>");
                    if (h.getContactPhone() != null) {
                        sb.append("<div class='text-muted' style='font-size:0.78rem'>").append(h.getContactPhone()).append("</div>");
                    }
                    sb.append("</div></div></td>");
                    sb.append("<td><code class='small'>").append(idNum).append("</code></td>");
                    sb.append("<td class='small'>").append(stand).append("</td>");
                    sb.append("<td class='small'>").append(village).append("</td>");
                    sb.append("<td class='small'>").append(regDate).append("</td>");
                    sb.append("<td>");
                    if (active) {
                        sb.append("<span class='badge bg-success'>Active</span>");
                    } else {
                        sb.append("<span class='badge bg-secondary'>Inactive</span>");
                    }
                    sb.append("</td>");
                    sb.append("<td class='text-center'>");
                    sb.append("<div class='btn-group btn-group-sm'>");
                    sb.append("<a href='/households/").append(h.getId()).append("' class='btn btn-outline-info' title='View'><i class='bi bi-eye'></i></a>");
                    sb.append("<a href='/households/").append(h.getId()).append("/edit' class='btn btn-outline-primary' title='Edit'><i class='bi bi-pencil'></i></a>");
                    sb.append("</div></td>");
                    sb.append("</tr>");
                }
            }

            sb.append("</tbody></table></div></div></div>");
            sb.append("</div>");

            sb.append("<style>.household-avatar{width:36px;height:36px;background:#D6E4F0;color:#1B3A6B;border-radius:8px;display:flex;align-items:center;justify-content:center;font-size:0.75rem;font-weight:700;flex-shrink:0;}</style>");
            sb.append("<script src='https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js'></script>");
            sb.append("</body></html>");

            return sb.toString();
        } catch (Throwable e) {
            log.error("Error loading household list: {}", e.getMessage(), e);
            return "<html><body><h1>Error loading households</h1><p>" + e.getClass().getSimpleName() + " - " + e.getMessage() + "</p></body></html>";
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

            List<za.co.taloms.parcel.application.dto.ParcelResponse> availableParcels = new ArrayList<>();
            try {
                var allParcels = parcelService.findAll();
                if (allParcels != null && !allParcels.isEmpty()) {
                    availableParcels = allParcels.stream()
                            .filter(p -> p.getStatus() == ParcelStatus.AVAILABLE)
                            .collect(Collectors.toList());
                    log.info("Found {} available parcels for household creation", availableParcels.size());
                }
            } catch (Throwable e) {
                log.error("Error loading available parcels: {}", e.getMessage(), e);
                availableParcels = Collections.emptyList();
            }

            List<za.co.taloms.pto.application.dto.PTOResponse> activePtos = new ArrayList<>();
            try {
                activePtos = ptoService.findByStatus(PTOStatus.ACTIVE);
                if (activePtos == null) {
                    activePtos = Collections.emptyList();
                }
                log.info("Found {} active PTOs for household creation", activePtos.size());
            } catch (Throwable e) {
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
        } catch (Throwable e) {
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
                ra.addFlashAttribute("errorMessage", "Please select a parcel for this household.");
                ra.addFlashAttribute("form", request);
                return "redirect:/households/create";
            }

            var response = householdService.createHousehold(request, userDetails.getUsername());
            ra.addFlashAttribute("successMessage",
                    "Household created successfully for " + (response.getHouseholdHeadName() != null ? response.getHouseholdHeadName() : "record") + ".");
            return "redirect:/households";

        } catch (Throwable e) {
            log.error("Error creating household: {}", e.getMessage(), e);
            ra.addFlashAttribute("errorMessage", "Error creating household: " + e.getMessage());
            ra.addFlashAttribute("form", request);
            return "redirect:/households/create";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        try {
            var household = householdService.findById(id);
            model.addAttribute("household", household);
            model.addAttribute("pageTitle", "Household - " + (household.getHouseholdHeadName() != null ? household.getHouseholdHeadName() : "Unknown"));
            model.addAttribute("currentPage", "households");
            return "households/detail";
        } catch (Throwable e) {
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
            } catch (Throwable e) {
                log.error("Error loading available parcels: {}", e.getMessage(), e);
                availableParcels = Collections.emptyList();
            }

            List<za.co.taloms.pto.application.dto.PTOResponse> activePtos = new ArrayList<>();
            try {
                activePtos = ptoService.findByStatus(PTOStatus.ACTIVE);
                if (activePtos == null) {
                    activePtos = Collections.emptyList();
                }
            } catch (Throwable e) {
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
        } catch (Throwable e) {
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
                ra.addFlashAttribute("errorMessage", "Please select a parcel for this household.");
                ra.addFlashAttribute("form", request);
                return "redirect:/households/" + id + "/edit";
            }

            var response = householdService.updateHousehold(id, request, userDetails.getUsername());
            ra.addFlashAttribute("successMessage",
                    "Household updated successfully for " + (response.getHouseholdHeadName() != null ? response.getHouseholdHeadName() : "record") + ".");
            return "redirect:/households/" + id;
        } catch (Throwable e) {
            log.error("Error updating household: {}", e.getMessage(), e);
            ra.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
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
            ra.addFlashAttribute("successMessage", "Household deactivated successfully.");
        } catch (Throwable e) {
            ra.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
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
            ra.addFlashAttribute("successMessage", "Household activated successfully.");
        } catch (Throwable e) {
            ra.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/households/" + id;
    }

    @GetMapping("/parcel/{parcelId}/active")
    @ResponseBody
    public Object getActiveHousehold(@PathVariable Long parcelId) {
        try {
            var household = householdService.findActiveByParcelId(parcelId);
            return household != null ? household : Collections.emptyMap();
        } catch (Throwable e) {
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

            try {
                var households = householdService.findAll();
                sb.append("Service: OK\n");
                sb.append("Household count: ").append(households != null ? households.size() : 0).append("\n");
            } catch (Throwable e) {
                sb.append("Service Error: ").append(e.getMessage()).append("\n");
            }

            sb.append("\n=== Template Check ===\n");
            try {
                return "households/list";
            } catch (Throwable e) {
                sb.append("Template Error: ").append(e.getMessage()).append("\n");
            }

            return sb.toString();
        } catch (Throwable e) {
            return "Debug Error: " + e.getMessage();
        }
    }
}
