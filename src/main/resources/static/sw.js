/**
 * TALOMS Service Worker — offline-first caching for rural South Africa.
 *
 * Strategy:
 *  - App shell (HTML, CSS, JS, manifest): StaleWhileRevalidate with 7-day max-age.
 *  - Static assets (icons, fonts): CacheFirst with content-hashed filenames.
 *  - API: NetworkFirst with 20s timeout, falling back to IndexedDB cache.
 *  - Tile layer: StaleWhileRevalidate with 30-day max-age (low-bandwidth tolerant).
 *
 * Install: precache known static assets + Vite-manifest hashed assets.
 * Activate: purge old caches.
 * Fetch: route by request type.
 * Sync: flush outbox when connectivity returns.
 * Message: accept cache management commands from the page.
 */

const CACHE_NAME = 'taloms-shell-v5'; // v5: handleTileRequest accepts opaque tile responses (fixes map 503s)
const TILE_CACHE = 'taloms-tiles-v1';
const API_CACHE = 'taloms-api-v1';
const OFFLINE_PAGE = '/offline.html';

// Only precache static assets that don't require auth
const PRECACHE_URLS = [
  '/',
  '/login',
  '/offline.html',
  '/css/bootstrap.min.css',
  '/css/bootstrap-icons.css',
  '/css/taloms.css',
  '/css/leaflet.min.css',
  '/css/error.css',
  '/js/bootstrap.bundle.min.js',
  '/js/taloms.js',
  '/js/offline-db.js',
  '/js/boundary-map.js',
  '/js/form-draft.js',
  '/js/leaflet.min.js',
  '/favicon.ico',
  '/manifest.webmanifest',
  '/icons/icon.svg',
];

// Pages to cache after login (require authentication)
const WARM_CACHE_URLS = [
  '/dashboard',
  '/parcels',
  '/parcels/create',
  '/ptos',
  '/ptos/create',
  '/gis',
  '/documents',
  '/documents/upload',
  '/authorities',
  '/authorities/create',
  '/villages',
  '/users',
  '/users/create',
  '/users/change-password',
  '/audit'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(async function(cache) {
        const allUrls = [];

        // Try to load Vite manifest for hashed assets
        try {
          const response = await fetch('/.vite/manifest.json');
          const manifest = await response.json();
          // Collect all hashed file URLs from the manifest
          const hashedUrls = Object.values(manifest).flatMap(function(chunk) {
            const urls = [chunk.file];
            if (chunk.css) urls.push(...chunk.css);
            if (chunk.assets) urls.push(...chunk.assets);
            return urls;
          });
          allUrls.push(...hashedUrls);
        } catch (e) {
          console.warn('Vite manifest not found, using static precache');
        }

        allUrls.push(...PRECACHE_URLS);
        // Fetch with credentials (cookies) so authenticated pages work
        // Only cache successful responses, skip failures silently
        return Promise.all(
          allUrls.map(url => {
            return fetch(url, { credentials: 'same-origin' })
              .then(response => {
                if (response.ok) {
                  return cache.put(url, response);
                }
              })
              .catch(() => { /* skip failed URLs */ });
          })
        );
      })
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  const currentCaches = [CACHE_NAME, TILE_CACHE, API_CACHE];
  event.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(
        keys.map((key) => {
          if (!currentCaches.includes(key)) {
            return caches.delete(key);
          }
        })
      );
    }).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const { request } = event;
  const url = new URL(request.url);

  // Skip non-GET requests (let them through to network)
  if (request.method !== 'GET') {
    return;
  }

  // Skip chrome-extension and other non-http(s) schemes
  if (!url.protocol.startsWith('http')) {
    return;
  }

  // API requests — NetworkFirst
  if (url.pathname.startsWith('/api/')) {
    event.respondWith(handleApiRequest(request));
    return;
  }

  // Tile requests — StaleWhileRevalidate
  if (url.hostname.includes('tile.openstreetmap.org') || url.hostname.includes('cartocdn.com')) {
    event.respondWith(handleTileRequest(request));
    return;
  }

  // App shell and static assets — StaleWhileRevalidate
  event.respondWith(handleAppShellRequest(request));
});

async function handleApiRequest(request) {
  const cache = await caches.open(API_CACHE);
  const cacheKey = request.url + '|' + request.method;

  try {
    const networkPromise = fetch(request.clone()).then((response) => {
      if (response.ok) {
        cache.put(cacheKey, response.clone());
      }
      return response;
    });

    const timeoutPromise = new Promise((resolve) => {
      setTimeout(() => resolve(null), 10000);
    });

    const response = await Promise.race([networkPromise, timeoutPromise]);
    if (response) return response;

    const cached = await cache.match(cacheKey);
    if (cached) return cached;

    return new Response(JSON.stringify({ success: false, message: 'Offline — no cached data' }), {
      status: 503,
      headers: { 'Content-Type': 'application/json' }
    });
  } catch (err) {
    const cached = await cache.match(cacheKey);
    if (cached) return cached;
    return new Response(JSON.stringify({ success: false, message: 'Offline — network error' }), {
      status: 503,
      headers: { 'Content-Type': 'application/json' }
    });
  }
}

async function handleTileRequest(request) {
  const cache = await caches.open(TILE_CACHE);
  try {
    const networkPromise = fetch(request.clone());
    const timeoutPromise = new Promise((resolve) => {
      setTimeout(() => resolve(null), 15000);
    });
    const response = await Promise.race([networkPromise, timeoutPromise]);
    // NOTE: map tiles are loaded by the browser as images (no-cors), so a
    // successful fetch comes back as an *opaque* response whose `ok` is
    // always false (status 0). Accept opaque responses as hits — otherwise
    // every tile is wrongly discarded and the map fails with a 503.
    if (response && (response.ok || response.type === 'opaque' || response.status === 0)) {
      try {
        await cache.put(request, response.clone());
      } catch (cacheErr) {
        console.warn('Tile cache put failed:', cacheErr);
      }
      return response;
    }
    const cached = await cache.match(request);
    if (cached) return cached;
    // Surface the real upstream response (e.g. tile-server 4xx/5xx) instead
    // of masking it with a synthetic 503, so DevTools shows the true cause.
    if (response) return response;
    return new Response('', { status: 503 });
  } catch (err) {
    const cached = await cache.match(request);
    if (cached) return cached;
    return new Response('', { status: 503 });
  }
}

async function handleAppShellRequest(request) {
  const cache = await caches.open(CACHE_NAME);
  try {
    const networkPromise = fetch(request.clone()).then((response) => {
      if (response.ok) {
        cache.put(request, response.clone());
      }
      return response;
    }).catch(() => null);
    const cached = await cache.match(request);
    const response = await Promise.race([
      networkPromise,
      new Promise((resolve) => setTimeout(() => resolve(null), 3000))
    ]);
    if (response) return response;
    if (cached) return cached;
    // Offline fallback for navigation requests
    if (request.mode === 'navigate') {
      return await cache.match(OFFLINE_PAGE) || await cache.match('/');
    }
    return new Response('Offline', { status: 503 });
  } catch (err) {
    const cached = await cache.match(request);
    if (cached) return cached;
    // Offline fallback for navigation requests
    if (request.mode === 'navigate') {
      return await cache.match(OFFLINE_PAGE) || await cache.match('/');
    }
    return new Response('Offline', { status: 503 });
  }
}

// Background sync for outbox
self.addEventListener('sync', (event) => {
  if (event.tag === 'taloms-outbox-sync') {
    event.waitUntil(flushOutbox());
  }
});

async function flushOutbox() {
  const db = await openIndexedDB();
  const tx = db.transaction('outbox', 'readwrite');
  const store = tx.objectStore('outbox');
  const items = await promisifyRequest(store.getAll());

  for (const item of items) {
    if (item.status === 'PENDING' || item.status === 'FAILED') {
      try {
        const response = await fetch(item.url, {
          method: item.method,
          headers: item.headers,
          body: item.body
        });
        if (response.ok) {
          store.delete(item.id);
        } else {
          item.status = 'FAILED';
          item.retryCount = (item.retryCount || 0) + 1;
          store.put(item);
        }
      } catch (err) {
        item.status = 'FAILED';
        item.retryCount = (item.retryCount || 0) + 1;
        store.put(item);
      }
    }
  }
}

// Message channel for cache management
self.addEventListener('message', (event) => {
  const { type, payload } = event.data || {};

  switch (type) {
    case 'CLEAR_APP_CACHE':
      event.waitUntil(
        caches.delete(CACHE_NAME)
          .then(() => caches.open(CACHE_NAME))
          .then(() => event.source && event.source.postMessage({ type: 'CACHE_CLEARED' }))
      );
      break;

    case 'SKIP_WAITING':
      self.skipWaiting();
      break;

    case 'GET_CACHE_USAGE':
      event.waitUntil(
        (async function() {
          const cacheNames = await caches.keys();
          let totalEntries = 0;
          for (const name of cacheNames) {
            const cache = await caches.open(name);
            totalEntries += await cache.keys();
          }
          event.source && event.source.postMessage({
            type: 'CACHE_USAGE',
            payload: { cacheNames: cacheNames, totalEntries: totalEntries }
          });
        })()
      );
      break;

    case 'WARM_CACHE':
      event.waitUntil(
        (async function() {
          const cache = await caches.open(CACHE_NAME);
          const results = { cached: 0, failed: 0, skipped: [] };
          for (const url of WARM_CACHE_URLS) {
            try {
              const response = await fetch(url, { credentials: 'same-origin' });
              if (response.ok) {
                // Cache under the original URL, even if redirected
                // This ensures pages like /villages (which redirect to /authorities/4 for CHIEF)
                // are available offline under the original URL
                await cache.put(url, response);
                results.cached++;
              } else {
                results.failed++;
                results.skipped.push(url + ' (HTTP ' + response.status + ')');
              }
            } catch (e) {
              results.failed++;
              results.skipped.push(url + ' (error)');
            }
          }
          // Reply on the MessageChannel port if provided, otherwise on event.source
          var ports = event.ports;
          if (ports && ports.length > 0) {
            ports[0].postMessage({ type: 'CACHE_WARMED', payload: results });
          } else if (event.source) {
            event.source.postMessage({ type: 'CACHE_WARMED', payload: results });
          }
        })()
      );
      break;
  }
});

function openIndexedDB() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open('taloms-offline', 2);
    request.onerror = () => reject(request.error);
    request.onsuccess = () => resolve(request.result);
  });
}

function promisifyRequest(request) {
  return new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}
