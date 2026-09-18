package za.co.taloms.company.infrastructure.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Generates and verifies company API keys.
 *
 * Uses only standard JDK cryptography:
 *   * {@link SecureRandom} for 256 bits of entropy (cryptographically secure),
 *   * SHA-256 (via {@link MessageDigest}) as the one-way stored representation,
 *   * {@link MessageDigest#isEqual} for a constant-time comparison.
 *
 * No custom cryptographic algorithm is invented. Because the raw key is 256 bits
 * of random data, SHA-256 is sufficient for pre-image resistance (there is no
 * low-entropy secret to brute-force, so no key-stretching KDF is required).
 */
@Component
public class ApiKeyHasher {

    /** Human-recognisable prefix on generated keys. Not secret. */
    public static final String KEY_PREFIX = "taloms_";

    /** Number of bytes of entropy in the random portion of a key. */
    private static final int KEY_ENTROPY_BYTES = 32;

    /** Characters of the raw key retained for display/identification. */
    private static final int DISPLAY_PREFIX_LENGTH = KEY_PREFIX.length() + 8;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a new raw API key, e.g. {@code taloms_<43 base64url chars>}.
     * This value is returned to the administrator exactly once and is never stored.
     */
    public String generateRawKey() {
        byte[] randomBytes = new byte[KEY_ENTROPY_BYTES];
        secureRandom.nextBytes(randomBytes);
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return KEY_PREFIX + encoded;
    }

    /** SHA-256 hex digest of a raw key — the value persisted in the database. */
    public String hashApiKey(String rawApiKey) {
        return sha256Hex(rawApiKey);
    }

    /** SHA-256 hex digest of a resident ID number, used only for log correlation. */
    public String hashIdNumber(String idNumber) {
        if (idNumber == null) {
            return null;
        }
        return sha256Hex(idNumber.trim());
    }

    /**
     * Constant-time verification of a supplied raw key against a stored digest.
     * Returns false for null/blank inputs and never throws on malformed digests.
     */
    public boolean matches(String rawApiKey, String storedHash) {
        if (rawApiKey == null || rawApiKey.isBlank() || storedHash == null || storedHash.isBlank()) {
            return false;
        }
        String computed = hashApiKey(rawApiKey);
        return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.UTF_8),
                storedHash.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    /** Short, non-secret fragment used to identify a key in the admin UI. */
    public String displayPrefix(String rawApiKey) {
        if (rawApiKey == null) {
            return null;
        }
        return rawApiKey.length() <= DISPLAY_PREFIX_LENGTH
                ? rawApiKey
                : rawApiKey.substring(0, DISPLAY_PREFIX_LENGTH);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the JDK; this cannot happen on a valid JVM.
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}