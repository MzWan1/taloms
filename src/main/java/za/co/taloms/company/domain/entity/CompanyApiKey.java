package za.co.taloms.company.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import za.co.taloms.company.infrastructure.converter.ApiScopeSetConverter;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * An API credential belonging to exactly one {@link Company}.
 *
 * Security: the raw key is NEVER stored. {@link #keyHash} holds the SHA-256 hex
 * digest of the raw key and {@link #keyPrefix} is a short non-secret fragment
 * used only so administrators can tell keys apart in the UI.
 */
@Entity
@Table(name = "company_api_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "label", length = 100)
    private String label;

    /** Non-secret display fragment, e.g. {@code taloms_ab12cd34}. */
    @Column(name = "key_prefix", nullable = false, length = 24)
    private String keyPrefix;

    /** SHA-256 hex digest of the raw key. */
    @Column(name = "key_hash", nullable = false, unique = true, length = 64)
    private String keyHash;

    @Convert(converter = ApiScopeSetConverter.class)
    @Column(name = "scopes", nullable = false, length = 255)
    @Builder.Default
    private Set<ApiScope> scopes = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ApiKeyStatus status = ApiKeyStatus.ACTIVE;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_by", length = 50)
    private String revokedBy;

    @Column(name = "revoke_reason", length = 255)
    private String revokeReason;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = ApiKeyStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return ApiKeyStatus.ACTIVE == status;
    }

    public boolean hasScope(ApiScope scope) {
        return scope != null && scopes != null && scopes.contains(scope);
    }

    /** Admin action: permanently disable this credential. */
    public void revoke(String revokedBy, String reason) {
        this.status = ApiKeyStatus.REVOKED;
        this.revokedBy = revokedBy;
        this.revokeReason = reason;
        this.revokedAt = LocalDateTime.now();
    }

    public void touchLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }
}