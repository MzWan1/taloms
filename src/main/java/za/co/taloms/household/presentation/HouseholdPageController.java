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

    private String escapeHtml(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            switch (c) {
                case '&' -> sb.append("&");
                case '<' -> sb.append("<");
                case '>' -> sb.append(">");
                case '"' -> sb.append(String.valueOf('"'));
                case '\'' -> sb.append("'");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private String buildNavbarHtml(String currentPage) {
        StringBuilder sb = new StringBuilder();
        sb.append("<nav class='navbar navbar-expand-lg navbar-dark bg-navy shadow-sm sticky-top'>");
        sb.append("<div class='container-fluid px-4'>");
        sb.append("<a class='navbar-brand fw-bold' href='/dashboard'><i class='bi bi-geo-alt-fill me-2'></i>TALOMS</a>");
        sb.append("<button class='navbar-toggler border-0' type='button' data-bs-toggle='collapse' data-bs-target='#mainNav'>");
        sb.append("<span class='navbar-toggler-icon'></span></button>");
        sb.append("<div class='collapse navbar-collapse' id='mainNav'>");
        sb.append("<ul class='navbar-nav me-auto gap-1'>");
        sb.append("<li class='nav-item'><a class='nav-link rounded-2 px-3' href='/dashboard'><i class='bi bi-speedometer2 me-1'></i>Dashboard</a></li>");
        sb.append("<li class='nav-item'><a class='nav-link rounded-2 px-3' href='/users'><i class='bi bi-people me-1'></i>Users</a></li>");
        sb.append("<li class='nav-item'><a class='nav-link rounded-2 px-3' href='/authorities'><i class='bi bi-building me-1'></i>Authorities</a></li>");
        sb.append("<li class='nav-item'><a class='nav-link rounded-2 px-3' href='/ptos'><i class='bi bi-file-earmark-text me-1'></i>PTOs</a></li>");
        sb.append("<li class='nav-item'><a class='nav-link rounded-2 px-3' href='/parcels'><i class='bi bi-map me-1'></i>Parcels</a></li>");
        if ("households".equals(currentPage)) {
            sb.append("<li class='nav-item'><a class='nav-link rounded-2 px-3 active bg-white bg-opacity-10' href='/households'><i class='bi bi-people me-1'></i>Households</a></li>");
        } else {
            sb.append("<li class='nav-item'><a class='nav-link rounded-2 px-3' href='/households'><i class='bi bi-people me-1'></i>Households</a></li>");
        }
        sb.append("<li class='nav-item'><a class='nav-link rounded-2 px-3' href='/residents'><i class='bi bi-person-badge me-1'></i>Residents</a></li>");
        sb.append("<li class='nav-item'><a class='nav-link rounded-2 px-3' href='/reports'><i class='bi bi-bar-chart me-1'></i>Reports</a></li>");
        sb.append("<li class='nav-item'><a class='nav-link rounded-2 px-3' href='/business-occupancies'><i class='bi bi-shop me-1'></i>Business</a></li>");
        sb.append("<li class='nav-item'><a class='nav-link rounded-2 px-3' href='/documents'><i class='bi bi-files me-1'></i>Documents</a></li>");
        sb.append("<li class='nav-item'><a class='nav-link rounded-2 px-3' href='/audit'><i class='bi bi-clock-history me-1'></i>Audit</a></li>");
        sb.append("<li class='nav-item'><a class='nav-link rounded-2 px-3' href='/gis'><i class='bi bi-map me-1'></i>GIS</a></li>");
        sb.append("</ul>");
        sb.append("</div></div></nav>");
        return sb.toString();
    }

    private String buildFooterHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<style>.household-avatar{width:36px;height:36px;background:#D6E4F0;color:#1B3A6B;border-radius:8px;display:flex;align-items:center;justify-content:center;font-size:0.75rem;font-weight:700;flex-shrink:0;}</style>");
        sb.append("<script src='https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js'></script>");
        sb.append("</body></html>");
        return sb.toString();
    }

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
    @ResponseBody
    public String createForm(Model model) {
        try {
            log.info("Loading household create form");

            HouseholdRequest form;
            if (model.containsAttribute("form")) {
                form = (HouseholdRequest) model.getAttribute("form");
            } else {
                form = HouseholdRequest.builder()
                        .registrationDate(LocalDate.now())
                        .build();
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

            String errorMessage = (String) model.asMap().get("errorMessage");
            String successMessage = (String) model.asMap().get("successMessage");

            StringBuilder sb = new StringBuilder();
            sb.append("<!DOCTYPE html><html lang='en'><head>");
            sb.append("<meta charset='UTF-8'>");
            sb.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
            sb.append("<title>Create Household | TALOMS</title>");
            sb.append("<link href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css' rel='stylesheet'>");
            sb.append("<link href='https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css' rel='stylesheet'>");
            sb.append("<link href='/css/taloms.css' rel='stylesheet'>");
            sb.append("</head><body class='bg-light'>");

            sb.append(buildNavbarHtml("households"));

            sb.append("<div class='container-fluid px-4 py-4'>");

            if (successMessage != null) {
                sb.append("<div class='alert alert-success alert-dismissible fade show border-0 shadow-sm mb-4'>");
                sb.append("<i class='bi bi-check-circle-fill me-2'></i>");
                sb.append("<span>").append(escapeHtml(successMessage)).append("</span>");
                sb.append("<button type='button' class='btn-close' data-bs-dismiss='alert'></button>");
                sb.append("</div>");
            }
            if (errorMessage != null) {
                sb.append("<div class='alert alert-danger alert-dismissible fade show border-0 shadow-sm mb-4'>");
                sb.append("<i class='bi bi-exclamation-circle-fill me-2'></i>");
                sb.append("<span>").append(escapeHtml(errorMessage)).append("</span>");
                sb.append("<button type='button' class='btn-close' data-bs-dismiss='alert'></button>");
                sb.append("</div>");
            }

            sb.append("<div class='d-flex align-items-start justify-content-between mb-4 flex-wrap gap-3'>");
            sb.append("<div>");
            sb.append("<nav aria-label='breadcrumb'><ol class='breadcrumb mb-1 small'>");
            sb.append("<li class='breadcrumb-item'><a href='/dashboard' class='text-muted text-decoration-none'>Dashboard</a></li>");
            sb.append("<li class='breadcrumb-item'><a href='/households' class='text-muted text-decoration-none'>Households</a></li>");
            sb.append("<li class='breadcrumb-item active'>New Household</li>");
            sb.append("</ol></nav>");
            sb.append("<h4 class='fw-bold text-navy mb-0'><i class='bi bi-person-plus me-2'></i>Create New Household</h4>");
            sb.append("<p class='text-muted small mb-0'>Register a new household on a parcel</p>");
            sb.append("</div>");
            sb.append("<a href='/households' class='btn btn-outline-secondary px-3'><i class='bi bi-arrow-left me-1'></i>Back</a>");
            sb.append("</div>");

            sb.append("<div class='row justify-content-center'>");
            sb.append("<div class='col-12 col-lg-8'>");
            sb.append("<form action='/households/create' method='post' id='householdForm'>");

            sb.append("<div class='card border-0 shadow-sm'>");
            sb.append("<div class='card-header bg-white border-bottom py-3'>");
            sb.append("<h6 class='fw-bold text-navy mb-0'><i class='bi bi-info-circle me-2'></i>Household Details</h6>");
            sb.append("</div>");
            sb.append("<div class='card-body p-4'>");
            sb.append("<div class='row g-3'>");

            sb.append("<div class='col-12'>");
            sb.append("<label for='householdHeadName' class='form-label'>Household Head Name <span class='text-danger'>*</span></label>");
            sb.append("<div class='input-group'>");
            sb.append("<span class='input-group-text'><i class='bi bi-person'></i></span>");
            sb.append("<input type='text' class='form-control' id='householdHeadName' name='householdHeadName' value='").append(escapeHtml(form.getHouseholdHeadName())).append("' required>");
            sb.append("</div></div>");

            sb.append("<div class='col-12 col-md-6'>");
            sb.append("<label for='householdHeadIdNumber' class='form-label'>ID Number <span class='text-danger'>*</span></label>");
            sb.append("<div class='input-group'>");
            sb.append("<span class='input-group-text'><i class='bi bi-credit-card'></i></span>");
            sb.append("<input type='text' class='form-control' id='householdHeadIdNumber' name='householdHeadIdNumber' value='").append(escapeHtml(form.getHouseholdHeadIdNumber())).append("' maxlength='13' required>");
            sb.append("</div>");
            sb.append("<div class='mt-1' id='idFeedback' style='display:none'></div>");
            sb.append("</div>");

            sb.append("<div class='col-12 col-md-6'>");
            sb.append("<label for='contactPhone' class='form-label'>Contact Phone</label>");
            sb.append("<div class='input-group'>");
            sb.append("<span class='input-group-text'><i class='bi bi-telephone'></i></span>");
            sb.append("<input type='tel' class='form-control' id='contactPhone' name='contactPhone' value='").append(escapeHtml(form.getContactPhone())).append("' placeholder='+27XXXXXXXXX'>");
            sb.append("</div></div>");

            sb.append("<div class='col-12'>");
            sb.append("<label for='contactEmail' class='form-label'>Contact Email</label>");
            sb.append("<div class='input-group'>");
            sb.append("<span class='input-group-text'><i class='bi bi-envelope'></i></span>");
            sb.append("<input type='email' class='form-control' id='contactEmail' name='contactEmail' value='").append(escapeHtml(form.getContactEmail())).append("' placeholder='e.g. thabo@example.com'>");
            sb.append("</div></div>");

            sb.append("<div class='col-12'>");
            sb.append("<label for='parcelId' class='form-label'>Parcel <span class='text-danger'>*</span></label>");
            sb.append("<div class='input-group'>");
            sb.append("<span class='input-group-text'><i class='bi bi-map'></i></span>");
            sb.append("<select id='parcelId' name='parcelId' class='form-select' required>");
            sb.append("<option value=''>— Select Parcel —</option>");
            for (var p : availableParcels) {
                String text = p.getParcelNumber() + " - Stand " + (p.getStandNumber() != null ? p.getStandNumber() : "") + " (" + (p.getVillageName() != null ? p.getVillageName() : "") + ")";
                sb.append("<option value='").append(p.getId()).append("'");
                if (form.getParcelId() != null && form.getParcelId().equals(p.getId())) {
                    sb.append(" selected");
                }
                sb.append(">").append(escapeHtml(text)).append("</option>");
            }
            sb.append("</select></div>");
            if (availableParcels.isEmpty()) {
                sb.append("<div class='text-warning small mt-1'><i class='bi bi-exclamation-triangle me-1'></i>No available parcels found. Please create a parcel first.</div>");
            } else {
                sb.append("<div class='form-text'>Select a parcel for this household (only available parcels are shown)</div>");
            }
            sb.append("</div>");

            if (activePtos != null && !activePtos.isEmpty()) {
                sb.append("<div class='col-12'>");
                sb.append("<label for='ptoId' class='form-label'>PTO</label>");
                sb.append("<div class='input-group'>");
                sb.append("<span class='input-group-text'><i class='bi bi-file-earmark-text'></i></span>");
                sb.append("<select id='ptoId' name='ptoId' class='form-select'>");
                sb.append("<option value=''>— Select PTO —</option>");
                for (var pto : activePtos) {
                    String text = pto.getPtoNumber() + " - " + (pto.getPtoHolderName() != null ? pto.getPtoHolderName() : "");
                    sb.append("<option value='").append(pto.getId()).append("'");
                    if (form.getPtoId() != null && form.getPtoId().equals(pto.getId())) {
                        sb.append(" selected");
                    }
                    sb.append(">").append(escapeHtml(text)).append("</option>");
                }
                sb.append("</select></div>");
                sb.append("<div class='form-text'>Link this household to an existing PTO</div>");
                sb.append("</div>");
            }

            sb.append("<div class='col-12 col-md-6'>");
            sb.append("<label for='registrationDate' class='form-label'>Registration Date</label>");
            sb.append("<div class='input-group'>");
            sb.append("<span class='input-group-text'><i class='bi bi-calendar'></i></span>");
            String regDate = form.getRegistrationDate() != null ? form.getRegistrationDate().toString() : "";
            sb.append("<input type='date' class='form-control' id='registrationDate' name='registrationDate' value='").append(escapeHtml(regDate)).append("'>");
            sb.append("</div></div>");

            sb.append("<div class='col-12'>");
            sb.append("<label for='notes' class='form-label'>Notes</label>");
            sb.append("<textarea id='notes' name='notes' class='form-control' rows='2' placeholder='Additional notes about this household...'>").append(escapeHtml(form.getNotes())).append("</textarea>");
            sb.append("</div>");

            sb.append("</div></div></div>");

            sb.append("<div class='d-flex gap-3 justify-content-end mt-4 mb-4'>");
            sb.append("<a href='/households' class='btn btn-light px-4'><i class='bi bi-x me-1'></i>Cancel</a>");
            sb.append("<button type='submit' class='btn btn-navy px-4 fw-semibold' id='submitBtn'><i class='bi bi-save me-2'></i>Create Household</button>");
            sb.append("</div>");

            sb.append("</form></div></div></div>");

            sb.append(buildFooterHtml());

            return sb.toString();
        } catch (Throwable e) {
            log.error("Error loading create household form: {}", e.getMessage(), e);
            return "<html><body><h1>Error loading form</h1><p>" + e.getClass().getSimpleName() + " - " + e.getMessage() + "</p><a href='/households'>Back</a></body></html>";
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
    @ResponseBody
    public String detail(@PathVariable Long id, Model model) {
        try {
            var household = householdService.findById(id);
            model.addAttribute("household", household);
            model.addAttribute("pageTitle", "Household - " + (household.getHouseholdHeadName() != null ? household.getHouseholdHeadName() : "Unknown"));
            model.addAttribute("currentPage", "households");

            StringBuilder sb = new StringBuilder();
            sb.append("<!DOCTYPE html><html lang='en'><head>");
            sb.append("<meta charset='UTF-8'>");
            sb.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
            sb.append("<title>").append(escapeHtml(household.getHouseholdHeadName() != null ? household.getHouseholdHeadName() : "Household")).append(" | TALOMS</title>");
            sb.append("<link href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css' rel='stylesheet'>");
            sb.append("<link href='https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css' rel='stylesheet'>");
            sb.append("<link href='/css/taloms.css' rel='stylesheet'>");
            sb.append("</head><body class='bg-light'>");

            sb.append(buildNavbarHtml("households"));

            sb.append("<div class='container-fluid px-4 py-4'>");

            sb.append("<div class='d-flex align-items-start justify-content-between mb-4 flex-wrap gap-3'>");
            sb.append("<div>");
            sb.append("<nav aria-label='breadcrumb'><ol class='breadcrumb mb-1 small'>");
            sb.append("<li class='breadcrumb-item'><a href='/dashboard' class='text-muted text-decoration-none'>Dashboard</a></li>");
            sb.append("<li class='breadcrumb-item'><a href='/households' class='text-muted text-decoration-none'>Households</a></li>");
            sb.append("<li class='breadcrumb-item active'>").append(escapeHtml(household.getHouseholdHeadName() != null ? household.getHouseholdHeadName() : "Household")).append("</li>");
            sb.append("</ol></nav>");
            sb.append("<h4 class='fw-bold text-navy mb-0'>").append(escapeHtml(household.getHouseholdHeadName() != null ? household.getHouseholdHeadName() : "Household Head")).append("</h4>");
            sb.append("<p class='text-muted small mb-0'>Parcel <strong>").append(escapeHtml(household.getStandNumber() != null ? household.getStandNumber() : "")).append("</strong> · <span>").append(escapeHtml(household.getVillageName() != null ? household.getVillageName() : "")).append("</span></p>");
            sb.append("</div>");
            sb.append("<div class='d-flex gap-2'>");
            sb.append("<a href='/households' class='btn btn-outline-secondary px-3'><i class='bi bi-arrow-left me-1'></i>Back</a>");
            sb.append("<a href='/households/").append(household.getId()).append("/edit' class='btn btn-outline-primary px-3'><i class='bi bi-pencil me-1'></i>Edit</a>");
            sb.append("</div></div>");

            sb.append("<div class='card border-0 shadow-sm mb-4'>");
            sb.append("<div class='card-body p-4'>");
            sb.append("<div class='d-flex align-items-center justify-content-between flex-wrap gap-3'>");
            sb.append("<div class='d-flex align-items-center gap-4'>");
            sb.append("<div class='household-hero-avatar' style='width:64px;height:64px;background:#D6E4F0;border-radius:16px;display:flex;align-items:center;justify-content:center;flex-shrink:0;'>");
            sb.append("<i class='bi bi-people fs-2 text-navy'></i>");
            sb.append("</div>");
            sb.append("<div>");
            sb.append("<div class='d-flex align-items-center gap-3 flex-wrap mb-1'>");
            sb.append("<h5 class='fw-bold text-navy mb-0'>").append(escapeHtml(household.getHouseholdHeadName() != null ? household.getHouseholdHeadName() : "Name")).append("</h5>");
            if (Boolean.TRUE.equals(household.getActive())) {
                sb.append("<span class='badge bg-success fs-6 px-3'>Active</span>");
            } else {
                sb.append("<span class='badge bg-secondary fs-6 px-3'>Inactive</span>");
            }
            sb.append("</div>");
            sb.append("<div class='text-muted small'>Registered on ");
            if (household.getRegistrationDate() != null) {
                sb.append(household.getRegistrationDate().toString());
            } else {
                sb.append("—");
            }
            sb.append(" <span>by <strong>").append(escapeHtml(household.getCreatedBy() != null ? household.getCreatedBy() : "admin")).append("</strong></span></div>");
            sb.append("</div></div>");
            sb.append("</div></div></div></div>");

            sb.append("<div class='row g-4'>");
            sb.append("<div class='col-12 col-lg-7'>");

            sb.append("<div class='card border-0 shadow-sm mb-4'>");
            sb.append("<div class='card-header bg-white border-bottom py-3'>");
            sb.append("<h6 class='fw-bold text-navy mb-0'><i class='bi bi-person me-2'></i>Household Head Details</h6>");
            sb.append("</div>");
            sb.append("<div class='card-body p-0'>");
            sb.append("<ul class='list-group list-group-flush'>");

            sb.append("<li class='list-group-item px-4 py-3'><div class='row'><div class='col-5 col-md-4 text-muted small fw-semibold'>Full Name</div><div class='col-7 col-md-8 fw-semibold'>").append(escapeHtml(household.getHouseholdHeadName() != null ? household.getHouseholdHeadName() : "Name")).append("</div></div></li>");
            sb.append("<li class='list-group-item px-4 py-3'><div class='row'><div class='col-5 col-md-4 text-muted small fw-semibold'>ID Number</div><div class='col-7 col-md-8'><code>").append(escapeHtml(household.getHouseholdHeadIdNumber() != null ? household.getHouseholdHeadIdNumber() : "ID")).append("</code></div></div></li>");
            if (household.getContactPhone() != null) {
                sb.append("<li class='list-group-item px-4 py-3'><div class='row'><div class='col-5 col-md-4 text-muted small fw-semibold'>Phone</div><div class='col-7 col-md-8'>").append(escapeHtml(household.getContactPhone())).append("</div></div></li>");
            }
            if (household.getContactEmail() != null) {
                sb.append("<li class='list-group-item px-4 py-3'><div class='row'><div class='col-5 col-md-4 text-muted small fw-semibold'>Email</div><div class='col-7 col-md-8'><a href='mailto:").append(escapeHtml(household.getContactEmail())).append("' class='text-navy text-decoration-none'>").append(escapeHtml(household.getContactEmail())).append("</a></div></div></li>");
            }
            sb.append("<li class='list-group-item px-4 py-3'><div class='row'><div class='col-5 col-md-4 text-muted small fw-semibold'>Status</div><div class='col-7 col-md-8'>");
            if (Boolean.TRUE.equals(household.getActive())) {
                sb.append("<span class='badge bg-success'>Active</span>");
            } else {
                sb.append("<span class='badge bg-secondary'>Inactive</span>");
            }
            sb.append("</div></div></li>");
            sb.append("<li class='list-group-item px-4 py-3'><div class='row'><div class='col-5 col-md-4 text-muted small fw-semibold'>Registration Date</div><div class='col-7 col-md-8'>");
            if (household.getRegistrationDate() != null) {
                sb.append(household.getRegistrationDate().toString());
            } else {
                sb.append("—");
            }
            sb.append("</div></div></li>");
            if (household.getNotes() != null) {
                sb.append("<li class='list-group-item px-4 py-3'><div class='row'><div class='col-5 col-md-4 text-muted small fw-semibold'>Notes</div><div class='col-7 col-md-8 small'>").append(escapeHtml(household.getNotes())).append("</div></div></li>");
            }
            sb.append("</ul></div></div>");

            sb.append("<div class='card border-0 shadow-sm'>");
            sb.append("<div class='card-header bg-white border-bottom py-3'>");
            sb.append("<h6 class='fw-bold text-navy mb-0'><i class='bi bi-geo-alt me-2'></i>Location</h6>");
            sb.append("</div>");
            sb.append("<div class='card-body p-0'>");
            sb.append("<ul class='list-group list-group-flush'>");

            sb.append("<li class='list-group-item px-4 py-3'><div class='row'><div class='col-5 col-md-4 text-muted small fw-semibold'>Parcel</div><div class='col-7 col-md-8'><a href='/parcels/").append(household.getParcelId()).append("' class='text-navy text-decoration-none'>").append(escapeHtml((household.getStandNumber() != null ? household.getStandNumber() : "") + " (" + (household.getParcelNumber() != null ? household.getParcelNumber() : "") + ")")).append("</a></div></div></li>");
            sb.append("<li class='list-group-item px-4 py-3'><div class='row'><div class='col-5 col-md-4 text-muted small fw-semibold'>Village</div><div class='col-7 col-md-8'>").append(escapeHtml(household.getVillageName() != null ? household.getVillageName() : "")).append("</div></div></li>");
            sb.append("<li class='list-group-item px-4 py-3'><div class='row'><div class='col-5 col-md-4 text-muted small fw-semibold'>Traditional Authority</div><div class='col-7 col-md-8'>").append(escapeHtml(household.getAuthorityName() != null ? household.getAuthorityName() : "")).append("</div></div></li>");
            if (household.getPtoNumber() != null) {
                sb.append("<li class='list-group-item px-4 py-3'><div class='row'><div class='col-5 col-md-4 text-muted small fw-semibold'>PTO</div><div class='col-7 col-md-8'><a href='/ptos/").append(household.getPtoId()).append("' class='text-navy text-decoration-none'>").append(escapeHtml(household.getPtoNumber())).append("</a>");
                if (household.getPtoHolderName() != null) {
                    sb.append(" <span class='text-muted small'>— ").append(escapeHtml(household.getPtoHolderName())).append("</span>");
                }
                sb.append("</div></div></li>");
            }
            sb.append("</ul></div></div>");
            sb.append("</div>");

            sb.append("<div class='col-12 col-lg-5'>");
            sb.append("<div class='card border-0 shadow-sm'>");
            sb.append("<div class='card-header bg-white border-bottom py-3'>");
            sb.append("<h6 class='fw-bold text-navy mb-0'><i class='bi bi-lightning me-2'></i>Actions</h6>");
            sb.append("</div>");
            sb.append("<div class='card-body d-grid gap-2 p-3'>");
            if (Boolean.TRUE.equals(household.getActive())) {
                sb.append("<form action='/households/").append(household.getId()).append("/deactivate' method='post'>");
                sb.append("<button type='submit' class='btn btn-outline-danger w-100 text-start'><i class='bi bi-x-circle me-2'></i>Deactivate Household</button>");
                sb.append("</form>");
            } else {
                sb.append("<form action='/households/").append(household.getId()).append("/activate' method='post'>");
                sb.append("<button type='submit' class='btn btn-outline-success w-100 text-start'><i class='bi bi-check-circle me-2'></i>Activate Household</button>");
                sb.append("</form>");
            }
            sb.append("<a href='/households/").append(household.getId()).append("/edit' class='btn btn-outline-primary text-start'><i class='bi bi-pencil me-2'></i>Edit Household</a>");
            sb.append("<a href='/households' class='btn btn-outline-secondary text-start btn-sm'><i class='bi bi-list-ul me-2'></i>Back to Household List</a>");
            sb.append("</div></div>");
            sb.append("</div>");
            sb.append("</div>");
            sb.append("</div>");

            sb.append(buildFooterHtml());

            return sb.toString();
        } catch (Throwable e) {
            log.error("Error loading household detail: {}", e.getMessage(), e);
            return "<html><body><h1>Error loading household</h1><p>" + e.getClass().getSimpleName() + " - " + e.getMessage() + "</p><a href='/households'>Back</a></body></html>";
        }
    }

    @GetMapping("/{id}/edit")
    @ResponseBody
    public String editForm(@PathVariable Long id, Model model) {
        try {
            log.info("Loading household edit form for ID: {}", id);

            var household = householdService.findById(id);
            if (household == null) {
                return "<html><body><h1>Not Found</h1><p>Household not found.</p><a href='/households'>Back</a></body></html>";
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

            String errorMessage = (String) model.asMap().get("errorMessage");
            String successMessage = (String) model.asMap().get("successMessage");

            StringBuilder sb = new StringBuilder();
            sb.append("<!DOCTYPE html><html lang='en'><head>");
            sb.append("<meta charset='UTF-8'>");
            sb.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
            sb.append("<title>Edit Household | TALOMS</title>");
            sb.append("<link href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css' rel='stylesheet'>");
            sb.append("<link href='https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css' rel='stylesheet'>");
            sb.append("<link href='/css/taloms.css' rel='stylesheet'>");
            sb.append("</head><body class='bg-light'>");

            sb.append(buildNavbarHtml("households"));

            sb.append("<div class='container-fluid px-4 py-4'>");

            if (successMessage != null) {
                sb.append("<div class='alert alert-success alert-dismissible fade show border-0 shadow-sm mb-4'>");
                sb.append("<i class='bi bi-check-circle-fill me-2'></i>");
                sb.append("<span>").append(escapeHtml(successMessage)).append("</span>");
                sb.append("<button type='button' class='btn-close' data-bs-dismiss='alert'></button>");
                sb.append("</div>");
            }
            if (errorMessage != null) {
                sb.append("<div class='alert alert-danger alert-dismissible fade show border-0 shadow-sm mb-4'>");
                sb.append("<i class='bi bi-exclamation-circle-fill me-2'></i>");
                sb.append("<span>").append(escapeHtml(errorMessage)).append("</span>");
                sb.append("<button type='button' class='btn-close' data-bs-dismiss='alert'></button>");
                sb.append("</div>");
            }

            sb.append("<div class='d-flex align-items-start justify-content-between mb-4 flex-wrap gap-3'>");
            sb.append("<div>");
            sb.append("<nav aria-label='breadcrumb'><ol class='breadcrumb mb-1 small'>");
            sb.append("<li class='breadcrumb-item'><a href='/dashboard' class='text-muted text-decoration-none'>Dashboard</a></li>");
            sb.append("<li class='breadcrumb-item'><a href='/households' class='text-muted text-decoration-none'>Households</a></li>");
            sb.append("<li class='breadcrumb-item'><a href='/households/").append(household.getId()).append("' class='text-muted text-decoration-none'>").append(escapeHtml(household.getHouseholdHeadName() != null ? household.getHouseholdHeadName() : "Household")).append("</a></li>");
            sb.append("<li class='breadcrumb-item active'>Edit</li>");
            sb.append("</ol></nav>");
            sb.append("<h4 class='fw-bold text-navy mb-0'><i class='bi bi-pencil-square me-2'></i>Edit Household</h4>");
            sb.append("<p class='text-muted small mb-0'>Update household details</p>");
            sb.append("</div>");
            sb.append("<a href='/households/").append(household.getId()).append("' class='btn btn-outline-secondary px-3'><i class='bi bi-arrow-left me-1'></i>Back to Household</a>");
            sb.append("</div>");

            sb.append("<div class='row justify-content-center'>");
            sb.append("<div class='col-12 col-lg-8'>");
            sb.append("<form action='/households/").append(household.getId()).append("/edit' method='post' id='householdForm'>");

            sb.append("<div class='card border-0 shadow-sm'>");
            sb.append("<div class='card-header bg-white border-bottom py-3'>");
            sb.append("<h6 class='fw-bold text-navy mb-0'><i class='bi bi-info-circle me-2'></i>Household Details</h6>");
            sb.append("</div>");
            sb.append("<div class='card-body p-4'>");
            sb.append("<div class='row g-3'>");

            sb.append("<div class='col-12'>");
            sb.append("<label for='householdHeadName' class='form-label'>Household Head Name <span class='text-danger'>*</span></label>");
            sb.append("<div class='input-group'>");
            sb.append("<span class='input-group-text'><i class='bi bi-person'></i></span>");
            sb.append("<input type='text' class='form-control' id='householdHeadName' name='householdHeadName' value='").append(escapeHtml(form.getHouseholdHeadName())).append("' required>");
            sb.append("</div></div>");

            sb.append("<div class='col-12 col-md-6'>");
            sb.append("<label for='householdHeadIdNumber' class='form-label'>ID Number <span class='text-danger'>*</span></label>");
            sb.append("<div class='input-group'>");
            sb.append("<span class='input-group-text'><i class='bi bi-credit-card'></i></span>");
            sb.append("<input type='text' class='form-control' id='householdHeadIdNumber' name='householdHeadIdNumber' value='").append(escapeHtml(form.getHouseholdHeadIdNumber())).append("' maxlength='13' required>");
            sb.append("</div>");
            sb.append("<div class='mt-1' id='idFeedback' style='display:none'></div>");
            sb.append("</div>");

            sb.append("<div class='col-12 col-md-6'>");
            sb.append("<label for='contactPhone' class='form-label'>Contact Phone</label>");
            sb.append("<div class='input-group'>");
            sb.append("<span class='input-group-text'><i class='bi bi-telephone'></i></span>");
            sb.append("<input type='tel' class='form-control' id='contactPhone' name='contactPhone' value='").append(escapeHtml(form.getContactPhone())).append("' placeholder='+27XXXXXXXXX'>");
            sb.append("</div></div>");

            sb.append("<div class='col-12'>");
            sb.append("<label for='contactEmail' class='form-label'>Contact Email</label>");
            sb.append("<div class='input-group'>");
            sb.append("<span class='input-group-text'><i class='bi bi-envelope'></i></span>");
            sb.append("<input type='email' class='form-control' id='contactEmail' name='contactEmail' value='").append(escapeHtml(form.getContactEmail())).append("' placeholder='e.g. thabo@example.com'>");
            sb.append("</div></div>");

            sb.append("<div class='col-12'>");
            sb.append("<label for='parcelId' class='form-label'>Parcel <span class='text-danger'>*</span></label>");
            sb.append("<div class='input-group'>");
            sb.append("<span class='input-group-text'><i class='bi bi-map'></i></span>");
            sb.append("<select id='parcelId' name='parcelId' class='form-select' required>");
            sb.append("<option value=''>— Select Parcel —</option>");
            for (var p : availableParcels) {
                String text = p.getParcelNumber() + " - Stand " + (p.getStandNumber() != null ? p.getStandNumber() : "") + " (" + (p.getVillageName() != null ? p.getVillageName() : "") + ")";
                sb.append("<option value='").append(p.getId()).append("'");
                if (form.getParcelId() != null && form.getParcelId().equals(p.getId())) {
                    sb.append(" selected");
                }
                sb.append(">").append(escapeHtml(text)).append("</option>");
            }
            sb.append("</select></div>");
            if (availableParcels.isEmpty()) {
                sb.append("<div class='text-warning small mt-1'><i class='bi bi-exclamation-triangle me-1'></i>No available parcels found. Please create a parcel first.</div>");
            } else {
                sb.append("<div class='form-text'>Select a parcel for this household</div>");
            }
            sb.append("</div>");

            if (activePtos != null && !activePtos.isEmpty()) {
                sb.append("<div class='col-12'>");
                sb.append("<label for='ptoId' class='form-label'>PTO</label>");
                sb.append("<div class='input-group'>");
                sb.append("<span class='input-group-text'><i class='bi bi-file-earmark-text'></i></span>");
                sb.append("<select id='ptoId' name='ptoId' class='form-select'>");
                sb.append("<option value=''>— Select PTO —</option>");
                for (var pto : activePtos) {
                    String text = pto.getPtoNumber() + " - " + (pto.getPtoHolderName() != null ? pto.getPtoHolderName() : "");
                    sb.append("<option value='").append(pto.getId()).append("'");
                    if (form.getPtoId() != null && form.getPtoId().equals(pto.getId())) {
                        sb.append(" selected");
                    }
                    sb.append(">").append(escapeHtml(text)).append("</option>");
                }
                sb.append("</select></div>");
                sb.append("</div>");
            }

            sb.append("<div class='col-12 col-md-6'>");
            sb.append("<label for='registrationDate' class='form-label'>Registration Date</label>");
            sb.append("<div class='input-group'>");
            sb.append("<span class='input-group-text'><i class='bi bi-calendar'></i></span>");
            String regDate = form.getRegistrationDate() != null ? form.getRegistrationDate().toString() : "";
            sb.append("<input type='date' class='form-control' id='registrationDate' name='registrationDate' value='").append(escapeHtml(regDate)).append("'>");
            sb.append("</div></div>");

            sb.append("<div class='col-12'>");
            sb.append("<label for='notes' class='form-label'>Notes</label>");
            sb.append("<textarea id='notes' name='notes' class='form-control' rows='2' placeholder='Additional notes...'>").append(escapeHtml(form.getNotes())).append("</textarea>");
            sb.append("</div>");

            sb.append("</div></div></div>");

            sb.append("<div class='d-flex gap-3 justify-content-end mt-4 mb-4'>");
            sb.append("<a href='/households/").append(household.getId()).append("' class='btn btn-light px-4'><i class='bi bi-x me-1'></i>Cancel</a>");
            sb.append("<button type='submit' class='btn btn-navy px-4 fw-semibold'><i class='bi bi-save me-2'></i>Update Household</button>");
            sb.append("</div>");

            sb.append("</form></div></div></div>");

            sb.append(buildFooterHtml());

            return sb.toString();
        } catch (Throwable e) {
            log.error("Error loading edit form: {}", e.getMessage(), e);
            return "<html><body><h1>Error loading edit form</h1><p>" + e.getClass().getSimpleName() + " - " + e.getMessage() + "</p><a href='/households'>Back</a></body></html>";
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
