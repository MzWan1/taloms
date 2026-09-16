package za.co.taloms.company.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import za.co.taloms.company.domain.entity.Company;
import za.co.taloms.company.domain.entity.CompanyStatus;
import za.co.taloms.company.domain.repository.CompanyRepositoryPort;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CompanyRepositoryAdapter implements CompanyRepositoryPort {

    private final CompanyJpaRepository jpaRepository;

    @Override
    public Company save(Company company) {
        return jpaRepository.save(company);
    }

    @Override
    public Optional<Company> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Company> findByName(String name) {
        return jpaRepository.findByName(name);
    }

    @Override
    public boolean existsByName(String name) {
        return jpaRepository.existsByName(name);
    }

    @Override
    public boolean existsByRegistrationNumber(String registrationNumber) {
        return jpaRepository.existsByRegistrationNumber(registrationNumber);
    }

    @Override
    public List<Company> findAll() {
        return jpaRepository.findAllOrderByCreatedAtDesc();
    }

    @Override
    public List<Company> findByStatus(CompanyStatus status) {
        return jpaRepository.findByStatus(status);
    }

    @Override
    public long countAll() {
        return jpaRepository.count();
    }
}