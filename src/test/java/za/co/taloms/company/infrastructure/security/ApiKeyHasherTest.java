package za.co.taloms.company.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * API keys must be unguessable and must never be recoverable from their stored
 * representation.
 */
class ApiKeyHasherTest {

    private final ApiKeyHasher hasher = new ApiKeyHasher();

    private static final Pattern HEX_64 = Pattern.compile("^[0-9a-f]{64}$");

    @Test
    void shouldGenerateUniqueHighEntropyKeys() {
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            String key = hasher.generateRawKey();
            assertTrue(key.startsWith(ApiKeyHasher.KEY_PREFIX), "key must carry the documented prefix");
            // 32 random bytes base64url-encoded = 43 chars, plus the prefix.
            assertTrue(key.length() >= 40, "key must carry enough entropy: " + key.length());
            keys.add(key);
        }
        assertEquals(200, keys.size(), "generated keys must be unique");
    }

    @Test
    void shouldHashDeterministicallyToSha256Hex() {
        String raw = hasher.generateRawKey();

        String hash1 = hasher.hashApiKey(raw);
        String hash2 = hasher.hashApiKey(raw);

        assertEquals(hash1, hash2, "hashing must be deterministic");
        assertTrue(HEX_64.matcher(hash1).matches(), "hash must be a SHA-256 hex digest");
        assertNotEquals(raw, hash1, "the stored hash must never be the raw key");
    }

    @Test
    void shouldMatchOnlyTheCorrectKey() {
        String raw = hasher.generateRawKey();
        String storedHash = hasher.hashApiKey(raw);

        assertTrue(hasher.matches(raw, storedHash), "correct key must verify");
        assertFalse(hasher.matches(hasher.generateRawKey(), storedHash), "another key must not verify");
        assertFalse(hasher.matches(raw, hasher.hashApiKey("some-other-key")), "wrong digest must not verify");
    }

    @Test
    void shouldRejectBlankAndNullInputsSafely() {
        String storedHash = hasher.hashApiKey(hasher.generateRawKey());

        assertFalse(hasher.matches(null, storedHash));
        assertFalse(hasher.matches("", storedHash));
        assertFalse(hasher.matches("   ", storedHash));
        assertFalse(hasher.matches(hasher.generateRawKey(), null));
        assertFalse(hasher.matches(hasher.generateRawKey(), ""));
    }

    @Test
    void shouldHashIdNumbersWithoutStoringThem() {
        String idNumber = "9001015800085";

        String hash = hasher.hashIdNumber(idNumber);

        assertTrue(HEX_64.matcher(hash).matches());
        assertNotEquals(idNumber, hash);
        assertFalse(hash.contains(idNumber), "the digest must not contain the ID number");
        assertEquals(hash, hasher.hashIdNumber(idNumber), "digest must be stable for correlation");
        assertNull(hasher.hashIdNumber(null));
    }

    @Test
    void displayPrefixShouldNotRevealTheWholeKey() {
        String raw = hasher.generateRawKey();

        String prefix = hasher.displayPrefix(raw);

        assertNotEquals(raw, prefix, "the display prefix must not be the full key");
        assertTrue(raw.startsWith(prefix), "the prefix must be a leading fragment of the key");
    }
}