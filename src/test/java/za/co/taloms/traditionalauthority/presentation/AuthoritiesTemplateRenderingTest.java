package za.co.taloms.traditionalauthority.presentation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import za.co.taloms.traditionalauthority.application.dto.TraditionalAuthorityResponse;
import za.co.taloms.traditionalauthority.application.service.TraditionalAuthorityService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class AuthoritiesTemplateRenderingTest {

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Autowired
    private TraditionalAuthorityService authorityService;

    @Test
    void testThymeleafDataAttributeRendering() {
        List<TraditionalAuthorityResponse> authorities = authorityService.findAll();

        Context context = new Context();
        context.setVariable("authorities", authorities);

        String result = templateEngine.process("test-authorities-list", context);

        long authorityRowCount = countOccurrences(result, "authority-row");
        long dataStatusCount = countOccurrences(result, "data-status=");

        assertEquals(authorities.size(), authorityRowCount,
                "Each authority row should have authority-row class");
        assertEquals(authorities.size(), dataStatusCount,
                "Each authority row should have a data-status attribute");
        assertTrue(authorities.size() > 0,
                "Test DB should have authorities");
    }

    @Test
    void testNullAuthorityNameRendersDataAttribute() {
        TraditionalAuthorityResponse resp = TraditionalAuthorityResponse.builder()
                .id(1L)
                .authorityName(null)
                .chiefName(null)
                .headmanName(null)
                .region(null)
                .active(true)
                .villageCount(0)
                .build();

        Context context = new Context();
        context.setVariable("authorities", List.of(resp));

        String result = templateEngine.process("test-authorities-list", context);

        long authorityRowCount = countOccurrences(result, "authority-row");
        long dataStatusCount = countOccurrences(result, "data-status=");

        log.info("Result HTML: {}", result);

        assertEquals(1, authorityRowCount,
                "authority-row class should be present even when authorityName is null");
        assertEquals(1, dataStatusCount,
                "data-status attribute should be present when active is true");
    }

    @Test
    void testEmptyAuthoritiesRendersNoDataRow() {
        Context context = new Context();
        context.setVariable("authorities", List.of());

        String result = templateEngine.process("test-authorities-list", context);

        long authorityRowCount = countOccurrences(result, "authority-row");
        assertTrue(result.contains("No authorities found"),
                "Should render the no-data message when authorities is empty");
        assertEquals(0, authorityRowCount,
                "No authority-row classes should exist when authorities is empty");
    }

    private long countOccurrences(String str, String findStr) {
        long count = 0;
        int lastIndex = 0;
        while (lastIndex != -1) {
            lastIndex = str.indexOf(findStr, lastIndex);
            if (lastIndex != -1) {
                count++;
                lastIndex += findStr.length();
            }
        }
        return count;
    }
}
