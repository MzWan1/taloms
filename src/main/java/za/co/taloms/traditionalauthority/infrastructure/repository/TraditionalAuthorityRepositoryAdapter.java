package za.co.taloms.traditionalauthority.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import za.co.taloms.traditionalauthority.domain.entity.TraditionalAuthority;
import za.co.taloms.traditionalauthority.domain.repository.TraditionalAuthorityRepositoryPort;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TraditionalAuthorityRepositoryAdapter
        implements TraditionalAuthorityRepositoryPort {

    private final TraditionalAuthorityJpaRepository jpaRepository;

    @Override
    public TraditionalAuthority save(TraditionalAuthority authority) {
        return jpaRepository.save(authority);
    }

    @Override
    public Optional<TraditionalAuthority> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<TraditionalAuthority> findAll() {
        return jpaRepository.findAllOrderByName();
    }

    @Override
    public List<TraditionalAuthority> findAllActive() {
        return jpaRepository.findAllActive();
    }

    @Override
    public boolean existsByAuthorityName(String name) {
        return jpaRepository.existsByAuthorityName(name);
    }

    @Override
    public boolean existsByAuthorityNameAndIdNot(String name, Long id) {
        return jpaRepository.existsByAuthorityNameAndIdNot(name, id);
    }

    @Override
    public void delete(TraditionalAuthority authority) {
        jpaRepository.delete(authority);
    }
}