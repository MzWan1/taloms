package za.co.taloms.gis.application.service;

import za.co.taloms.gis.application.dto.GeoJsonResponse;
import za.co.taloms.gis.application.dto.ParcelGeoJsonResponse;
import java.util.List;
import java.util.Map;

public interface GisService {
    ParcelGeoJsonResponse getParcelGeoJson(Long villageId);
    ParcelGeoJsonResponse getParcelGeoJsonByAuthority(Long authorityId);
    ParcelGeoJsonResponse getParcelGeoJsonByStatus(String status);
    ParcelGeoJsonResponse getParcelGeoJsonById(Long parcelId);
    GeoJsonResponse getGeoJsonForVillage(Long villageId);
    GeoJsonResponse getGeoJsonForAuthority(Long authorityId);
    Map<String, Object> getParcelGeometry(Long parcelId);
    boolean validateCoordinates(double lat, double lng);
    boolean validatePolygon(List<Map<String, Double>> coordinates);
}