package za.co.taloms.company.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.taloms.common.BusinessValidationException;
import za.co.taloms.common.DuplicateRecordException;
import za.co.taloms.common.ResourceNotFoundException;
import za.co.taloms.company.application.dto.CompanyCreateRequest;
import za.co.taloms.company.application.dto.CompanyResponse;
import za.co.taloms.company.domain.entity.ApiKeyStatus;
import za.co.taloms.company.domain.entity.Company;
import za.co.taloms.company.domain.repository.CompanyApiKeyRepositoryPort;
import za.co.taloms.company.domain.repository.CompanyRepositoryPort;
import za.co.taloms.security.domain.repository.UserRepositoryPort;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepositoryPort companyRepository;
    private final CompanyApiKeyRepositoryPort apiKeyRepository;
    private final UserRepositoryPort userRepositoryPort;

    @Override
    public CompanyResponse createCompany(CompanyCreateRequest request, String actorUsername) {
        String name = request.getName() == null ? null : request.getName().trim();
        if (companyRepository.existsByName(name)) {
            throw new DuplicateRecordException("A company with this name is already registered.");
        }
        String registrationNumber = blankToNull(request.getRegistrationNumber());
        if (registrationNumber != null
                && companyRepository.existsByRegistrationNumber(registrationNumber)) {
            throw new DuplicateRecordException("A company with this registration number is already registered.");
        }

        // Validate owner user
        za.co.taloms.security.domain.entity.User ownerUser = userRepositoryPort.findById(request.getOwnerUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Company owner user not found with ID: " + request.getOwnerUserId()));

        boolean hasCompanyRole = ownerUser.getRoles().stream().anyMatch(r -> "ROLE_COMPANY".equals(r.getName()));
        if (!hasCompanyRole) {
            throw new BusinessValidationException("The specified owner user does not have the COMPANY role.");
        }

        if (ownerUser.getCompany() != null) {
            throw new BusinessValidationException("The specified owner user is already linked to a company.");
        }

        Company company = Company.builder()
                .name(name)
                .registrationNumber(registrationNumber)
                .contactEmail(request.getContactEmail().trim())
                .contactPhone(blankToNull(request.getContactPhone()))
                .createdBy(actorUsername)
                .build();

        Company saved = companyRepository.save(company);

        ownerUser.setCompany(saved);
        userRepositoryPort.save(ownerUser);

        log.info("Company '{}' (id {}) registered by {}. Linked to user '{}'", saved.getName(), saved.getId(),
                actorUsername, ownerUser.getUsername());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyResponse> findAll() {
        return companyRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponse findById(Long id) {
        return toResponse(requireCompany(id));
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponse findByUsername(String username) {
        // Find the user by username and get their linked company
        return userRepositoryPort.findByUsername(username)
                .filter(user -> user.getCompany() != null)
                .map(user -> toResponse(user.getCompany()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Company not found for user: " + username));
    }

    @Override
    public CompanyResponse disableCompany(Long id, String reason, String actorUsername) {
        Company company = requireCompany(id);
        if (!company.isActive()) {
            throw new BusinessValidationException("This company is already disabled.");
        }
        company.disable(blankToNull(reason));
        Company saved = companyRepository.save(company);
        log.warn("Company '{}' (id {}) API access DISABLED by {} (reason: {})",
                saved.getName(), saved.getId(), actorUsername, reason);
        return toResponse(saved);
    }

    @Override
    public CompanyResponse activateCompany(Long id, String actorUsername) {
        Company company = requireCompany(id);
        if (company.isActive()) {
            throw new BusinessValidationException("This company is already active.");
        }
        company.activate();
        Company saved = companyRepository.save(company);
        log.info("Company '{}' (id {}) API access ENABLED by {}", saved.getName(), saved.getId(), actorUsername);
        return toResponse(saved);
    }

    private Company requireCompany(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", id));
    }

    CompanyResponse toResponse(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .registrationNumber(company.getRegistrationNumber())
                .contactEmail(company.getContactEmail())
                .contactPhone(company.getContactPhone())
                .status(company.getStatus())
                .statusDisplay(company.getStatus() == null ? null : company.getStatus().getDisplayName())
                .statusBadgeClass(company.getStatus() == null ? null : company.getStatus().getBadgeClass())
                .disabledReason(company.getDisabledReason())
                .disabledAt(company.getDisabledAt())
                .createdBy(company.getCreatedBy())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .activeKeyCount(apiKeyRepository.countByCompanyIdAndStatus(company.getId(), ApiKeyStatus.ACTIVE))
                .build();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}