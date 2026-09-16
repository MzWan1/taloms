package za.co.taloms.company.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import za.co.taloms.company.domain.entity.Company;
import za.co.taloms.company.domain.entity.CompanyStatus;

import java.util.List;
import java.util.Optional;

public interface CompanyJpaRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByName(String name);

    boolean existsByName(String name);

    boolean existsByRegistrationNumber(String registrationNumber);

    List<Company> findByStatus(CompanyStatus status);

    @Query("SELECT c FROM Company c ORDER BY c.createdAt DESC")
    List<Company> findAllOrderByCreatedAtDesc();
}