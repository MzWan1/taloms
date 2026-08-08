package za.co.taloms;

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
public class TestDataSeeder {

    @Bean
    CommandLineRunner seedAdmin(UserRepositoryPort userRepo,
                                RoleJpaRepository roleRepo,
                                PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepo.findByUsername("admin").isPresent()) return;

            Role adminRole = Role.builder()
                    .name("ROLE_SYSTEM_ADMIN")
                    .description("Full system access")
                    .build();
            roleRepo.save(adminRole);

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
        };
    }
}
