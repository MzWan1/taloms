package za.co.taloms.gis.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.taloms.common.ResourceNotFoundException;
import za.co.taloms.businessoccupancy.domain.entity.BusinessOccupancy;
import za.co.taloms.businessoccupancy.domain.entity.BusinessStatus;
import za.co.taloms.businessoccupancy.domain.repository.BusinessOccupancyRepositoryPort;
import za.co.taloms.gis.application.dto.GeoJsonFeature;
import za.co.taloms.gis.application.dto.GeoJsonResponse;
import za.co.taloms.gis.application.dto.ParcelGeoJsonResponse;
import za.co.taloms.household.domain.entity.Household;
import za.co.taloms.household.domain.repository.HouseholdRepositoryPort;
import za.co.taloms.parcel.application.dto.ParcelResponse;
import za.co.taloms.parcel.application.service.ParcelService;
import za.co.taloms.parcel.domain.entity.ParcelStatus;
import za.co.taloms.traditionalauthority.application.dto.TraditionalAuthorityResponse;
import za.co.taloms.traditionalauthority.application.dto.VillageResponse;
import za.co.taloms.traditionalauthority.application.service.TraditionalAuthorityService;
import za.co.taloms.traditionalauthority.application.service.VillageService;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GisServiceImpl implements GisService {

    private final ParcelService parcelService;
    private final TraditionalAuthorityService authorityService;
    private final VillageService villageService;
    private final BusinessOccupancyRepositoryPort businessOccupancyRepository;
    private final HouseholdRepositoryPort householdRepository;

    @Override
    @Transactional(readOnly = true)
    public ParcelGeoJsonResponse getParcelGeoJson(Long villageId) {
        var parcels = parcelService.findByVillage(villageId);
        var village = villageService.findById(villageId);
        return buildParcelGeoJsonResponse(parcels, village);
    }

    @Override
    @Transactional(readOnly = true)
    public ParcelGeoJsonResponse getParcelGeoJsonByAuthority(Long authorityId) {
        var authority = authorityService.findById(authorityId);
        var villages = villageService.findByAuthority(authorityId);

        List<ParcelResponse> allParcels = new ArrayList<>();
        for (VillageResponse village : villages) {
            allParcels.addAll(parcelService.findByVillage(village.getId()));
        }
        return buildParcelGeoJsonResponse(allParcels, authority);
    }

    @Override
    @Transactional(readOnly = true)
    public ParcelGeoJsonResponse getParcelGeoJsonByStatus(String status) {
        ParcelStatus parcelStatus = ParcelStatus.valueOf(status.toUpperCase());
        var parcels = parcelService.findByStatus(parcelStatus);
        return buildParcelGeoJsonResponse(parcels, null);
    }

    @Override
    @Transactional(readOnly = true)
    public ParcelGeoJsonResponse getParcelGeoJsonById(Long parcelId) {
        var parcel = parcelService.findById(parcelId);
        List<ParcelResponse> parcels = Collections.singletonList(parcel);
        return buildParcelGeoJsonResponse(parcels, null);
    }

    @Override
    @Transactional(readOnly = true)
    public GeoJsonResponse getGeoJsonForVillage(Long villageId) {
        var parcels = parcelService.findByVillage(villageId);
        return buildGeoJsonResponse(parcels);
    }

    @Override
    @Transactional(readOnly = true)
    public GeoJsonResponse getGeoJsonForAuthority(Long authorityId) {
        var villages = villageService.findByAuthority(authorityId);
        List<ParcelResponse> allParcels = new ArrayList<>();
        for (VillageResponse village : villages) {
            allParcels.addAll(parcelService.findByVillage(village.getId()));
        }
        return buildGeoJsonResponse(allParcels);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getParcelGeometry(Long parcelId) {
        var parcel = parcelService.findById(parcelId);
        if (parcel.getBoundaries() == null || parcel.getBoundaries().isEmpty()) {
            throw new ResourceNotFoundException("No boundary data found for parcel: " + parcelId);
        }

        List<List<Double>> coordinates = parcel.getBoundaries().stream()
                .map(b -> Arrays.asList(b.getLongitude(), b.getLatitude()))
                .collect(Collectors.toList());

        if (!coordinates.isEmpty()) {
            coordinates.add(coordinates.get(0));
        }

        Map<String, Object> geometry = new HashMap<>();
        geometry.put("type", "Polygon");
        geometry.put("coordinates", Collections.singletonList(coordinates));

        Map<String, Object> result = new HashMap<>();
        result.put("type", "Feature");
        result.put("geometry", geometry);

        Map<String, Object> properties = new HashMap<>();
        properties.put("id", parcel.getId());
        properties.put("parcelNumber", parcel.getParcelNumber());
        properties.put("standNumber", parcel.getStandNumber());
        properties.put("parcelType", parcel.getParcelTypeDisplay());
        properties.put("status", parcel.getStatusDisplay());
        properties.put("areaM2", parcel.getAreaM2());
        properties.put("villageName", parcel.getVillageName());
        result.put("properties", properties);

        return result;
    }

    @Override
    public boolean validateCoordinates(double lat, double lng) {
        return lat >= -35 && lat <= -22 && lng >= 16 && lng <= 33;
    }

    @Override
    public boolean validatePolygon(List<Map<String, Double>> coordinates) {
        if (coordinates == null || coordinates.size() < 3) {
            return false;
        }

        Map<String, Double> first = coordinates.get(0);
        Map<String, Double> last = coordinates.get(coordinates.size() - 1);

        if (!first.get("latitude").equals(last.get("latitude")) ||
                !first.get("longitude").equals(last.get("longitude"))) {
            return false;
        }

        for (Map<String, Double> coord : coordinates) {
            double lat = coord.get("latitude");
            double lng = coord.get("longitude");
            if (!validateCoordinates(lat, lng)) {
                return false;
            }
        }

        return true;
    }

    private ParcelGeoJsonResponse buildParcelGeoJsonResponse(List<ParcelResponse> parcels, Object entity) {
        List<ParcelGeoJsonResponse.ParcelFeature> features = new ArrayList<>();

        List<Long> parcelIds = parcels.stream()
                .map(ParcelResponse::getId)
                .collect(Collectors.toList());

        Map<Long, String> businessByParcel = Collections.emptyMap();
        Map<Long, String> householdByParcel = Collections.emptyMap();

        if (!parcelIds.isEmpty()) {
            List<BusinessOccupancy> businessOccupancies = businessOccupancyRepository.findByParcelIdIn(parcelIds);
            businessByParcel = businessOccupancies.stream()
                    .filter(b -> b.getParcel() != null && b.getParcel().getId() != null)
                    .collect(Collectors.toMap(b -> b.getParcel().getId(), b -> b.getBusinessName(), (a, b) -> a));

            List<Household> households = householdRepository.findByParcelIdIn(parcelIds);
            householdByParcel = households.stream()
                    .filter(h -> Boolean.TRUE.equals(h.getActive()) && h.getParcel() != null && h.getParcel().getId() != null)
                    .collect(Collectors.toMap(h -> h.getParcel().getId(), h -> h.getHouseholdHeadName(), (a, b) -> a));
        }

        for (ParcelResponse parcel : parcels) {
            if (parcel.getBoundaries() == null || parcel.getBoundaries().isEmpty()) {
                continue;
            }

            List<List<Double>> coordinates = parcel.getBoundaries().stream()
                    .map(b -> Arrays.asList(b.getLongitude(), b.getLatitude()))
                    .collect(Collectors.toList());

            if (!coordinates.isEmpty()) {
                coordinates.add(coordinates.get(0));
            }

            Map<String, Object> geometry = new HashMap<>();
            geometry.put("type", "Polygon");
            geometry.put("coordinates", Collections.singletonList(coordinates));

            String occupantTag = determineOccupantTag(parcel, businessByParcel, householdByParcel);

            ParcelGeoJsonResponse.ParcelProperties properties =
                    ParcelGeoJsonResponse.ParcelProperties.builder()
                            .id(parcel.getId())
                            .parcelNumber(parcel.getParcelNumber())
                            .standNumber(parcel.getStandNumber())
                            .parcelType(parcel.getParcelType().name())
                            .parcelTypeDisplay(parcel.getParcelTypeDisplay())
                            .status(parcel.getStatus().name())
                            .statusDisplay(parcel.getStatusDisplay())
                            .villageName(parcel.getVillageName())
                            .areaM2(parcel.getAreaM2())
                            .ptoNumber(parcel.getPtoNumber())
                            .ptoHolderName(parcel.getPtoHolderName())
                            .occupantTag(occupantTag)
                            .build();

            ParcelGeoJsonResponse.ParcelFeature feature = new ParcelGeoJsonResponse.ParcelFeature();
            feature.setType("Feature");
            feature.setGeometry(geometry);
            feature.setProperties(properties);

            features.add(feature);
        }

        ParcelGeoJsonResponse.ParcelMetadata metadata =
                ParcelGeoJsonResponse.ParcelMetadata.builder()
                        .totalCount((long) parcels.size())
                        .availableCount(parcels.stream().filter(p -> p.getStatus() == ParcelStatus.AVAILABLE).count())
                        .allocatedCount(parcels.stream().filter(p -> p.getStatus() == ParcelStatus.ALLOCATED).count())
                        .totalAreaM2(parcels.stream().mapToDouble(p -> p.getAreaM2() != null ? p.getAreaM2() : 0).sum())
                        .build();

        if (entity instanceof VillageResponse) {
            VillageResponse village = (VillageResponse) entity;
            metadata.setVillageName(village.getVillageName());
            metadata.setAuthorityName(village.getAuthorityName());
        } else if (entity instanceof TraditionalAuthorityResponse) {
            TraditionalAuthorityResponse authority = (TraditionalAuthorityResponse) entity;
            metadata.setAuthorityName(authority.getAuthorityName());
        }

        return ParcelGeoJsonResponse.builder()
                .features(features)
                .metadata(metadata)
                .build();
    }

    private GeoJsonResponse buildGeoJsonResponse(List<ParcelResponse> parcels) {
        List<GeoJsonFeature> features = new ArrayList<>();

        List<Long> parcelIds = parcels.stream()
                .map(ParcelResponse::getId)
                .collect(Collectors.toList());

        Map<Long, String> businessByParcel = Collections.emptyMap();
        Map<Long, String> householdByParcel = Collections.emptyMap();

        if (!parcelIds.isEmpty()) {
            List<BusinessOccupancy> businessOccupancies = businessOccupancyRepository.findByParcelIdIn(parcelIds);
            businessByParcel = businessOccupancies.stream()
                    .filter(b -> b.getParcel() != null && b.getParcel().getId() != null)
                    .collect(Collectors.toMap(b -> b.getParcel().getId(), b -> b.getBusinessName(), (a, b) -> a));

            List<Household> households = householdRepository.findByParcelIdIn(parcelIds);
            householdByParcel = households.stream()
                    .filter(h -> Boolean.TRUE.equals(h.getActive()) && h.getParcel() != null && h.getParcel().getId() != null)
                    .collect(Collectors.toMap(h -> h.getParcel().getId(), h -> h.getHouseholdHeadName(), (a, b) -> a));
        }

        for (ParcelResponse parcel : parcels) {
            if (parcel.getBoundaries() == null || parcel.getBoundaries().isEmpty()) {
                continue;
            }

            List<List<Double>> coordinates = parcel.getBoundaries().stream()
                    .map(b -> Arrays.asList(b.getLongitude(), b.getLatitude()))
                    .collect(Collectors.toList());

            if (!coordinates.isEmpty()) {
                coordinates.add(coordinates.get(0));
            }

            Map<String, Object> geometry = new HashMap<>();
            geometry.put("type", "Polygon");
            geometry.put("coordinates", Collections.singletonList(coordinates));

            String occupantTag = determineOccupantTag(parcel, businessByParcel, householdByParcel);

            Map<String, Object> properties = new HashMap<>();
            properties.put("id", parcel.getId());
            properties.put("parcelNumber", parcel.getParcelNumber());
            properties.put("standNumber", parcel.getStandNumber());
            properties.put("parcelType", parcel.getParcelTypeDisplay());
            properties.put("status", parcel.getStatus().name());
            properties.put("statusDisplay", parcel.getStatus().getDisplayName());
            properties.put("villageName", parcel.getVillageName());
            properties.put("areaM2", parcel.getAreaM2());
            properties.put("occupantTag", occupantTag);

            GeoJsonFeature feature = GeoJsonFeature.builder()
                    .type("Feature")
                    .geometry(geometry)
                    .properties(properties)
                    .build();

            features.add(feature);
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("totalCount", parcels.size());

        return GeoJsonResponse.builder()
                .features(features)
                .metadata(metadata)
                .build();
    }

    private String determineOccupantTag(ParcelResponse parcel,
                                        Map<Long, String> businessByParcel,
                                        Map<Long, String> householdByParcel) {
        if (parcel == null || parcel.getId() == null) {
            return "Not Assigned";
        }

        Long parcelId = parcel.getId();

        if (businessByParcel.containsKey(parcelId)) {
            return businessByParcel.get(parcelId);
        }

        if (householdByParcel.containsKey(parcelId)) {
            return householdByParcel.get(parcelId);
        }

        if (parcel.getPtoHolderName() != null && !parcel.getPtoHolderName().isBlank()) {
            return parcel.getPtoHolderName();
        }

        return "Not Assigned";
    }
}