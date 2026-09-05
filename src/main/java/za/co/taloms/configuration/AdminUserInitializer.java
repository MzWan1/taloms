package za.co.taloms.configuration;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import za.co.taloms.security.domain.entity.Role;
import za.co.taloms.security.domain.entity.User;
import za.co.taloms.security.domain.repository.UserRepositoryPort;
import za.co.taloms.security.infrastructure.repository.RoleJpaRepository;

import java.util.Set;

@Configuration
public class AdminUserInitializer {

    @Bean
    CommandLineRunner ensureAdminUserExists(UserRepositoryPort userRepo,
                                            RoleJpaRepository roleRepo,
                                            PasswordEncoder passwordEncoder) {
        return args -> {
            // Populate the 3 roles if they don't exist
            Role adminRole = roleRepo.findByName("ROLE_ADMIN")
                    .orElseGet(() -> roleRepo.save(Role.builder()
                            .name("ROLE_ADMIN")
                            .description("System administrator with full access")
                            .build()));

            Role chiefRole = roleRepo.findByName("ROLE_CHIEF")
                    .orElseGet(() -> roleRepo.save(Role.builder()
                            .name("ROLE_CHIEF")
                            .description("Chief managing an assigned authority")
                            .build()));

            Role headsmanRole = roleRepo.findByName("ROLE_HEADSMAN")
                    .orElseGet(() -> roleRepo.save(Role.builder()
                            .name("ROLE_HEADSMAN")
                            .description("Headsman handling day-to-day operations")
                            .build()));

            // Create admin user if it doesn't exist
            if (userRepo.findByUsername("admin").isEmpty()) {
                User admin = User.builder()
                        .username("admin")
                        .email("admin@taloms.co.za")
                        .passwordHash(passwordEncoder.encode("Admin@1234"))
                        .fullName("System Administrator")
                        .enabled(true)
                        .accountLocked(false)
                        .failedLoginAttempts(0)
                        .roles(Set.of(adminRole))
                        .build();
                userRepo.save(admin);
            }
        };
    }
}
