package za.co.taloms.company.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.taloms.common.BusinessValidationException;
import za.co.taloms.common.DuplicateRecordException;
import za.co.taloms.common.ResourceNotFoundException;
import za.co.taloms.company.application.dto.CompanyCreateRequest;
import za.co.taloms.company.application.dto.CompanyResponse;
import za.co.taloms.company.domain.entity.Company;
import za.co.taloms.company.domain.entity.CompanyStatus;
import za.co.taloms.company.domain.repository.CompanyApiKeyRepositoryPort;
import za.co.taloms.company.domain.repository.CompanyRepositoryPort;
import za.co.taloms.security.domain.repository.UserRepositoryPort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Company lifecycle: ADMIN-only registration, activation and deactivation.
 */
@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

    @Mock private CompanyRepositoryPort companyRepository;
    @Mock private CompanyApiKeyRepositoryPort apiKeyRepository;
    @Mock private UserRepositoryPort userRepository;

    private CompanyServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CompanyServiceImpl(companyRepository, apiKeyRepository, userRepository);
    }

    private Company existing(Long id, CompanyStatus status) {
        Company c = Company.builder()
                .id(id)
                .name("Acme Verification Services")
                .contactEmail("ops@acme.test")
                .status(status)
                .createdBy("admin")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        if (status == CompanyStatus.DISABLED) {
            c.disable("fraud investigation");
        }
        return c;
    }

    private CompanyCreateRequest request(String name, String regNo) {
        return CompanyCreateRequest.builder()
                .name(name)
                .registrationNumber(regNo)
                .contactEmail("ops@acme.test")
                .contactPhone("0115551234")
                .build();
    }

    @Test
    void shouldRegisterCompanyAsActive() {
        when(companyRepository.existsByName("Acme Verification Services")).thenReturn(false);
        when(companyRepository.existsByRegistrationNumber(anyString())).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> {
            Company saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        CompanyResponse response = service.createCompany(
                request("  Acme Verification Services  ", "2019/123456/07"), "admin");

        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(captor.capture());
        Company saved = captor.getValue();

        assertEquals("Acme Verification Services", saved.getName(), "name must be trimmed");
        assertEquals(CompanyStatus.ACTIVE, saved.getStatus(), "a new company starts ACTIVE");
        assertEquals("admin", saved.getCreatedBy());
        assertEquals(1L, response.getId());
        assertEquals(CompanyStatus.ACTIVE, response.getStatus());
    }

    @Test
    void shouldRejectDuplicateCompanyName() {
        when(companyRepository.existsByName("Acme Verification Services")).thenReturn(true);

        assertThrows(DuplicateRecordException.class, () ->
                service.createCompany(request("Acme Verification Services", null), "admin"));
        verify(companyRepository, never()).save(any(Company.class));
    }

    @Test
    void shouldRejectDuplicateRegistrationNumber() {
        when(companyRepository.existsByName(anyString())).thenReturn(false);
        when(companyRepository.existsByRegistrationNumber("2019/123456/07")).thenReturn(true);

        assertThrows(DuplicateRecordException.class, () ->
                service.createCompany(request("Another Co", "2019/123456/07"), "admin"));
        verify(companyRepository, never()).save(any(Company.class));
    }
}