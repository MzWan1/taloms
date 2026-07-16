package za.co.taloms.gis.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import za.co.taloms.common.ApiResponse;
import za.co.taloms.gis.application.dto.GeoJsonResponse;
import za.co.taloms.gis.application.dto.ParcelGeoJsonResponse;
import za.co.taloms.gis.application.service.GisService;
import za.co.taloms.parcel.domain.entity.ParcelStatus;

@Slf4j
@RestController
@RequestMapping("/api/gis")
@RequiredArgsConstructor
public class GisRestController {

    private final GisService gisService;

    @GetMapping("/parcels/village/{villageId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_LAND_OFFICER','ROLE_REPORT_VIEWER')")
    public ResponseEntity<ApiResponse<ParcelGeoJsonResponse>> getParcelsByVillage(@PathVariable Long villageId) {
        var response = gisService.getParcelGeoJson(villageId);
        return ResponseEntity.ok(ApiResponse.success(response, "Parcel GeoJSON retrieved successfully"));
    }

    @GetMapping("/parcels/authority/{authorityId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_LAND_OFFICER','ROLE_REPORT_VIEWER')")
    public ResponseEntity<ApiResponse<ParcelGeoJsonResponse>> getParcelsByAuthority(@PathVariable Long authorityId) {
        var response = gisService.getParcelGeoJsonByAuthority(authorityId);
        return ResponseEntity.ok(ApiResponse.success(response, "Parcel GeoJSON retrieved successfully"));
    }

    @GetMapping("/parcels/status/{status}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_LAND_OFFICER','ROLE_REPORT_VIEWER')")
    public ResponseEntity<ApiResponse<ParcelGeoJsonResponse>> getParcelsByStatus(@PathVariable String status) {
        var response = gisService.getParcelGeoJsonByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(response, "Parcel GeoJSON retrieved successfully"));
    }

    @GetMapping("/parcels/{parcelId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_LAND_OFFICER','ROLE_REPORT_VIEWER')")
    public ResponseEntity<ApiResponse<ParcelGeoJsonResponse>> getParcelById(@PathVariable Long parcelId) {
        var response = gisService.getParcelGeoJsonById(parcelId);
        return ResponseEntity.ok(ApiResponse.success(response, "Parcel GeoJSON retrieved successfully"));
    }

    @GetMapping("/geojson/village/{villageId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_LAND_OFFICER','ROLE_REPORT_VIEWER')")
    public ResponseEntity<ApiResponse<GeoJsonResponse>> getGeoJsonByVillage(@PathVariable Long villageId) {
        var response = gisService.getGeoJsonForVillage(villageId);
        return ResponseEntity.ok(ApiResponse.success(response, "GeoJSON retrieved successfully"));
    }

    @GetMapping("/geojson/authority/{authorityId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_LAND_OFFICER','ROLE_REPORT_VIEWER')")
    public ResponseEntity<ApiResponse<GeoJsonResponse>> getGeoJsonByAuthority(@PathVariable Long authorityId) {
        var response = gisService.getGeoJsonForAuthority(authorityId);
        return ResponseEntity.ok(ApiResponse.success(response, "GeoJSON retrieved successfully"));
    }

    @GetMapping("/parcel/{parcelId}/geometry")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_LAND_OFFICER','ROLE_REPORT_VIEWER')")
    public ResponseEntity<ApiResponse<Object>> getParcelGeometry(@PathVariable Long parcelId) {
        var response = gisService.getParcelGeometry(parcelId);
        return ResponseEntity.ok(ApiResponse.success(response, "Parcel geometry retrieved successfully"));
    }

    @GetMapping("/validate/coordinates")
    public ResponseEntity<ApiResponse<Boolean>> validateCoordinates(
            @RequestParam double latitude,
            @RequestParam double longitude) {
        boolean valid = gisService.validateCoordinates(latitude, longitude);
        return ResponseEntity.ok(ApiResponse.success(valid, "Coordinate validation completed"));
    }
}