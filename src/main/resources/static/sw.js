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

const CACHE_NAME = 'taloms-shell-v2';
const TILE_CACHE = 'taloms-tiles-v1';
const API_CACHE = 'taloms-api-v1';
const OFFLINE_PAGE = '/offline.html';

const PRECACHE_URLS = [
  '/',
  '/css/taloms.css',
  '/js/taloms.js',
  '/js/offline-db.js',
  '/js/boundary-map.js',
  '/js/form-draft.js',
  '/favicon.ico',
  '/manifest.webmanifest',
  '/icons/icon.svg',
  OFFLINE_PAGE
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
        return cache.addAll(allUrls);
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
      setTimeout(() => resolve(null), 20000);
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
    if (response && response.ok) {
      cache.put(request, response.clone());
      return response;
    }
    const cached = await cache.match(request);
    if (cached) return cached;
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
    });
    const cached = await cache.match(request);
    const response = await Promise.race([
      networkPromise,
      new Promise((resolve) => setTimeout(() => resolve(null), 5000))
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
  }
});

function openIndexedDB() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open('taloms-offline', 1);
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
