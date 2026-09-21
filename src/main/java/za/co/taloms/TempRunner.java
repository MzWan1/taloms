package za.co.taloms;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import za.co.taloms.security.infrastructure.repository.UserJpaRepository;

@Component
public class TempRunner implements CommandLineRunner {
    private final UserJpaRepository repo;
    public TempRunner(UserJpaRepository repo) { this.repo = repo; }
    
    @Override
    public void run(String... args) {
        System.out.println("=== TEMP RUNNER START ===");
        repo.findAll().forEach(u -> {
            System.out.print("User: " + u.getUsername() + ", Roles: ");
            u.getRoles().forEach(r -> System.out.print(r.getName() + " "));
            System.out.println(", Company: " + (u.getCompany() != null ? u.getCompany().getId() : "null"));
        });
        System.out.println("=== TEMP RUNNER END ===");
    }
}
