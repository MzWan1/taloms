package za.co.taloms.common.resiliency;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Thin read-through cache facade over Spring's CacheManager.
 *
 * WHAT IS CACHED
 *   Reference data whose backend fetch is expensive and whose invalidation is
 *   owned by the object using this cache. Cached values MUST be safe to serve
 *   to any caller — never user-scoped data without extra isolation.
 *
 * CACHE KEY
 *   String-based. Caller defines the key scheme.
 *
 * TTL / INVALIDATION
 *   Handled by Spring's @CacheEvict annotations on the mutating service methods
 *   (see CachingConfig).
 *
 * WHY NOT SOMETHING ELSE
 *   This is a convenience facade only; storage is still Spring's configured
 *   CacheManager (currently an in-memory ConcurrentMapCache). Adding Redis
 *   would be a new dependency/operational burden that is not required by current
 *   TALOMS traffic.
 */
@Component
public final class ReadThroughCache<K, V> {

    private final CacheManager cacheManager;

    ReadThroughCache(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public Optional<V> get(K key, Class<V> valueType) {
        Cache cache = cacheManager.getCache("auditRead");
        if (cache == null) {
            return Optional.empty();
        }
        Object raw = cache.get(key, valueType);
        if (raw == null) {
            return Optional.empty();
        }
        return Optional.of(valueType.cast(raw));
    }

    public void put(K key, V value) {
        Cache cache = cacheManager.getCache("auditRead");
        if (cache != null) {
            cache.put(key, value);
        }
    }

    public void clear() {
        Cache cache = cacheManager.getCache("auditRead");
        if (cache != null) {
            cache.clear();
        }
    }
}
