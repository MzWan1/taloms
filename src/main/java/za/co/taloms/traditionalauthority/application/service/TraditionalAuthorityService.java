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
    void deactivate(Long id);
    void activate(Long id);
}