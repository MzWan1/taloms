package za.co.taloms.company.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.taloms.company.application.dto.CompanyApiAuthResult;
import za.co.taloms.company.domain.entity.*;
import za.co.taloms.company.domain.repository.CompanyApiKeyRepositoryPort;
import za.co.taloms.company.domain.repository.CompanyRepositoryPort;
import za.co.taloms.company.infrastructure.security.ApiKeyHasher;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Security-critical authentication rules for the company API.
 */
@ExtendWith(MockitoExtension.class)
class CompanyApiAuthenticationServiceTest {

    @Mock private CompanyApiKeyRepositoryPort apiKeyRepository;
    @Mock private CompanyRepositoryPort companyRepository;

    private ApiKeyHasher hasher;
    private CompanyApiAuthenticationService service;

    private static final String RAW_KEY = "taloms_testRawApiKeyValue1234567890abcdefghijklmn";

    @BeforeEach
    void setUp() {
        hasher = new ApiKeyHasher();
        service = new CompanyApiAuthenticationService(apiKeyRepository, companyRepository, hasher);
    }

    private Company company(Long id, CompanyStatus status) {
        return Company.builder()
                .id(id)
                .name("Company " + id)
                .contactEmail("c" + id + "@test.com")
                .status(status)
                .createdBy("admin")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private CompanyApiKey key(Long id, Company owner, ApiKeyStatus status) {
        return CompanyApiKey.builder()
                .id(id)
                .company(owner)
                .keyPrefix(hasher.displayPrefix(RAW_KEY))
                .keyHash(hasher.hashApiKey(RAW_KEY))
                .scopes(Set.of(ApiScope.POR_READ))
                .status(status)
                .createdBy("admin")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ── Authentication ──────────────────────────────────────────────────────

    @Test
    void shouldRejectMissingApiKey() {
        assertRejected(service.authenticate(null), ApiFailureReason.MISSING_API_KEY);
        assertRejected(service.authenticate(""), ApiFailureReason.MISSING_API_KEY);
        assertRejected(service.authenticate("   "), ApiFailureReason.MISSING_API_KEY);
        verifyNoInteractions(apiKeyRepository);
    }

    @Test
    void shouldRejectUnknownApiKey() {
        when(apiKeyRepository.findByKeyHash(anyString())).thenReturn(Optional.empty());

        assertRejected(service.authenticate("taloms_notARealKey"), ApiFailureReason.INVALID_API_KEY);
        verifyNoInteractions(companyRepository);
    }

    @Test
    void shouldRejectRevokedApiKey() {
        Company c = company(1L, CompanyStatus.ACTIVE);
        when(apiKeyRepository.findByKeyHash(anyString()))
                .thenReturn(Optional.of(key(10L, c, ApiKeyStatus.REVOKED)));

        CompanyApiAuthResult result = service.authenticate(RAW_KEY);

        assertRejected(result, ApiFailureReason.REVOKED_API_KEY);
        // Diagnostics still attribute the attempt to the known key/company.
        assertEquals(1L, result.getCompanyId());
        assertEquals(10L, result.getApiKeyId());
    }

    @Test
    void shouldRejectApiKeyOfDisabledCompany() {
        Company c = company(1L, CompanyStatus.DISABLED);
        when(apiKeyRepository.findByKeyHash(anyString()))
                .thenReturn(Optional.of(key(10L, c, ApiKeyStatus.ACTIVE)));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(c));

        assertRejected(service.authenticate(RAW_KEY), ApiFailureReason.DISABLED_COMPANY);
    }

    @Test
    void shouldRejectApiKeyWhenCompanyIsMissing() {
        Company c = company(1L, CompanyStatus.ACTIVE);
        when(apiKeyRepository.findByKeyHash(anyString()))
                .thenReturn(Optional.of(key(10L, c, ApiKeyStatus.ACTIVE)));
        when(companyRepository.findById(1L)).thenReturn(Optional.empty());

        assertRejected(service.authenticate(RAW_KEY), ApiFailureReason.DISABLED_COMPANY);
    }

    @Test
    void shouldAuthenticateActiveKeyOfActiveCompany() {
        Company c = company(7L, CompanyStatus.ACTIVE);
        CompanyApiKey k = key(10L, c, ApiKeyStatus.ACTIVE);
        when(apiKeyRepository.findByKeyHash(anyString())).thenReturn(Optional.of(k));
        when(companyRepository.findById(7L)).thenReturn(Optional.of(c));

        CompanyApiAuthResult result = service.authenticate(RAW_KEY);

        assertTrue(result.isAuthenticated());
        assertNotNull(result.getPrincipal());
        assertEquals(7L, result.getPrincipal().companyId());
        assertEquals(10L, result.getPrincipal().apiKeyId());
        assertEquals("Company 7", result.getPrincipal().companyName());
        assertTrue(result.getPrincipal().hasScope(ApiScope.POR_READ));
    }

    @Test
    void shouldLookUpKeyByItsHashNeverTheRawKey() {
        Company c = company(1L, CompanyStatus.ACTIVE);
        when(apiKeyRepository.findByKeyHash(anyString()))
                .thenReturn(Optional.of(key(10L, c, ApiKeyStatus.ACTIVE)));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(c));

        service.authenticate(RAW_KEY);

        // Only the digest is ever handed to the repository.
        verify(apiKeyRepository).findByKeyHash(hasher.hashApiKey(RAW_KEY));
        verify(apiKeyRepository, never()).findByKeyHash(RAW_KEY);
    }

    @Test
    void principalShouldIdentifyOnlyTheKeysOwningCompany() {
        // The identity is derived exclusively from the key's owning company, so a
        // company can never act on behalf of another company.
        Company owner = company(2L, CompanyStatus.ACTIVE);
        when(apiKeyRepository.findByKeyHash(anyString()))
                .thenReturn(Optional.of(key(20L, owner, ApiKeyStatus.ACTIVE)));
        when(companyRepository.findById(2L)).thenReturn(Optional.of(owner));

        var principal = service.authenticate(RAW_KEY).getPrincipal();

        assertEquals(2L, principal.companyId());
        assertEquals(20L, principal.apiKeyId());
    }

    @Test
    void shouldRecordLastUsedButNotOnEveryRequest() {
        Company c = company(1L, CompanyStatus.ACTIVE);
        CompanyApiKey k = key(10L, c, ApiKeyStatus.ACTIVE);
        k.setLastUsedAt(LocalDateTime.now());
        when(apiKeyRepository.findByKeyHash(anyString())).thenReturn(Optional.of(k));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(c));

        service.authenticate(RAW_KEY);

        // Recently touched keys are not written again (write amplification guard).
        verify(apiKeyRepository, never()).save(any(CompanyApiKey.class));
    }

    private void assertRejected(CompanyApiAuthResult result, ApiFailureReason expected) {
        assertFalse(result.isAuthenticated(), "request must not be authenticated");
        assertNull(result.getPrincipal());
        assertEquals(expected, result.getFailureReason());
    }
}