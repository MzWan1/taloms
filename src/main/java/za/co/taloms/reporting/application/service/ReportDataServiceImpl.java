package za.co.taloms.reporting.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import za.co.taloms.audit.application.dto.AuditLogResponse;
import za.co.taloms.audit.application.service.AuditService;
import za.co.taloms.businessoccupancy.application.dto.BusinessOccupancyResponse;
import za.co.taloms.businessoccupancy.application.service.BusinessOccupancyService;
import za.co.taloms.businessoccupancy.domain.entity.BusinessStatus;
import za.co.taloms.document.application.dto.DocumentResponse;
import za.co.taloms.document.application.service.DocumentService;
import za.co.taloms.household.application.dto.HouseholdResponse;
import za.co.taloms.household.application.service.HouseholdService;
import za.co.taloms.parcel.application.dto.ParcelResponse;
import za.co.taloms.parcel.application.service.ParcelService;
import za.co.taloms.parcel.domain.entity.ParcelStatus;
import za.co.taloms.parcel.domain.entity.ParcelType;
import za.co.taloms.pto.application.dto.PTOResponse;
import za.co.taloms.pto.application.service.PTOService;
import za.co.taloms.pto.domain.entity.PTOStatus;
import za.co.taloms.reporting.application.dto.ReportData;
import za.co.taloms.reporting.application.dto.ReportRequest;
import za.co.taloms.reporting.application.dto.ReportSection;
import za.co.taloms.reporting.domain.entity.ReportFormat;
import za.co.taloms.resident.application.dto.ResidentResponse;
import za.co.taloms.resident.application.service.ResidentService;
import za.co.taloms.security.application.dto.UserResponse;
import za.co.taloms.security.application.service.UserService;
import za.co.taloms.traditionalauthority.application.dto.TraditionalAuthorityResponse;
import za.co.taloms.traditionalauthority.application.dto.VillageResponse;
import za.co.taloms.traditionalauthority.application.service.TraditionalAuthorityService;
import za.co.taloms.traditionalauthority.application.service.VillageService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportDataServiceImpl implements ReportDataService {

    private final PTOService ptoService;
    private final ParcelService parcelService;
    private final ResidentService residentService;
    private final HouseholdService householdService;
    private final BusinessOccupancyService businessOccupancyService;
    private final AuditService auditService;
    private final DocumentService documentService;
    private final UserService userService;
    private final VillageService villageService;
    private final TraditionalAuthorityService traditionalAuthorityService;

    @Override
    public ReportData generatePtoOccupancyRegisterReport(ReportRequest request) {
        List<PTOResponse> ptos = fetchPtos(request);

        Map<String, String> metrics = new LinkedHashMap<>();
        metrics.put("Total PTOs", String.valueOf(ptos.size()));

        long pending = ptos.stream().filter(p -> p.getStatus() == PTOStatus.PENDING).count();
        long active = ptos.stream().filter(p -> p.getStatus() == PTOStatus.ACTIVE).count();
        long suspended = ptos.stream().filter(p -> p.getStatus() == PTOStatus.SUSPENDED).count();
        long revoked = ptos.stream().filter(p -> p.getStatus() == PTOStatus.REVOKED).count();
        long expired = ptos.stream().filter(p -> p.getStatus() == PTOStatus.EXPIRED).count();
        metrics.put("Pending Approval", String.valueOf(pending));
        metrics.put("Active", String.valueOf(active));
        metrics.put("Suspended", String.valueOf(suspended));
        metrics.put("Revoked", String.valueOf(revoked));
        metrics.put("Expired", String.valueOf(expired));

        long residential = ptos.stream().filter(p -> p.getPurpose().name().equals("RESIDENTIAL")).count();
        long business = ptos.stream().filter(p -> p.getPurpose().name().equals("BUSINESS")).count();
        long agricultural = ptos.stream().filter(p -> p.getPurpose().name().equals("AGRICULTURAL")).count();
        long mixedUse = ptos.stream().filter(p -> p.getPurpose().name().equals("MIXED_USE")).count();
        metrics.put("Residential Purpose", String.valueOf(residential));
        metrics.put("Business Purpose", String.valueOf(business));
        metrics.put("Agricultural Purpose", String.valueOf(agricultural));
        metrics.put("Mixed Use", String.valueOf(mixedUse));

        LocalDate now = LocalDate.now();
        long expiring30 = ptos.stream().filter(p -> p.getExpiryDate() != null
                && !p.getExpiryDate().isBefore(now)
                && !p.getExpiryDate().isAfter(now.plusDays(30))).count();
        long expiring60 = ptos.stream().filter(p -> p.getExpiryDate() != null
                && !p.getExpiryDate().isBefore(now)
                && !p.getExpiryDate().isAfter(now.plusDays(60))).count();
        long expiring90 = ptos.stream().filter(p -> p.getExpiryDate() != null
                && !p.getExpiryDate().isBefore(now)
                && !p.getExpiryDate().isAfter(now.plusDays(90))).count();
        metrics.put("Expiring within 30 days", String.valueOf(expiring30));
        metrics.put("Expiring within 60 days", String.valueOf(expiring60));
        metrics.put("Expiring within 90 days", String.valueOf(expiring90));

        Map<String, Long> byVillage = ptos.stream()
                .filter(p -> p.getVillageName() != null)
                .collect(Collectors.groupingBy(PTOResponse::getVillageName, Collectors.counting()));
        byVillage.forEach((v, c) -> metrics.put("Village: " + v, String.valueOf(c)));

        Map<Integer, Long> byYear = ptos.stream()
                .filter(p -> p.getIssueDate() != null)
                .collect(Collectors.groupingBy(p -> p.getIssueDate().getYear(), TreeMap::new, Collectors.counting()));
        byYear.forEach((y, c) -> metrics.put("Issued in " + y, String.valueOf(c)));

        double avgApprovalTime = ptos.stream()
                .filter(p -> p.getApprovedAt() != null && p.getCreatedAt() != null)
                .mapToDouble(p -> java.time.Duration.between(p.getCreatedAt(), p.getApprovedAt()).toDays())
                .average()
                .orElse(0.0);
        metrics.put("Avg Approval Time (days)", String.format("%.1f", avgApprovalTime));

        Map<String, Long> approvedBy = ptos.stream()
                .filter(p -> p.getApprovedBy() != null && !p.getApprovedBy().isBlank())
                .collect(Collectors.groupingBy(PTOResponse::getApprovedBy, Collectors.counting()));
        approvedBy.forEach((u, c) -> metrics.put("Approved By: " + u, String.valueOf(c)));

        List<String> headers = Arrays.asList("PTO Number", "Holder Name", "Purpose", "Status", "Village", "Issue Date", "Expiry Date", "Approved By");
        List<List<String>> rows = ptos.stream().map(p -> Arrays.asList(
                p.getPtoNumber() != null ? p.getPtoNumber() : "",
                p.getPtoHolderName() != null ? p.getPtoHolderName() : "",
                p.getPurposeDisplay() != null ? p.getPurposeDisplay() : "",
                p.getStatusDisplay() != null ? p.getStatusDisplay() : "",
                p.getVillageName() != null ? p.getVillageName() : "",
                p.getIssueDate() != null ? p.getIssueDate().toString() : "",
                p.getExpiryDate() != null ? p.getExpiryDate().toString() : "",
                p.getApprovedBy() != null ? p.getApprovedBy() : ""
        )).collect(Collectors.toList());

        ReportSection section = new ReportSection(
                "PTO Occupancy Summary",
                List.of(new HashMap<>(metrics)),
                headers,
                rows
        );

        List<ReportSection> sections = List.of(section);

        TraditionalAuthorityResponse auth = getAuthority(request.getAuthorityId());
        VillageResponse village = getVillage(request.getVillageId());

        return new ReportData(
                "PTO Occupancy Register",
                auth != null ? auth.getAuthorityName() : null,
                village != null ? village.getVillageName() : null,
                request.getDateFrom(),
                request.getDateTo(),
                sections,
                ptos,
                null, null, null, null, null, null
        );
    }

    @Override
    public ReportData generateLandParcelUtilisationReport(ReportRequest request) {
        List<ParcelResponse> parcels = fetchAllParcels(request);

        Map<String, String> metrics = new LinkedHashMap<>();
        metrics.put("Total Parcels", String.valueOf(parcels.size()));

        long available = parcels.stream().filter(p -> p.getStatus() == ParcelStatus.AVAILABLE).count();
        long allocated = parcels.stream().filter(p -> p.getStatus() == ParcelStatus.ALLOCATED).count();
        long disputed = parcels.stream().filter(p -> p.getStatus() == ParcelStatus.DISPUTED).count();
        long reserved = parcels.stream().filter(p -> p.getStatus() == ParcelStatus.RESERVED).count();
        long inactive = parcels.stream().filter(p -> p.getStatus() == ParcelStatus.INACTIVE).count();
        metrics.put("Available", String.valueOf(available));
        metrics.put("Allocated", String.valueOf(allocated));
        metrics.put("Disputed", String.valueOf(disputed));
        metrics.put("Reserved", String.valueOf(reserved));
        metrics.put("Inactive", String.valueOf(inactive));

        double totalArea = parcels.stream().mapToDouble(p -> p.getAreaM2() != null ? p.getAreaM2() : 0.0).sum();
        double allocatedArea = parcels.stream().filter(p -> p.getStatus() == ParcelStatus.ALLOCATED)
                .mapToDouble(p -> p.getAreaM2() != null ? p.getAreaM2() : 0.0).sum();
        double availableArea = parcels.stream().filter(p -> p.getStatus() == ParcelStatus.AVAILABLE)
                .mapToDouble(p -> p.getAreaM2() != null ? p.getAreaM2() : 0.0).sum();
        metrics.put("Total Area (m²)", String.format("%.2f", totalArea));
        metrics.put("Allocated Area (m²)", String.format("%.2f", allocatedArea));
        metrics.put("Available Area (m²)", String.format("%.2f", availableArea));

        Map<Enum<?>, Long> byType = parcels.stream()
                .filter(p -> p.getParcelType() != null)
                .collect(Collectors.groupingBy(ParcelResponse::getParcelType, TreeMap::new, Collectors.counting()));
        byType.forEach((t, c) -> metrics.put(t.name(), String.valueOf(c)));

        long unallocated = parcels.stream().filter(p -> p.getPtoId() == null).count();
        metrics.put("Unallocated (No PTO)", String.valueOf(unallocated));

        OptionalDouble largest = parcels.stream().mapToDouble(p -> p.getAreaM2() != null ? p.getAreaM2() : 0.0).max();
        OptionalDouble smallest = parcels.stream().mapToDouble(p -> p.getAreaM2() != null ? p.getAreaM2() : 0.0).filter(d -> d > 0).min();
        metrics.put("Largest Stand Area (m²)", largest.isPresent() ? String.format("%.2f", largest.getAsDouble()) : "0");
        metrics.put("Smallest Stand Area (m²)", smallest.isPresent() ? String.format("%.2f", smallest.getAsDouble()) : "0");

        metrics.put("Disputed Count", String.valueOf(disputed));

        List<String> disputedHeaders = Arrays.asList("Parcel #", "Stand #", "Type", "Village", "Area (m²)", "Status");
        List<List<String>> disputedRows = parcels.stream()
                .filter(p -> p.getStatus() == ParcelStatus.DISPUTED)
                .map(p -> Arrays.asList(
                        p.getParcelNumber() != null ? p.getParcelNumber() : "",
                        p.getStandNumber() != null ? p.getStandNumber() : "",
                        p.getParcelTypeDisplay() != null ? p.getParcelTypeDisplay() : "",
                        p.getVillageName() != null ? p.getVillageName() : "",
                        p.getAreaM2() != null ? p.getAreaM2().toString() : "",
                        p.getStatusDisplay() != null ? p.getStatusDisplay() : ""
                )).collect(Collectors.toList());

        List<String> allHeaders = Arrays.asList("Parcel #", "Stand #", "Type", "Status", "Village", "Area (m²)", "PTO Linked");
        List<List<String>> allRows = parcels.stream().map(p -> Arrays.asList(
                p.getParcelNumber() != null ? p.getParcelNumber() : "",
                p.getStandNumber() != null ? p.getStandNumber() : "",
                p.getParcelTypeDisplay() != null ? p.getParcelTypeDisplay() : "",
                p.getStatusDisplay() != null ? p.getStatusDisplay() : "",
                p.getVillageName() != null ? p.getVillageName() : "",
                p.getAreaM2() != null ? p.getAreaM2().toString() : "",
                p.getPtoId() != null ? "Yes" : "No"
        )).collect(Collectors.toList());

        List<ReportSection> sections = new ArrayList<>();
        ReportSection summary = new ReportSection(
                "Land Parcel Utilisation Summary",
                List.of(new HashMap<>(metrics)),
                allHeaders,
                allRows
        );
        sections.add(summary);

        if (!disputedRows.isEmpty()) {
            ReportSection disputedSection = new ReportSection(
                    "Disputed Parcels",
                    Collections.emptyList(),
                    disputedHeaders,
                    disputedRows
            );
            sections.add(disputedSection);
        }

        TraditionalAuthorityResponse auth = getAuthority(request.getAuthorityId());
        VillageResponse village = getVillage(request.getVillageId());

        return new ReportData(
                "Land Parcel Utilisation Report",
                auth != null ? auth.getAuthorityName() : null,
                village != null ? village.getVillageName() : null,
                request.getDateFrom(),
                request.getDateTo(),
                sections,
                null,
                parcels,
                null, null, null, null, null
        );
    }

    @Override
    public ReportData generateStandAllocationReport(ReportRequest request) {
        List<ParcelResponse> parcels = fetchAllParcels(request);
        List<PTOResponse> ptos = fetchPtos(request);

        LocalDate dateFrom = request.getDateFrom() != null ? request.getDateFrom() : LocalDate.now().minusMonths(1);
        LocalDate dateTo = request.getDateTo() != null ? request.getDateTo() : LocalDate.now();

        Map<String, String> metrics = new LinkedHashMap<>();
        long total = parcels.size();
        long available = parcels.stream().filter(p -> p.getStatus() == ParcelStatus.AVAILABLE).count();
        long allocatedThisPeriod = ptos.stream()
                .filter(p -> p.getIssueDate() != null
                        && !p.getIssueDate().isBefore(dateFrom)
                        && !p.getIssueDate().isAfter(dateTo))
                .count();
        long pendingApproval = ptos.stream().filter(p -> p.getStatus() == PTOStatus.PENDING).count();
        double allocationRate = total > 0 ? (allocatedThisPeriod * 100.0 / total) : 0.0;

        metrics.put("Total Stands", String.valueOf(total));
        metrics.put("Available Stands", String.valueOf(available));
        metrics.put("Allocated This Period", String.valueOf(allocatedThisPeriod));
        metrics.put("Allocation Rate", String.format("%.1f%%", allocationRate));
        metrics.put("Pending Approval", String.valueOf(pendingApproval));

        Map<String, Long> availableByVillage = parcels.stream()
                .filter(p -> p.getStatus() == ParcelStatus.AVAILABLE && p.getVillageName() != null)
                .collect(Collectors.groupingBy(ParcelResponse::getVillageName, TreeMap::new, Collectors.counting()));
        availableByVillage.forEach((v, c) -> metrics.put("Available - " + v, String.valueOf(c)));

        List<String> headers = Arrays.asList("Parcel #", "Stand #", "Type", "Village", "Area (m²)", "Status");
        List<List<String>> rows = parcels.stream()
                .filter(p -> p.getStatus() == ParcelStatus.AVAILABLE)
                .map(p -> Arrays.asList(
                        p.getParcelNumber() != null ? p.getParcelNumber() : "",
                        p.getStandNumber() != null ? p.getStandNumber() : "",
                        p.getParcelTypeDisplay() != null ? p.getParcelTypeDisplay() : "",
                        p.getVillageName() != null ? p.getVillageName() : "",
                        p.getAreaM2() != null ? p.getAreaM2().toString() : "",
                        p.getStatusDisplay() != null ? p.getStatusDisplay() : ""
                )).collect(Collectors.toList());

        ReportSection section = new ReportSection(
                "Stand Allocation Summary",
                List.of(new HashMap<>(metrics)),
                headers,
                rows
        );

        TraditionalAuthorityResponse auth = getAuthority(request.getAuthorityId());
        VillageResponse village = getVillage(request.getVillageId());

        return new ReportData(
                "Stand Allocation Report",
                auth != null ? auth.getAuthorityName() : null,
                village != null ? village.getVillageName() : null,
                dateFrom,
                dateTo,
                List.of(section),
                null,
                parcels,
                null, null, null, null, null
        );
    }

    @Override
    public ReportData generateVillagePopulationReport(ReportRequest request) {
        List<ResidentResponse> residents = residentService.findAll();
        List<HouseholdResponse> households = householdService.findAll();

        if (request.getVillageId() != null) {
            residents = residents.stream()
                    .filter(r -> r.getVillageName() != null)
                    .collect(Collectors.toList());
            households = households.stream()
                    .filter(h -> h.getVillageName() != null)
                    .collect(Collectors.toList());
        }
        if (request.getAuthorityId() != null) {
            TraditionalAuthorityResponse auth = getAuthority(request.getAuthorityId());
            if (auth != null) {
                residents = residents.stream()
                        .filter(r -> auth.getAuthorityName().equals(r.getAuthorityName()))
                        .collect(Collectors.toList());
                households = households.stream()
                        .filter(h -> auth.getAuthorityName().equals(h.getAuthorityName()))
                        .collect(Collectors.toList());
            }
        }

        Map<String, String> metrics = new LinkedHashMap<>();
        metrics.put("Total Population", String.valueOf(residents.size()));
        metrics.put("Total Households", String.valueOf(households.size()));

        long male = residents.stream().filter(r -> r.getGender() != null && r.getGender().name().equals("MALE")).count();
        long female = residents.stream().filter(r -> r.getGender() != null && r.getGender().name().equals("FEMALE")).count();
        metrics.put("Male", String.valueOf(male));
        metrics.put("Female", String.valueOf(female));

        long under18 = residents.stream().filter(r -> r.getAge() != null && r.getAge() < 18).count();
        long age18to35 = residents.stream().filter(r -> r.getAge() != null && r.getAge() >= 18 && r.getAge() <= 35).count();
        long age36to60 = residents.stream().filter(r -> r.getAge() != null && r.getAge() >= 36 && r.getAge() <= 60).count();
        long over60 = residents.stream().filter(r -> r.getAge() != null && r.getAge() > 60).count();
        metrics.put("Under 18", String.valueOf(under18));
        metrics.put("18-35", String.valueOf(age18to35));
        metrics.put("36-60", String.valueOf(age36to60));
        metrics.put("Over 60", String.valueOf(over60));

        Map<String, Long> householdsPerVillage = households.stream()
                .filter(h -> h.getVillageName() != null)
                .collect(Collectors.groupingBy(HouseholdResponse::getVillageName, TreeMap::new, Collectors.counting()));
        householdsPerVillage.forEach((v, c) -> metrics.put("Households in " + v, String.valueOf(c)));

        double avgHouseholdSize = households.isEmpty() ? 0.0 : (double) residents.size() / households.size();
        metrics.put("Average Household Size", String.format("%.1f", avgHouseholdSize));

        long householdHeads = residents.stream()
                .filter(r -> r.getRelationshipType() != null && r.getRelationshipType().name().equals("HOUSEHOLD_HEAD"))
                .count();
        metrics.put("Household Heads", String.valueOf(householdHeads));

        long childrenUnder18 = residents.stream().filter(r -> r.getAge() != null && r.getAge() < 18).count();
        metrics.put("Children Under 18", String.valueOf(childrenUnder18));

        long elderly65 = residents.stream().filter(r -> r.getAge() != null && r.getAge() >= 65).count();
        metrics.put("Elderly (65+)", String.valueOf(elderly65));

        List<String> headers = Arrays.asList("Full Name", "Gender", "ID Number", "Village", "Stand Number");
        List<List<String>> rows = residents.stream()
                .filter(r -> r.getRelationshipType() != null && r.getRelationshipType().name().equals("HOUSEHOLD_HEAD"))
                .map(r -> Arrays.asList(
                        r.getFullName() != null ? r.getFullName() : "",
                        r.getGenderDisplay() != null ? r.getGenderDisplay() : "",
                        r.getIdNumber() != null ? r.getIdNumber() : "",
                        r.getVillageName() != null ? r.getVillageName() : "",
                        r.getStandNumber() != null ? r.getStandNumber() : ""
                )).collect(Collectors.toList());

        ReportSection section = new ReportSection(
                "Village Population Summary",
                List.of(new HashMap<>(metrics)),
                headers,
                rows
        );

        TraditionalAuthorityResponse auth = getAuthority(request.getAuthorityId());
        VillageResponse village = getVillage(request.getVillageId());

        return new ReportData(
                "Village Population Report",
                auth != null ? auth.getAuthorityName() : null,
                village != null ? village.getVillageName() : null,
                request.getDateFrom(),
                request.getDateTo(),
                List.of(section),
                null,
                null,
                residents, households, null, null, null
        );
    }

    @Override
    public ReportData generateHouseholdRegisterReport(ReportRequest request) {
        List<HouseholdResponse> households = householdService.findAll();
        LocalDate now = LocalDate.now();

        Map<String, String> metrics = new LinkedHashMap<>();
        metrics.put("Total Households", String.valueOf(households.size()));
        long active = households.stream().filter(h -> Boolean.TRUE.equals(h.getActive())).count();
        long inactive = households.stream().filter(h -> !Boolean.TRUE.equals(h.getActive())).count();
        metrics.put("Active", String.valueOf(active));
        metrics.put("Inactive", String.valueOf(inactive));

        long noPto = households.stream().filter(h -> h.getPtoId() == null).count();
        metrics.put("With No PTO", String.valueOf(noPto));

        long singleMember = households.stream()
                .filter(h -> h.getHouseholdHeadIdNumber() != null && h.getHouseholdHeadName() != null)
                .count();
        metrics.put("Single-Member", String.valueOf(singleMember));

        long newThisYear = households.stream()
                .filter(h -> h.getRegistrationDate() != null && h.getRegistrationDate().getYear() == now.getYear())
                .count();
        metrics.put("New This Year", String.valueOf(newThisYear));

        Map<String, Long> byVillage = households.stream()
                .filter(h -> h.getVillageName() != null)
                .collect(Collectors.groupingBy(HouseholdResponse::getVillageName, TreeMap::new, Collectors.counting()));
        byVillage.forEach((v, c) -> metrics.put("Village: " + v, String.valueOf(c)));

        long withParcelAllocation = households.stream().filter(h -> h.getParcelId() != null).count();
        metrics.put("With Parcel Allocation", String.valueOf(withParcelAllocation));

        List<String> headers = Arrays.asList("Household ID", "Head Name", "ID Number", "Village", "Stand #", "Parcel #", "PTO #", "Active", "Registration Date");
        List<List<String>> rows = households.stream().map(h -> Arrays.asList(
                h.getId() != null ? h.getId().toString() : "",
                h.getHouseholdHeadName() != null ? h.getHouseholdHeadName() : "",
                h.getHouseholdHeadIdNumber() != null ? h.getHouseholdHeadIdNumber() : "",
                h.getVillageName() != null ? h.getVillageName() : "",
                h.getStandNumber() != null ? h.getStandNumber() : "",
                h.getParcelNumber() != null ? h.getParcelNumber() : "",
                h.getPtoNumber() != null ? h.getPtoNumber() : "",
                Boolean.TRUE.equals(h.getActive()) ? "Yes" : "No",
                h.getRegistrationDate() != null ? h.getRegistrationDate().toString() : ""
        )).collect(Collectors.toList());

        ReportSection section = new ReportSection(
                "Household Register Summary",
                List.of(new HashMap<>(metrics)),
                headers,
                rows
        );

        TraditionalAuthorityResponse auth = getAuthority(request.getAuthorityId());
        VillageResponse village = getVillage(request.getVillageId());

        return new ReportData(
                "Household Register",
                auth != null ? auth.getAuthorityName() : null,
                village != null ? village.getVillageName() : null,
                request.getDateFrom(),
                request.getDateTo(),
                List.of(section),
                null, null, null,
                households, null, null, null
        );
    }

    @Override
    public ReportData generateResidentDemographicsReport(ReportRequest request) {
        List<ResidentResponse> residents = residentService.findAll();
        List<HouseholdResponse> households = householdService.findAll();

        Map<String, String> metrics = new LinkedHashMap<>();
        metrics.put("Total Registered", String.valueOf(residents.size()));
        long active = residents.stream().filter(r -> Boolean.TRUE.equals(r.getActive())).count();
        long inactive = residents.stream().filter(r -> !Boolean.TRUE.equals(r.getActive())).count();
        metrics.put("Active", String.valueOf(active));
        metrics.put("Inactive", String.valueOf(inactive));

        Map<Enum<?>, Long> byRelationship = residents.stream()
                .filter(r -> r.getRelationshipType() != null)
                .collect(Collectors.groupingBy(ResidentResponse::getRelationshipType, TreeMap::new, Collectors.counting()));
        byRelationship.forEach((r, c) -> metrics.put(r.name(), String.valueOf(c)));

        Map<String, Long> genderByVillage = residents.stream()
                .filter(r -> r.getVillageName() != null && r.getGender() != null)
                .collect(Collectors.groupingBy(ResidentResponse::getVillageName, TreeMap::new, Collectors.counting()));
        genderByVillage.forEach((v, c) -> metrics.put("Population - " + v, String.valueOf(c)));

        long under18 = residents.stream().filter(r -> r.getAge() != null && r.getAge() < 18).count();
        long age18to35 = residents.stream().filter(r -> r.getAge() != null && r.getAge() >= 18 && r.getAge() <= 35).count();
        long age36to60 = residents.stream().filter(r -> r.getAge() != null && r.getAge() >= 36 && r.getAge() <= 60).count();
        long over60 = residents.stream().filter(r -> r.getAge() != null && r.getAge() > 60).count();
        metrics.put("Under 18", String.valueOf(under18));
        metrics.put("18-35", String.valueOf(age18to35));
        metrics.put("36-60", String.valueOf(age36to60));
        metrics.put("Over 60", String.valueOf(over60));

        long noId = residents.stream().filter(r -> r.getIdNumber() == null || r.getIdNumber().isBlank()).count();
        metrics.put("No ID Number", String.valueOf(noId));

        LocalDate thisMonth = YearMonth.from(LocalDate.now()).atDay(1);
        long newRegistrations = residents.stream()
                .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().toLocalDate().isAfter(thisMonth))
                .count();
        metrics.put("New Registrations This Month", String.valueOf(newRegistrations));

        double avgPerHousehold = households.isEmpty() ? 0.0 : (double) residents.size() / households.size();
        metrics.put("Avg Residents per Household", String.format("%.1f", avgPerHousehold));

        List<String> headers = Arrays.asList("Name", "Gender", "Relationship", "Date of Birth", "Age", "Village", "Stand #", "Household", "ID Number");
        List<List<String>> rows = residents.stream().map(r -> Arrays.asList(
                r.getFullName() != null ? r.getFullName() : "",
                r.getGenderDisplay() != null ? r.getGenderDisplay() : "",
                r.getRelationshipDisplay() != null ? r.getRelationshipDisplay() : "",
                r.getDateOfBirth() != null ? r.getDateOfBirth().toString() : "",
                r.getAge() != null ? r.getAge().toString() : "",
                r.getVillageName() != null ? r.getVillageName() : "",
                r.getStandNumber() != null ? r.getStandNumber() : "",
                r.getHouseholdHeadName() != null ? r.getHouseholdHeadName() : "",
                r.getIdNumber() != null ? r.getIdNumber() : ""
        )).collect(Collectors.toList());

        ReportSection section = new ReportSection(
                "Resident Demographics Summary",
                List.of(new HashMap<>(metrics)),
                headers,
                rows
        );

        TraditionalAuthorityResponse auth = getAuthority(request.getAuthorityId());
        VillageResponse village = getVillage(request.getVillageId());

        return new ReportData(
                "Resident Demographics Report",
                auth != null ? auth.getAuthorityName() : null,
                village != null ? village.getVillageName() : null,
                request.getDateFrom(),
                request.getDateTo(),
                List.of(section),
                null, null,
                residents, households, null, null, null
        );
    }

    @Override
    public ReportData generateBusinessOccupancyReport(ReportRequest request) {
        List<BusinessOccupancyResponse> businesses = fetchAllBusinesses(request);

        Map<String, String> metrics = new LinkedHashMap<>();
        metrics.put("Total Businesses", String.valueOf(businesses.size()));

        Map<Enum<?>, Long> byType = businesses.stream()
                .filter(b -> b.getBusinessType() != null)
                .collect(Collectors.groupingBy(BusinessOccupancyResponse::getBusinessType, TreeMap::new, Collectors.counting()));
        byType.forEach((t, c) -> metrics.put(t.name(), String.valueOf(c)));

        long active = businesses.stream().filter(b -> b.getStatus() == BusinessStatus.ACTIVE).count();
        long inactive = businesses.stream().filter(b -> b.getStatus() == BusinessStatus.INACTIVE).count();
        long pending = businesses.stream().filter(b -> b.getStatus() == BusinessStatus.PENDING).count();
        metrics.put("Active", String.valueOf(active));
        metrics.put("Inactive", String.valueOf(inactive));
        metrics.put("Pending", String.valueOf(pending));

        Map<String, Long> perVillage = businesses.stream()
                .filter(b -> b.getVillageName() != null)
                .collect(Collectors.groupingBy(BusinessOccupancyResponse::getVillageName, TreeMap::new, Collectors.counting()));
        perVillage.forEach((v, c) -> metrics.put("Village: " + v, String.valueOf(c)));

        long withValidPto = businesses.stream().filter(b -> b.getPtoId() != null).count();
        metrics.put("With Valid PTO", String.valueOf(withValidPto));

        LocalDate thisYearStart = LocalDate.of(LocalDate.now().getYear(), 1, 1);
        long newThisYear = businesses.stream()
                .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().toLocalDate().isAfter(thisYearStart))
                .count();
        metrics.put("New This Year", String.valueOf(newThisYear));

        List<String> headers = Arrays.asList("Business Name", "Type", "Owner", "Contact", "Village", "Stand #", "Parcel #", "PTO #", "Status", "Registered Date");
        List<List<String>> rows = businesses.stream().map(b -> Arrays.asList(
                b.getBusinessName() != null ? b.getBusinessName() : "",
                b.getBusinessTypeDisplay() != null ? b.getBusinessTypeDisplay() : "",
                b.getOwnerName() != null ? b.getOwnerName() : "",
                b.getContactPhone() != null ? b.getContactPhone() : "",
                b.getVillageName() != null ? b.getVillageName() : "",
                b.getStandNumber() != null ? b.getStandNumber() : "",
                b.getParcelNumber() != null ? b.getParcelNumber() : "",
                b.getPtoNumber() != null ? b.getPtoNumber() : "",
                b.getStatusDisplay() != null ? b.getStatusDisplay() : "",
                b.getCreatedAt() != null ? b.getCreatedAt().toLocalDate().toString() : ""
        )).collect(Collectors.toList());

        ReportSection section = new ReportSection(
                "Business Occupancy Summary",
                List.of(new HashMap<>(metrics)),
                headers,
                rows
        );

        TraditionalAuthorityResponse auth = getAuthority(request.getAuthorityId());
        VillageResponse village = getVillage(request.getVillageId());

        return new ReportData(
                "Business Occupancy Report",
                auth != null ? auth.getAuthorityName() : null,
                village != null ? village.getVillageName() : null,
                request.getDateFrom(),
                request.getDateTo(),
                List.of(section),
                null, null, null, null,
                businesses, null, null
        );
    }

    @Override
    public ReportData generateEconomicActivityReport(ReportRequest request) {
        List<ParcelResponse> parcels = fetchAllParcels(request);
        List<BusinessOccupancyResponse> businesses = fetchAllBusinesses(request);
        List<HouseholdResponse> households = householdService.findAll();

        Map<String, String> metrics = new LinkedHashMap<>();

        double commercialArea = parcels.stream()
                .filter(p -> p.getParcelType() == ParcelType.BUSINESS)
                .mapToDouble(p -> p.getAreaM2() != null ? p.getAreaM2() : 0.0).sum();
        double agriculturalArea = parcels.stream()
                .filter(p -> p.getParcelType() == ParcelType.AGRICULTURAL)
                .mapToDouble(p -> p.getAreaM2() != null ? p.getAreaM2() : 0.0).sum();
        double residentialArea = parcels.stream()
                .filter(p -> p.getParcelType() == ParcelType.RESIDENTIAL)
                .mapToDouble(p -> p.getAreaM2() != null ? p.getAreaM2() : 0.0).sum();
        metrics.put("Commercial Land Area (m²)", String.format("%.2f", commercialArea));
        metrics.put("Agricultural Land Area (m²)", String.format("%.2f", agriculturalArea));
        metrics.put("Residential Land Area (m²)", String.format("%.2f", residentialArea));

        double residentialLand = residentialArea;
        double businessToResidential = residentialLand > 0 ? (businesses.size() * 100.0 / residentialLand) : 0.0;
        metrics.put("Business to Residential Ratio", String.format("%.2f", businessToResidential));

        double businessesPer100Households = households.isEmpty() ? 0.0 : (businesses.size() * 100.0 / households.size());
        metrics.put("Businesses per 100 Households", String.format("%.2f", businessesPer100Households));

        List<String> headers = Arrays.asList("Business Name", "Type", "Owner", "Village", "Status", "Employees");
        List<List<String>> rows = businesses.stream().map(b -> Arrays.asList(
                b.getBusinessName() != null ? b.getBusinessName() : "",
                b.getBusinessTypeDisplay() != null ? b.getBusinessTypeDisplay() : "",
                b.getOwnerName() != null ? b.getOwnerName() : "",
                b.getVillageName() != null ? b.getVillageName() : "",
                b.getStatusDisplay() != null ? b.getStatusDisplay() : "",
                b.getEmployeesCount() != null ? b.getEmployeesCount().toString() : ""
        )).collect(Collectors.toList());

        ReportSection section = new ReportSection(
                "Economic Activity Summary",
                List.of(new HashMap<>(metrics)),
                headers,
                rows
        );

        TraditionalAuthorityResponse auth = getAuthority(request.getAuthorityId());
        VillageResponse village = getVillage(request.getVillageId());

        return new ReportData(
                "Economic Activity Report",
                auth != null ? auth.getAuthorityName() : null,
                village != null ? village.getVillageName() : null,
                request.getDateFrom(),
                request.getDateTo(),
                List.of(section),
                null,
                parcels,
                null, null,
                businesses, null, null
        );
    }

    @Override
    public ReportData generateUserActivityAuditReport(ReportRequest request) {
        List<AuditLogResponse> auditLogs = auditService.findAll();
        List<UserResponse> users = userService.findAll();

        Map<String, String> metrics = new LinkedHashMap<>();
        metrics.put("Total Users", String.valueOf(users.size()));
        long activeUsers = users.stream().filter(u -> Boolean.TRUE.equals(u.getEnabled())).count();
        long lockedUsers = users.stream().filter(u -> Boolean.TRUE.equals(u.getAccountLocked())).count();
        metrics.put("Active Users", String.valueOf(activeUsers));
        metrics.put("Locked Users", String.valueOf(lockedUsers));
        metrics.put("Total Audit Logs", String.valueOf(auditLogs.size()));

        Map<String, Long> actionsByUser = auditLogs.stream()
                .filter(a -> a.getPerformedBy() != null)
                .collect(Collectors.groupingBy(AuditLogResponse::getPerformedBy, TreeMap::new, Collectors.counting()));
        actionsByUser.forEach((u, c) -> metrics.put("Actions by " + u, String.valueOf(c)));

        Map<String, Long> byAction = auditLogs.stream()
                .filter(a -> a.getAction() != null)
                .collect(Collectors.groupingBy(a -> a.getAction().name(), TreeMap::new, Collectors.counting()));
        byAction.forEach((a, c) -> metrics.put("Action: " + a, String.valueOf(c)));

        long failedLogins = users.stream().mapToInt(u -> u.getFailedLoginAttempts() != null ? u.getFailedLoginAttempts() : 0).sum();
        metrics.put("Failed Login Attempts", String.valueOf(failedLogins));

        long revokedPtos = auditLogs.stream()
                .filter(a -> a.getEntityType() != null && a.getEntityType().toLowerCase().contains("pto")
                        && a.getAction() != null && a.getAction().name().equals("REVOKE"))
                .count();
        metrics.put("Revoked PTOs", String.valueOf(revokedPtos));

        List<String> headers = Arrays.asList("Date", "User", "Action", "Entity", "Entity ID", "Description", "IP Address");
        List<List<String>> rows = auditLogs.stream().map(a -> Arrays.asList(
                a.getPerformedAt() != null ? a.getPerformedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "",
                a.getPerformedBy() != null ? a.getPerformedBy() : "",
                a.getActionDisplay() != null ? a.getActionDisplay() : "",
                a.getEntityType() != null ? a.getEntityType() : "",
                a.getEntityId() != null ? a.getEntityId().toString() : "",
                a.getDescription() != null ? a.getDescription() : "",
                a.getIpAddress() != null ? a.getIpAddress() : ""
        )).collect(Collectors.toList());

        ReportSection section = new ReportSection(
                "User Activity Summary",
                List.of(new HashMap<>(metrics)),
                headers,
                rows
        );

        TraditionalAuthorityResponse auth = getAuthority(request.getAuthorityId());
        VillageResponse village = getVillage(request.getVillageId());

        return new ReportData(
                "User Activity and Audit Report",
                auth != null ? auth.getAuthorityName() : null,
                village != null ? village.getVillageName() : null,
                request.getDateFrom(),
                request.getDateTo(),
                List.of(section),
                null, null, null, null, null,
                auditLogs, null
        );
    }

    @Override
    public ReportData generateDocumentManagementReport(ReportRequest request) {
        List<DocumentResponse> documents = documentService.findAll();
        LocalDate now = LocalDate.now();
        YearMonth thisMonth = YearMonth.now();

        Map<String, String> metrics = new LinkedHashMap<>();
        metrics.put("Total Documents", String.valueOf(documents.size()));

        Map<Enum<?>, Long> byType = documents.stream()
                .filter(d -> d.getDocumentType() != null)
                .collect(Collectors.groupingBy(DocumentResponse::getDocumentType, TreeMap::new, Collectors.counting()));
        byType.forEach((t, c) -> metrics.put(t.name(), String.valueOf(c)));

        long uploadedThisMonth = documents.stream()
                .filter(d -> d.getUploadedAt() != null && YearMonth.from(d.getUploadedAt().toLocalDate()).equals(thisMonth))
                .count();
        metrics.put("Uploaded This Month", String.valueOf(uploadedThisMonth));

        long totalStorageBytes = documents.stream().mapToLong(d -> d.getFileSize() != null ? d.getFileSize() : 0L).sum();
        double totalStorageMB = totalStorageBytes / (1024.0 * 1024.0);
        metrics.put("Total Storage (MB)", String.format("%.2f", totalStorageMB));

        List<String> headers = Arrays.asList("Filename", "Type", "Entity", "Uploaded By", "Date", "Size (MB)", "Status");
        List<List<String>> rows = documents.stream().map(d -> Arrays.asList(
                d.getOriginalFilename() != null ? d.getOriginalFilename() : "",
                d.getDocumentTypeDisplay() != null ? d.getDocumentTypeDisplay() : "",
                d.getEntityTypeDisplay() != null ? d.getEntityTypeDisplay() : "",
                d.getUploadedBy() != null ? d.getUploadedBy() : "",
                d.getUploadedAt() != null ? d.getUploadedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "",
                d.getFileSize() != null ? String.format("%.2f", d.getFileSize() / (1024.0 * 1024.0)) : "0",
                d.getStatusDisplay() != null ? d.getStatusDisplay() : ""
        )).collect(Collectors.toList());

        ReportSection section = new ReportSection(
                "Document Management Summary",
                List.of(new HashMap<>(metrics)),
                headers,
                rows
        );

        TraditionalAuthorityResponse auth = getAuthority(request.getAuthorityId());
        VillageResponse village = getVillage(request.getVillageId());

        return new ReportData(
                "Document Management Report",
                auth != null ? auth.getAuthorityName() : null,
                village != null ? village.getVillageName() : null,
                request.getDateFrom(),
                request.getDateTo(),
                List.of(section),
                null, null, null, null, null, null,
                documents
        );
    }

    @Override
    public ReportData generatePerformanceDashboardReport(ReportRequest request) {
        List<PTOResponse> ptos = ptoService.findAll();
        List<ResidentResponse> residents = residentService.findAll();
        List<HouseholdResponse> households = householdService.findAll();
        List<BusinessOccupancyResponse> businesses = businessOccupancyService.findAll();
        List<ParcelResponse> parcels = parcelService.findAll();

        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);

        Map<String, String> metrics = new LinkedHashMap<>();

        long newPtos = ptos.stream()
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().toLocalDate().isAfter(monthStart))
                .count();
        metrics.put("New PTOs (This Month)", String.valueOf(newPtos));

        long approvedPtos = ptos.stream()
                .filter(p -> p.getApprovedAt() != null && p.getApprovedAt().toLocalDate().isAfter(monthStart))
                .count();
        metrics.put("Approved PTOs (This Month)", String.valueOf(approvedPtos));

        long pendingOver30 = ptos.stream()
                .filter(p -> p.getStatus() == PTOStatus.PENDING && p.getCreatedAt() != null)
                .filter(p -> java.time.Duration.between(p.getCreatedAt().toLocalDate().atStartOfDay(), now.atStartOfDay()).toDays() > 30)
                .count();
        metrics.put("Pending > 30 Days", String.valueOf(pendingOver30));

        long newResidents = residents.stream()
                .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().toLocalDate().isAfter(monthStart))
                .count();
        metrics.put("New Residents (This Month)", String.valueOf(newResidents));

        long newHouseholds = households.stream()
                .filter(h -> h.getCreatedAt() != null && h.getCreatedAt().toLocalDate().isAfter(monthStart))
                .count();
        metrics.put("New Households (This Month)", String.valueOf(newHouseholds));

        long newBusinesses = businesses.stream()
                .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().toLocalDate().isAfter(monthStart))
                .count();
        metrics.put("New Businesses (This Month)", String.valueOf(newBusinesses));

        long newParcels = parcels.stream()
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().toLocalDate().isAfter(monthStart))
                .count();
        metrics.put("New Parcels (This Month)", String.valueOf(newParcels));

        long availableLandCount = parcels.stream().filter(p -> p.getStatus() == ParcelStatus.AVAILABLE).count();
        double availableLandArea = parcels.stream()
                .filter(p -> p.getStatus() == ParcelStatus.AVAILABLE)
                .mapToDouble(p -> p.getAreaM2() != null ? p.getAreaM2() : 0.0).sum();
        metrics.put("Available Land (Count)", String.valueOf(availableLandCount));
        metrics.put("Available Land Area (m²)", String.format("%.2f", availableLandArea));

        ReportSection section = new ReportSection(
                "Authority Performance",
                List.of(new HashMap<>(metrics)),
                Collections.emptyList(),
                Collections.emptyList()
        );

        TraditionalAuthorityResponse auth = getAuthority(request.getAuthorityId());
        VillageResponse village = getVillage(request.getVillageId());

        return new ReportData(
                "Authority Performance Dashboard",
                auth != null ? auth.getAuthorityName() : null,
                village != null ? village.getVillageName() : null,
                monthStart,
                now,
                List.of(section),
                ptos,
                parcels,
                residents, households, businesses, null, null
        );
    }

    @Override
    public ReportData generateLandBoundaryReport(ReportRequest request) {
        List<ParcelResponse> parcels = fetchAllParcels(request);

        Map<String, String> metrics = new LinkedHashMap<>();

        long withBoundaries = parcels.stream().filter(p -> p.getBoundaryCount() != null && p.getBoundaryCount() > 0).count();
        long withoutBoundaries = parcels.stream().filter(p -> p.getBoundaryCount() == null || p.getBoundaryCount() == 0).count();
        long disputed = parcels.stream().filter(p -> p.getStatus() == ParcelStatus.DISPUTED).count();
        double totalAreaHectares = parcels.stream().mapToDouble(p -> p.getAreaHectares() != null ? p.getAreaHectares() : 0.0).sum();

        metrics.put("Total Parcels", String.valueOf(parcels.size()));
        metrics.put("With Boundaries", String.valueOf(withBoundaries));
        metrics.put("Without Boundaries", String.valueOf(withoutBoundaries));
        metrics.put("Disputed Parcels", String.valueOf(disputed));
        metrics.put("Total Area Mapped (ha)", String.format("%.4f", totalAreaHectares));

        Map<String, Double> areaByVillage = parcels.stream()
                .filter(p -> p.getVillageName() != null)
                .collect(Collectors.groupingBy(ParcelResponse::getVillageName, TreeMap::new,
                        Collectors.summingDouble(p -> p.getAreaHectares() != null ? p.getAreaHectares() : 0.0)));
        areaByVillage.forEach((v, a) -> metrics.put("Area Mapped - " + v, String.format("%.4f ha", a)));

        List<String> headers = Arrays.asList("Parcel #", "Stand #", "Village", "Area (ha)", "Boundary Points", "Status");
        List<List<String>> rows = parcels.stream().map(p -> Arrays.asList(
                p.getParcelNumber() != null ? p.getParcelNumber() : "",
                p.getStandNumber() != null ? p.getStandNumber() : "",
                p.getVillageName() != null ? p.getVillageName() : "",
                p.getAreaHectares() != null ? p.getAreaHectares().toString() : "",
                p.getBoundaryCount() != null ? p.getBoundaryCount().toString() : "0",
                p.getStatusDisplay() != null ? p.getStatusDisplay() : ""
        )).collect(Collectors.toList());

        ReportSection section = new ReportSection(
                "Land Boundary Summary",
                List.of(new HashMap<>(metrics)),
                headers,
                rows
        );

        TraditionalAuthorityResponse auth = getAuthority(request.getAuthorityId());
        VillageResponse village = getVillage(request.getVillageId());

        return new ReportData(
                "Land Boundary Report",
                auth != null ? auth.getAuthorityName() : null,
                village != null ? village.getVillageName() : null,
                request.getDateFrom(),
                request.getDateTo(),
                List.of(section),
                null,
                parcels,
                null, null, null, null, null
        );
    }

    @Override
    public TraditionalAuthorityResponse getAuthority(Long authorityId) {
        if (authorityId == null) return null;
        return traditionalAuthorityService.findById(authorityId);
    }

    @Override
    public VillageResponse getVillage(Long villageId) {
        if (villageId == null) return null;
        return villageService.findById(villageId);
    }

    private List<PTOResponse> fetchPtos(ReportRequest request) {
        List<PTOResponse> ptos;
        if (request.getVillageId() != null) {
            ptos = ptoService.findByVillage(request.getVillageId());
        } else if (request.getAuthorityId() != null) {
            ptos = ptoService.findAll();
        } else {
            ptos = ptoService.findAll();
        }
        return ptos;
    }

    private List<ParcelResponse> fetchAllParcels(ReportRequest request) {
        List<ParcelResponse> parcels;
        if (request.getVillageId() != null) {
            parcels = parcelService.findByVillage(request.getVillageId());
        } else {
            parcels = parcelService.findAll();
        }
        return parcels;
    }

    private List<BusinessOccupancyResponse> fetchAllBusinesses(ReportRequest request) {
        List<BusinessOccupancyResponse> businesses = businessOccupancyService.findAll();
        if (request.getVillageId() != null) {
            VillageResponse village = getVillage(request.getVillageId());
            if (village != null) {
                businesses = businesses.stream()
                        .filter(b -> village.getVillageName().equals(b.getVillageName()))
                        .collect(Collectors.toList());
            }
        }
        return businesses;
    }
}
