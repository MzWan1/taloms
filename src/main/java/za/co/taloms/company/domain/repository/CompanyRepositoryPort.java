package za.co.taloms.company.domain.repository;

import za.co.taloms.company.domain.entity.Company;
import za.co.taloms.company.domain.entity.CompanyStatus;

import java.util.List;
import java.util.Optional;

public interface CompanyRepositoryPort {

    Company save(Company company);

    Optional<Company> findById(Long id);

    Optional<Company> findByName(String name);

    boolean existsByName(String name);

    boolean existsByRegistrationNumber(String registrationNumber);

    List<Company> findAll();

    List<Company> findByStatus(CompanyStatus status);

    long countAll();
}