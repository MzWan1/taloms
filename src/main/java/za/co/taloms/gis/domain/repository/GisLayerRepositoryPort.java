package za.co.taloms.gis.domain.repository;

import za.co.taloms.gis.domain.entity.GisLayer;
import za.co.taloms.gis.domain.entity.LayerType;
import java.util.List;
import java.util.Optional;

public interface GisLayerRepositoryPort {
    GisLayer save(GisLayer layer);
    Optional<GisLayer> findById(Long id);
    List<GisLayer> findAll();
    List<GisLayer> findByVisibleTrue();
    List<GisLayer> findByLayerType(LayerType layerType);
    List<GisLayer> findByVisibleTrueAndLayerType(LayerType layerType);
    long countAll();
}