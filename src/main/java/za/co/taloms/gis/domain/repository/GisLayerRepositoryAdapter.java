package za.co.taloms.gis.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import za.co.taloms.gis.domain.entity.GisLayer;
import za.co.taloms.gis.domain.entity.LayerType;
import za.co.taloms.gis.domain.repository.GisLayerRepositoryPort;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GisLayerRepositoryAdapter implements GisLayerRepositoryPort {

    private final GisLayerJpaRepository jpaRepository;

    @Override
    public GisLayer save(GisLayer layer) {
        return jpaRepository.save(layer);
    }

    @Override
    public Optional<GisLayer> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<GisLayer> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<GisLayer> findByVisibleTrue() {
        return jpaRepository.findByVisibleTrue();
    }

    @Override
    public List<GisLayer> findByLayerType(LayerType layerType) {
        return jpaRepository.findByLayerType(layerType);
    }

    @Override
    public List<GisLayer> findByVisibleTrueAndLayerType(LayerType layerType) {
        return jpaRepository.findByVisibleTrueAndLayerType(layerType);
    }

    @Override
    public long countAll() {
        return jpaRepository.count();
    }
}