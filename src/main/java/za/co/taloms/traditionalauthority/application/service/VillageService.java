package za.co.taloms.traditionalauthority.application.service;

import za.co.taloms.traditionalauthority.application.dto.*;
import java.util.List;

public interface VillageService {
    VillageResponse create(VillageRequest request);
    VillageResponse update(Long id, VillageRequest request);
    VillageResponse findById(Long id);
    List<VillageResponse> findAll();
    List<VillageResponse> findByAuthority(Long authorityId);
    void deactivate(Long id);
    void activate(Long id);
}