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

        System.out.println("=== testThymeleafDataAttributeRendering ===");
        System.out.println("Authorities count: " + authorities.size());

        long dataStatusCount = countOccurrences(result, "data-status=");
        System.out.println("data-status attributes: " + dataStatusCount);

        int tbodyStart = result.indexOf("<tbody>");
        int tbodyEnd = result.indexOf("</tbody>");
        if (tbodyStart >= 0 && tbodyEnd >= 0) {
            String tbody = result.substring(tbodyStart, tbodyEnd + 8);
            System.out.println("TBODY:\n" + tbody);
        }

        assertEquals(authorities.size(), dataStatusCount,
                "Each authority row should have a data-status attribute");
        assertTrue(authorities.size() > 0,
                "Test DB should have authorities");
        System.out.println("=== End ===");
    }

    @Test
    void testNullAuthorityNameStillRendersRow() {
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

        System.out.println("=== testNullAuthorityNameStillRendersRow ===");
        System.out.println("Result:\n" + result);

        assertTrue(result.contains("data-status=\"active\""),
                "Row should still render with data-status even when authorityName is null");
        assertTrue(result.contains("<tr"),
                "Row should be rendered in the table tbody");
        System.out.println("=== End ===");
    }

    @Test
    void testEmptyAuthoritiesRendersNoDataRow() {
        Context context = new Context();
        context.setVariable("authorities", List.of());

        String result = templateEngine.process("test-authorities-list", context);

        assertTrue(result.contains("No authorities found"),
                "Should render the no-data message when authorities is empty");
        assertTrue(result.contains("no-data"),
                "Should render the no-data row when authorities is empty");
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
