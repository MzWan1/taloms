package za.co.taloms.common.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.taloms.common.ApiResponse;
import za.co.taloms.traditionalauthority.application.service.TraditionalAuthorityService;
import za.co.taloms.traditionalauthority.application.service.VillageService;
import za.co.taloms.traditionalauthority.application.dto.TraditionalAuthorityResponse;
import za.co.taloms.traditionalauthority.application.dto.VillageResponse;
import za.co.taloms.parcel.application.service.ParcelService;
import za.co.taloms.parcel.application.dto.ParcelResponse;
import za.co.taloms.household.application.service.HouseholdService;
import za.co.taloms.household.application.dto.HouseholdResponse;
import za.co.taloms.businessoccupancy.application.service.BusinessOccupancyService;
import za.co.taloms.businessoccupancy.application.dto.BusinessOccupancyResponse;
import za.co.taloms.pto.application.service.PTOService;
import za.co.taloms.pto.application.dto.PTOResponse;
import za.co.taloms.pto.application.dto.PTOSearchCriteria;
import java.util.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class GlobalSearchController {

    private final TraditionalAuthorityService authorityService;
    private final VillageService villageService;
    private final ParcelService parcelService;
    private final HouseholdService householdService;
    private final BusinessOccupancyService businessService;
    private final PTOService ptoService;

    @GetMapping
    public ResponseEntity<ApiResponse<GlobalSearchResult>> search(@RequestParam(required = false) String q) {
        var result = new GlobalSearchResult();

        if (q == null || q.trim().isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(result, "Search completed"));
        }

        String query = q.trim().toLowerCase();

        // Search authorities
        try {
            result.setAuthorities(authorityService.searchByName(query));
        } catch (Exception e) {
            result.setAuthorities(Collections.emptyList());
        }

        // Search villages
        try {
            result.setVillages(villageService.searchByName(query));
        } catch (Exception e) {
            result.setVillages(Collections.emptyList());
        }

        // Search parcels
        try {
            result.setParcels(parcelService.search(query));
        } catch (Exception e) {
            result.setParcels(Collections.emptyList());
        }

        // Search households
        try {
            result.setHouseholds(householdService.searchByName(query));
        } catch (Exception e) {
            result.setHouseholds(Collections.emptyList());
        }

        // Search businesses
        try {
            result.setBusinesses(businessService.searchByName(query));
        } catch (Exception e) {
            result.setBusinesses(Collections.emptyList());
        }

        // Search PTOs
        try {
            var criteria = PTOSearchCriteria.builder()
                    .holderName(query)
                    .ptoNumber(query)
                    .build();
            result.setPtos(ptoService.search(criteria));
        } catch (Exception e) {
            result.setPtos(Collections.emptyList());
        }

        long total = result.getTotalResults();
        return ResponseEntity.ok(ApiResponse.success(result,
                "Found " + total + " result(s) across all entities"));
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class GlobalSearchResult {
    @lombok.Builder.Default
    private List<TraditionalAuthorityResponse> authorities = Collections.emptyList();
    @lombok.Builder.Default
    private List<VillageResponse> villages = Collections.emptyList();
    @lombok.Builder.Default
    private List<ParcelResponse> parcels = Collections.emptyList();
    @lombok.Builder.Default
    private List<PTOResponse> ptos = Collections.emptyList();
    @lombok.Builder.Default
    private List<HouseholdResponse> households = Collections.emptyList();
    @lombok.Builder.Default
    private List<BusinessOccupancyResponse> businesses = Collections.emptyList();

        public long getTotalResults() {
            return (authorities != null ? authorities.size() : 0)
                    + (villages != null ? villages.size() : 0)
                    + (parcels != null ? parcels.size() : 0)
                    + (ptos != null ? ptos.size() : 0)
                    + (households != null ? households.size() : 0)
                    + (businesses != null ? businesses.size() : 0);
        }
    }
}
