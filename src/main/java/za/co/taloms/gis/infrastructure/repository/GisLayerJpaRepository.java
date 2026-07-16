package za.co.taloms.gis.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.taloms.gis.domain.entity.GisLayer;
import za.co.taloms.gis.domain.entity.LayerType;
import java.util.List;

public interface GisLayerJpaRepository extends JpaRepository<GisLayer, Long> {

    List<GisLayer> findByVisibleTrue();

    List<GisLayer> findByLayerType(LayerType layerType);

    List<GisLayer> findByVisibleTrueAndLayerType(LayerType layerType);
}