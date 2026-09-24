package za.co.taloms.traditionalauthority.application.service;

import za.co.taloms.traditionalauthority.application.dto.*;
import java.util.List;

public interface TraditionalAuthorityService {
    TraditionalAuthorityResponse create(
            TraditionalAuthorityRequest request, String createdBy);
    TraditionalAuthorityResponse update(
            Long id, TraditionalAuthorityRequest request);
    TraditionalAuthorityResponse findById(Long id);
    List<TraditionalAuthorityResponse> findAll();
    List<TraditionalAuthorityResponse> findAllActive();
    List<TraditionalAuthorityResponse> searchByName(String name);
    void deactivate(Long id);
    void activate(Long id);

    /** Chiefs linked to the given authority (many-to-many). */
    List<TraditionalAuthorityChiefDto> findChiefs(Long authorityId);

    /** ADDS a chief → authority link; existing links for the chief are preserved. */
    void addChiefToAuthority(Long authorityId, Long chiefId);

    /** Removes a chief → authority link. */
    void removeChiefFromAuthority(Long authorityId, Long chiefId);
}

