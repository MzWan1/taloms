package za.co.taloms.traditionalauthority.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Service;
import za.co.taloms.common.BusinessValidationException;
import za.co.taloms.common.spatial.GeoBoundarySupport;
import za.co.taloms.traditionalauthority.application.dto.CoordinateDto;
import za.co.taloms.traditionalauthority.domain.entity.TraditionalAuthority;
import za.co.taloms.traditionalauthority.domain.entity.Village;
import za.co.taloms.traditionalauthority.domain.repository.TraditionalAuthorityRepositoryPort;
import za.co.taloms.traditionalauthority.domain.repository.VillageRepositoryPort;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates authority / village boundary polygons captured on the interactive
 * map: a village must sit fully inside its authority and must not overlap any
 * other village or authority; authorities must not overlap each other.
 */
@Service
@RequiredArgsConstructor
public class SpatialBoundaryService {

    private final ObjectMapper objectMapper;
    private final TraditionalAuthorityRepositoryPort authorityRepository;
    private final VillageRepositoryPort villageRepository;

    // ── Parsing / serialising ────────────────────────────────────────────

    /** Parses a boundaryJson string into [lat, lng] points; null when absent. */
    public List<double[]> parseBoundary(String boundaryJson) {
        if (boundaryJson == null || boundaryJson.isBlank()) {
            return null;
        }
        try {
            List<CoordinateDto> coords = objectMapper.readValue(
                    boundaryJson, new TypeReference<List<CoordinateDto>>() {});
            List<double[]> points = new ArrayList<>();
            for (CoordinateDto c : coords) {
                if (c == null || c.getLatitude() == null || c.getLongitude() == null) {
                    throw new BusinessValidationException(
                            "Boundary contains an invalid coordinate point.");
                }
                points.add(new double[]{c.getLatitude(), c.getLongitude()});
            }
            if (points.size() < 3) {
                throw new BusinessValidationException(
                        "A mapped boundary needs at least 3 points. "
                                + "Please adjust the shape on the map.");
            }
            return points;
        } catch (BusinessValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessValidationException(
                    "The mapped boundary could not be read. "
                            + "Please redraw the shape on the map.");
        }
    }

    /** Serialises [lat, lng] points back to the canonical boundaryJson format. */
    public String toJson(List<double[]> points) {
        if (points == null) {
            return null;
        }
        try {
            List<CoordinateDto> coords = points.stream()
                    .map(p -> CoordinateDto.builder()
                            .latitude(p[0]).longitude(p[1]).build())
                    .toList();
            return objectMapper.writeValueAsString(coords);
        } catch (Exception e) {
            throw new BusinessValidationException(
                    "Failed to store the mapped boundary.");
        }
    }

    // ── Validation rules ─────────────────────────────────────────────────

    /**
     * Validates a village boundary: must sit fully inside its authority and
     * must not overlap any other village or any other authority.
     */
    public void validateVillageBoundary(Village village, TraditionalAuthority authority,
                                        String boundaryJson, Long excludeVillageId) {
        List<double[]> points = parseBoundary(boundaryJson);
        if (points == null) {
            throw new BusinessValidationException(
                    "Please map the village boundary on the map before saving. "
                            + "Every village must be mapped inside its authority.");
        }
        Polygon villagePolygon = GeoBoundarySupport.toPolygon(points);
        if (villagePolygon == null) {
            throw new BusinessValidationException(
                    "The village boundary is invalid. Please redraw the shape on the map.");
        }

        // Rule 1 — village must sit fully inside its own authority
        String authorityBoundaryJson = authority.getBoundaryJson();
        if (authorityBoundaryJson == null || authorityBoundaryJson.isBlank()) {
            throw new BusinessValidationException(
                    "Traditional Authority '" + authority.getAuthorityName()
                            + "' has no mapped boundary yet. Ask an administrator to map "
                            + "the authority boundary before adding villages.");
        }
        Polygon authorityPolygon = GeoBoundarySupport.toPolygon(
                parseBoundary(authorityBoundaryJson));
        if (authorityPolygon == null
                || !GeoBoundarySupport.covers(authorityPolygon, villagePolygon)) {
            throw new BusinessValidationException(
                    "The mapped village boundary falls outside the '"
                            + authority.getAuthorityName()
                            + "' boundary. A village may only be created inside its own "
                            + "Traditional Authority area.");
        }

        // Rule 2 — must not overlap any OTHER village
        for (Village other : villageRepository.findAllActive()) {
            if (other.getId() != null && other.getId().equals(excludeVillageId)) {
                continue;
            }
            if (other.getBoundaryJson() == null || other.getBoundaryJson().isBlank()) {
                continue;
            }
            Polygon otherPolygon = GeoBoundarySupport.toPolygon(
                    parseBoundary(other.getBoundaryJson()));
            if (GeoBoundarySupport.overlaps(villagePolygon, otherPolygon)) {
                throw new BusinessValidationException(
                        "The mapped village boundary overlaps village '"
                                + other.getVillageName() + "'. Villages may not overlap.");
            }
        }

        // Rule 3 — must not overlap any OTHER authority
        for (TraditionalAuthority other : authorityRepository.findAllActive()) {
            if (other.getId().equals(authority.getId())) {
                continue;
            }
            if (other.getBoundaryJson() == null || other.getBoundaryJson().isBlank()) {
                continue;
            }
            Polygon otherPolygon = GeoBoundarySupport.toPolygon(
                    parseBoundary(other.getBoundaryJson()));
            if (GeoBoundarySupport.overlaps(villagePolygon, otherPolygon)) {
                throw new BusinessValidationException(
                        "The mapped village boundary overlaps Traditional Authority '"
                                + other.getAuthorityName()
                                + "'. Villages may not cross authority boundaries.");
            }
        }
    }

    /**
     * Validates an authority boundary: must not overlap any other authority and
     * (when updating) all of its existing villages must remain inside it.
     */
    public void validateAuthorityBoundary(TraditionalAuthority authority,
                                          String boundaryJson, Long excludeAuthorityId) {
        List<double[]> points = parseBoundary(boundaryJson);
        if (points == null) {
            throw new BusinessValidationException(
                    "Please map the authority boundary on the map before saving.");
        }
        Polygon authorityPolygon = GeoBoundarySupport.toPolygon(points);
        if (authorityPolygon == null) {
            throw new BusinessValidationException(
                    "The authority boundary is invalid. Please redraw the shape on the map.");
        }

        for (TraditionalAuthority other : authorityRepository.findAllActive()) {
            if (other.getId() != null && other.getId().equals(excludeAuthorityId)) {
                continue;
            }
            if (other.getBoundaryJson() == null || other.getBoundaryJson().isBlank()) {
                continue;
            }
            Polygon otherPolygon = GeoBoundarySupport.toPolygon(
                    parseBoundary(other.getBoundaryJson()));
            if (GeoBoundarySupport.overlaps(authorityPolygon, otherPolygon)) {
                throw new BusinessValidationException(
                        "The mapped authority boundary overlaps Traditional Authority '"
                                + other.getAuthorityName()
                                + "'. Authorities may not overlap.");
            }
        }

        // Existing villages of this authority must remain inside the new boundary
        for (Village village : villageRepository
                .findByTraditionalAuthorityId(authority.getId())) {
            if (village.getBoundaryJson() == null || village.getBoundaryJson().isBlank()) {
                continue;
            }
            Polygon villagePolygon = GeoBoundarySupport.toPolygon(
                    parseBoundary(village.getBoundaryJson()));
            if (villagePolygon != null
                    && !GeoBoundarySupport.covers(authorityPolygon, villagePolygon)) {
                throw new BusinessValidationException(
                        "The new authority boundary excludes village '"
                                + village.getVillageName()
                                + "'. Adjust the boundary so all its villages stay inside it.");
            }
        }
    }
}
