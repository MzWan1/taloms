package za.co.taloms.configuration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import za.co.taloms.traditionalauthority.application.dto.TraditionalAuthorityRequest;
import za.co.taloms.traditionalauthority.application.dto.TraditionalAuthorityResponse;
import za.co.taloms.traditionalauthority.application.dto.VillageRequest;
import za.co.taloms.traditionalauthority.application.service.TraditionalAuthorityService;
import za.co.taloms.traditionalauthority.application.service.VillageService;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final TraditionalAuthorityService authorityService;
    private final VillageService villageService;

    @PostConstruct
    public void init() {
        log.info("=== DataInitializer bean created ===");
    }

    @Bean
    CommandLineRunner seedDefaultData() {
        return args -> {
            log.info("=== DataInitializer: Checking if default data is needed ===");
            try {
                long count = authorityService.findAll().size();
                log.info("=== DataInitializer: Found {} existing authorities ===", count);

                if (count == 0) {
                    log.warn("!!! No authorities found. Seeding default data... !!!");

                    var authority1 = createAuthority(
                            "eZakeni Traditional Authority",
                            "Chief Mthembu",
                            "Headman Nkosi",
                            "+27700000001",
                            "ezakeni@example.org",
                            "Main Street, eZakeni",
                            "Eastern Cape");

                    var authority2 = createAuthority(
                            "Rahlabane Traditional Authority",
                            "Chief Rahlabane",
                            "Headman Mokoena",
                            "+27700000002",
                            "rahlabane@example.org",
                            "Main Road, Rahlabane",
                            "Free State");

                    var authority3 = createAuthority(
                            "Matatiele Traditional Authority",
                            "Chief Matatiele",
                            "Headman Dali",
                            "+27700000003",
                            "matatiele@example.org",
                            "High Street, Matatiele",
                            "KwaZulu-Natal");

                    if (authority1 != null) createVillages(authority1.getId(), "eZakeni");
                    if (authority2 != null) createVillages(authority2.getId(), "Rahlabane");
                    if (authority3 != null) createVillages(authority3.getId(), "Matatiele");

                    log.info("=== DataInitializer: Default authorities and villages seeded successfully ===");
                } else {
                    log.info("=== DataInitializer: Authorities already exist. Skipping seeding. ===");
                }
            } catch (Exception e) {
                log.error("!!! DataInitializer FAILED: {} !!!", e.getMessage(), e);
            }
        };
    }

    private TraditionalAuthorityResponse createAuthority(
            String name, String chief, String headman,
            String phone, String email, String address, String region) {
        try {
            var request = new TraditionalAuthorityRequest(
                    name, chief, headman, phone, email, address, region);
            var response = authorityService.create(request, "system");
            log.info("Created authority: {} (id={})", name, response.getId());
            return response;
        } catch (Exception e) {
            log.error("Failed to create authority '{}': {}", name, e.getMessage(), e);
            return null;
        }
    }

    private void createVillages(Long authorityId, String prefix) {
        String[][] villages = {
                {prefix + " Village A", prefix + " Region 1", "Headman A"},
                {prefix + " Village B", prefix + " Region 2", "Headman B"},
                {prefix + " Village C", prefix + " Region 3", "Headman C"}
        };

        for (String[] v : villages) {
            try {
                villageService.create(new VillageRequest(
                        v[0], v[1], v[2], "Auto-seeded village", authorityId
                ));
            } catch (Exception e) {
                log.error("Failed to create village '{}': {}", v[0], e.getMessage(), e);
            }
        }
    }
}
