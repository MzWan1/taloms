package za.co.taloms.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import za.co.taloms.traditionalauthority.application.dto.TraditionalAuthorityRequest;
import za.co.taloms.traditionalauthority.application.dto.VillageRequest;
import za.co.taloms.traditionalauthority.application.service.TraditionalAuthorityService;
import za.co.taloms.traditionalauthority.application.service.VillageService;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final TraditionalAuthorityService authorityService;
    private final VillageService villageService;

    @Bean
    CommandLineRunner seedDefaultData() {
        return args -> {
            if (authorityService.findAll().isEmpty()) {
                log.warn("!!! No authorities found in database. Seeding default data... !!!");

                var authority1 = authorityService.create(
                        new TraditionalAuthorityRequest(
                                "eZakeni Traditional Authority",
                                "Chief Mthembu",
                                "Headman Nkosi",
                                "+27700000001",
                                "ezakeni@example.org",
                                "Main Street, eZakeni",
                                "Eastern Cape"
                        ),
                        "system"
                );

                var authority2 = authorityService.create(
                        new TraditionalAuthorityRequest(
                                "Rahlabane Traditional Authority",
                                "Chief Rahlabane",
                                "Headman Mokoena",
                                "+27700000002",
                                "rahlabane@example.org",
                                "Main Road, Rahlabane",
                                "Free State"
                        ),
                        "system"
                );

                var authority3 = authorityService.create(
                        new TraditionalAuthorityRequest(
                                "Matatiele Traditional Authority",
                                "Chief Matatiele",
                                "Headman Dali",
                                "+27700000003",
                                "matatiele@example.org",
                                "High Street, Matatiele",
                                "KwaZulu-Natal"
                        ),
                        "system"
                );

                createVillages(authority1.getId(), "eZakeni");
                createVillages(authority2.getId(), "Rahlabane");
                createVillages(authority3.getId(), "Matatiele");

                log.info("Default authorities and villages seeded successfully.");
            } else {
                log.info("Authorities already exist ({}). Skipping default data seeding.", authorityService.findAll().size());
            }
        };
    }

    private void createVillages(Long authorityId, String prefix) {
        String[][] villages = {
                {prefix + " Village A", prefix + " Region 1", "Headman A"},
                {prefix + " Village B", prefix + " Region 2", "Headman B"},
                {prefix + " Village C", prefix + " Region 3", "Headman C"}
        };

        for (String[] v : villages) {
            villageService.create(new VillageRequest(
                    v[0], v[1], v[2], "Auto-seeded village", authorityId
            ));
        }
    }
}
