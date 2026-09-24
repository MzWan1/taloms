package za.co.taloms.common.resiliency;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-memory caching for safe, read-only reference data.
 *
 * Design decisions (documented for the production-readiness review):
 *
 * WHAT IS CACHED
 *   - Traditional Authority reference data (all, all-active)
 *   - Village reference data (all, by-authority, by-headman)
 *   These are reference datasets that change infrequently and are fetched on
 *   many pages. They are NOT user-scoped: the controller applies scope filtering
 *   in-memory after the cache hit, so caching the full set is safe.
 *
 * WHAT IS NOT CACHED
 *   - Any endpoint that returns user-scoped PTOs, parcels, residents, or
 *     documents. Those are filtered by the user's authority/village scope and
 *     must never be shared across users.
 *   - Any mutating operation.
 *
 * CACHE KEY
 *   The cache manager uses a single namespace per cache name ("authorities",
 *   "authoritiesActive", "villages", "villagesByAuthority",
 *   "villagesByHeadman"). Method-level keys are the parameter values (e.g.
 *   authorityId).
 *
 * TTL / INVALIDATION
 *   ConcurrentMapCacheManager has no built-in TTL. Instead, every mutating
 *   method on the affected services is annotated with @CacheEvict(allEntries =
 *   true) so the cache is invalidated at the source of truth whenever
 *   reference data changes. This keeps the window between write and
 *   invalidation to a single request — consistent with the existing
 *   in-memory rate limiter's per-JVM model.
 *
 * WHY IN-MEMORY (NOT REDIS)
 *   TALOMS already uses per-JVM in-memory state for rate limiting without issue
 *   on a single-instance deployment. Adding Redis solely for caching would be
 *   an unnecessary architectural change with operational overhead (a new
 *   service to deploy, monitor, and back up). ConcurrentMapCacheManager uses
 *   only what Spring Framework already ships — zero new dependencies.
 *
 * ENVIRONMENT OVERRIDES
 *   taloms.resiliency.caching.enabled=true  (default, set in properties)
 */
@Configuration
@EnableCaching
public class CachingConfig {

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager manager = new ConcurrentMapCacheManager(
                "authorities",
                "authoritiesActive",
                "villages",
                "villagesByAuthority",
                "villagesByHeadman"
        );
        manager.setAllowNullValues(false);
        return manager;
    }
}
