package za.co.taloms.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import za.co.taloms.pto.application.dto.PTOResponse;
import za.co.taloms.pto.application.dto.PTOSyncDto;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the single reusable ID-number masking utility
 * ({@link IdMasker#maskIdNumber(String)}) and the presentation layers that use
 * it (Jackson responses and Thymeleaf templates).
 *
 * The full ID number must never be altered: masking is display-only.
 */
class IdMaskerTest {

    private static final String FULL_ID = "9001011234087";
    private static final String MASKED_ID = "900101****087";

    // ── maskIdNumber: the documented contract ───────────────────────────────

    @Test
    void masksStandardThirteenDigitSaId() {
        assertEquals(MASKED_ID, IdMasker.maskIdNumber(FULL_ID));
    }

    @Test
    void keepsFirstSixAndLastThreeDigits() {
        String masked = IdMasker.maskIdNumber(FULL_ID);
        assertEquals("900101", masked.substring(0, 6));
        assertEquals("087", masked.substring(masked.length() - 3));
    }

    @Test
    void doesNotModifyTheOriginalValue() {
        String original = new String(FULL_ID);
        String masked = IdMasker.maskIdNumber(original);

        assertEquals(MASKED_ID, masked);
        assertEquals(FULL_ID, original, "the original ID number must remain unchanged");
    }

    @Test
    void trimsSurroundingWhitespaceBeforeMasking() {
        assertEquals(MASKED_ID, IdMasker.maskIdNumber("  " + FULL_ID + "  "));
    }

    @Test
    void handlesNullSafely() {
        assertNull(IdMasker.maskIdNumber(null));
    }

    @Test
    void handlesEmptyAndBlankStringsSafely() {
        assertNull(IdMasker.maskIdNumber(""));
        assertNull(IdMasker.maskIdNumber("   "));
    }

    @Test
    void fullyMasksValuesTooShortToSplitSafely() {
        assertEquals("*****", IdMasker.maskIdNumber("12345"));
        assertEquals("*********", IdMasker.maskIdNumber("123456789"));
    }

    @Test
    void handlesUnexpectedLengthsWithoutThrowing() {
        // 10 digits: first 6 + 1 masked + last 3
        assertEquals("123456*890", IdMasker.maskIdNumber("1234567890"));
        // 12 digits: first 6 + 3 masked + last 3
        assertEquals("123456***012", IdMasker.maskIdNumber("123456789012"));
        // 15 digits (longer than a standard SA ID): first 6 + 6 masked + last 3
        assertEquals("123456******456", IdMasker.maskIdNumber("123456789123456"));
    }

    @Test
    void handlesNonNumericValuesWithoutThrowing() {
        // Values shorter than the safe split threshold are fully hidden.
        assertEquals("***", IdMasker.maskIdNumber("N/A"));
        // A longer non-numeric value is still masked by position, never thrown.
        assertEquals("abcdef****klm", IdMasker.maskIdNumber("abcdefghijklm"));
    }

    // ── Legacy numeric masking used for internal DB ids remains intact ──────

    @Test
    void legacyNumericMaskStillHandlesNull() {
        assertNull(IdMasker.mask((Long) null));
        assertNull(IdMasker.mask((String) null));
    }

    // ── JSON responses: idNumber is masked, the getter stays full ───────────

    @Test
    void ptoJsonMasksIdNumberButKeepsGetterFull() throws Exception {
        PTOResponse pto = PTOResponse.builder()
                .idNumber(FULL_ID)
                .ptoNumber("PTO-2026-00001")
                .build();

        String json = new ObjectMapper().writeValueAsString(pto);

        assertTrue(json.contains("\"idNumber\":\"" + MASKED_ID + "\""),
                "the JSON response must contain the masked ID number");
        assertFalse(json.contains(FULL_ID),
                "the full ID number must never appear in the JSON response");
        assertEquals(FULL_ID, pto.getIdNumber(),
                "the in-memory value must stay full for business logic and edit forms");
    }

    @Test
    void internalSyncDtoKeepsFullIdNumberForOfflineBusinessLogic() throws Exception {
        // PTOSyncDto drives offline editing/sync and legitimately needs the full ID.
        PTOSyncDto dto = PTOSyncDto.builder().idNumber(FULL_ID).build();

        String json = new ObjectMapper().writeValueAsString(dto);

        assertTrue(json.contains(FULL_ID),
                "the internal sync payload must keep the full ID number");
    }

    // ── Thymeleaf: the exact expression used in the TALOMS templates ────────

    @Test
    void thymeleafExpressionMasksIdNumber() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        Context context = new Context();
        context.setVariable("value", FULL_ID);

        String output = engine.process(
                "<span th:text=\"${T(za.co.taloms.common.IdMasker).maskIdNumber(value)}\">x</span>",
                context);

        assertTrue(output.contains(MASKED_ID),
                "the Thymeleaf display expression must render the masked ID number");
        assertFalse(output.contains(FULL_ID),
                "the Thymeleaf output must not contain the full ID number");
    }

    @Test
    void thymeleafExpressionHandlesNull() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        Context context = new Context();
        context.setVariable("value", null);

        String output = engine.process(
                "<span th:text=\"${T(za.co.taloms.common.IdMasker).maskIdNumber(value)}\">x</span>",
                context);

        assertFalse(output.contains("900101"));
    }
}
