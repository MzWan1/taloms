package za.co.taloms.security.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import za.co.taloms.company.domain.entity.Company;
import za.co.taloms.traditionalauthority.domain.entity.TraditionalAuthority;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "account_locked", nullable = false)
    @Builder.Default
    private Boolean accountLocked = false;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "traditional_authority_id")
    private Long traditionalAuthorityId;

    /**
     * Authorities a CHIEF user belongs to (many-to-many). A chief may belong to
     * several authorities; an authority may have several chiefs. This is the
     * authoritative link used for chief authorization — see
     * {@code AuthorityScopeService}. The legacy single
     * {@link #traditionalAuthorityId} column is retained for HEADSMAN scoping.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "chief_authorities",
            joinColumns        = @JoinColumn(name = "chief_id"),
            inverseJoinColumns = @JoinColumn(name = "authority_id")
    )
    @Builder.Default
    private Set<TraditionalAuthority> authorities = new HashSet<>();

    @Column(name = "id_number", length = 13, unique = true)
    private String idNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns        = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

