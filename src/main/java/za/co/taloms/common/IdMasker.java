package za.co.taloms.common;

/**
 * Utility for masking sensitive identifiers so they are never exposed in full
 * outside the backend.
 * <p>
 * Two masking modes are provided:
 * <ul>
 *   <li>{@link #maskIdNumber(String)} — the canonical masking used for South
 *       African ID numbers. A 13-digit ID is rendered as
 *       {@code 900101****087} (first 6 characters + mask + last 3 characters).</li>
 *   <li>{@link #mask(String)} / {@link #mask(Long)} — the legacy fixed
 *       {@code ***} mask used by {@link MaskedLongSerializer} for internal
 *       numeric identifiers. Left unchanged for backwards compatibility.</li>
 * </ul>
 * <p>
 * Internal operations (database lookups, service layer logic, validation,
 * authentication, etc.) always use the real, unmasked value. Masking only
 * happens at the presentation/DTO layer, so the full value remains available
 * everywhere it is legitimately required.
 */
public final class IdMasker {

    // Minimum length to attempt masking (anything shorter shown as-is)
    private static final int MIN_LENGTH_FOR_MASKING = 7;
    private static final String MASK = "***";
    /** Leading characters preserved when masking a South African ID number. */
    private static final int ID_PREFIX_LENGTH = 6;
    /** Trailing characters preserved when masking a South African ID number. */
    private static final int ID_SUFFIX_LENGTH = 3;

    private IdMasker() {
        // utility class
    }

    /**
     * Mask a numeric ID for display purposes.
     * Shows first 6 and last 3 digits, masking everything in between.
     *
     * @param id the raw ID value
     * @return masked string like "123456***789", or the original value if too short
     */
    public static String mask(Long id) {
        if (id == null) {
            return null;
        }
        return mask(id.toString());
    }

    /**
     * Mask a string ID for display purposes.
     * Shows first 6 and last 3 characters, masking everything in between.
     *
     * @param id the raw ID value
     * @return masked string like "123456***789", or the original value if too short
     */
    public static String mask(String id) {
        if (id == null) {
            return null;
        }
        if (id.length() < MIN_LENGTH_FOR_MASKING) {
            // Too short to mask meaningfully — return as-is
            return id;
        }
        if (id.length() <= 9) {
            // Short but maskable — balance visibility vs security
            // For 7 chars: show first 6 + last 1 (mask middle 0)
            // For 8 chars: show first 6 + last 2 (mask middle 0)
            // For 9 chars: show first 6 + last 3 (mask middle 0)
            String prefix = id.substring(0, Math.min(6, id.length()));
            String suffix = id.substring(Math.max(id.length() - 3, 0));
            // If prefix + suffix >= full length, nothing to mask
            if (prefix.length() + suffix.length() >= id.length()) {
                return id;
            }
            return prefix + MASK + suffix;
        }
        // Standard masking for IDs with 10+ characters
        String prefix = id.substring(0, 6);
        String suffix = id.substring(id.length() - 3);
        return prefix + MASK + suffix;
    }

    /**
     * Mask a South African ID number for display.
     * <p>
     * Preserves the first {@value #ID_PREFIX_LENGTH} characters and the last
     * {@value #ID_SUFFIX_LENGTH} characters, replacing every character in between
     * with {@code *}. A standard 13-digit SA ID therefore becomes:
     * <pre>
     *   9001011234087  →  900101****087
     * </pre>
     * <p>
     * The original value is never modified — only the returned display value is
     * masked. Callers must keep using the full value for storage, lookups,
     * validation, authentication and any other legitimate internal operation.
     * <p>
     * Edge cases (this method never throws):
     * <ul>
     *   <li>{@code null} or blank → {@code null} (there is nothing to display)</li>
     *   <li>fewer than 10 characters → every character is replaced with {@code *},
     *       because the value is too short to reveal a 6-character prefix and a
     *       3-character suffix without exposing the whole value</li>
     *   <li>10 characters or more → first 6 + {@code *} mask + last 3</li>
     * </ul>
     *
     * @param idNumber the raw ID number (e.g. {@code "9001011234087"})
     * @return the masked ID number (e.g. {@code "900101****087"}) or {@code null}
     */
    public static String maskIdNumber(String idNumber) {
        if (idNumber == null) {
            return null;
        }
        String value = idNumber.trim();
        if (value.isEmpty()) {
            return null;
        }
        int length = value.length();
        if (length < ID_PREFIX_LENGTH + ID_SUFFIX_LENGTH + 1) {
            // Shorter than 10 characters: too short to safely keep a prefix and a
            // suffix, so hide the value entirely rather than leaking it.
            return "*".repeat(length);
        }
        int maskedLength = length - ID_PREFIX_LENGTH - ID_SUFFIX_LENGTH;
        return value.substring(0, ID_PREFIX_LENGTH)
                + "*".repeat(maskedLength)
                + value.substring(length - ID_SUFFIX_LENGTH);
    }

    /**
     * Check whether a value should be masked (length >= threshold).
     */
    public static boolean shouldMask(String value) {
        return value != null && value.length() >= MIN_LENGTH_FOR_MASKING;
    }
}

