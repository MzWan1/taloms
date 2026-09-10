/**
 * TALOMS — offline-first network layer, sync engine, and connectivity UX.
 *
 * Responsibilities:
 *  - Detect connectivity changes and update the persistent offline banner.
 *  - Intercept mutating fetch() calls and queue them in IndexedDB outbox.
 *  - Flush outbox when connectivity returns (with exponential backoff + jitter).
 *  - Pull delta from server and merge into local IndexedDB cache.
 *  - Handle 412 Precondition Failed conflicts with user-facing resolution UI.
 *  - Expose Taloms.Sync, Taloms.Network, Taloms.Outbox namespaces.
 */
(function () {
  'use strict';

  // ── Configuration ───────────────────────────────────────────────────
  const CONFIG = {
    PING_INTERVAL_MS: 30000,
    PING_URL: '/api/ping',
    SYNC_URL: '/api/parcels/sync/delta',
    PUSH_URL: '/api/parcels/sync/push',
    MAX_RETRIES: 8,
    BASE_RETRY_DELAY_MS: 1000,
    MAX_RETRY_DELAY_MS: 60000,
    GPS_AUTO_SAVE_INTERVAL_MS: 2000,
    OUTBOX_STATUS: { PENDING: 'PENDING', SYNCING: 'SYNCING', COMPLETED: 'COMPLETED', FAILED: 'FAILED' }
  };

  // ── Utilities ───────────────────────────────────────────────────────
  function generateId() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
      const r = Math.random() * 16 | 0;
      const v = c === 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  }

  function getBackoffDelay(retryCount) {
    const base = Math.min(CONFIG.BASE_RETRY_DELAY_MS * Math.pow(2, retryCount), CONFIG.MAX_RETRY_DELAY_MS);
    const jitter = Math.random() * 1000;
    return base + jitter;
  }

  function isSlowConnection() {
    const conn = navigator.connection || navigator.mozConnection || navigator.webkitConnection;
    return !!(conn && (conn.effectiveType === '2g' || conn.effectiveType === 'slow-2g' || conn.saveData));
  }

  // ── Network Monitor ─────────────────────────────────────────────────
  const Network = {
    isOnline: navigator.onLine,
    isSlow: isSlowConnection(),

    init() {
      window.addEventListener('online', () => {
        this.isOnline = true;
        this.updateBanner();
        this.flushOutbox();
      });
      window.addEventListener('offline', () => {
        this.isOnline = false;
        this.updateBanner();
      });
      window.addEventListener('connectionchange', () => {
        this.isSlow = isSlowConnection();
        this.updateBanner();
      });
      this.updateBanner();
    },

    updateBanner() {
      const banner = document.getElementById('offline-banner');
      const outboxCount = document.getElementById('outbox-count');
      if (!banner) return;

      if (!this.isOnline) {
        banner.style.display = 'block';
        banner.className = 'alert alert-warning py-2 mb-0 text-center';
        banner.innerHTML = '<i class="bi bi-wifi-off me-2"></i>You are offline. Changes will sync when connectivity returns.';
        if (outboxCount) outboxCount.style.display = 'none';
      } else if (this.isSlow) {
        banner.style.display = 'block';
        banner.className = 'alert alert-info py-2 mb-0 text-center';
        banner.innerHTML = '<i class="bi bi-speedometer2 me-2"></i>Low-bandwidth connection detected. App is optimizing data usage.';
        if (outboxCount) outboxCount.style.display = 'none';
      } else {
        banner.style.display = 'none';
        if (outboxCount) outboxCount.style.display = 'none';
      }
    },

    async updateOutboxCount() {
      const outboxCount = document.getElementById('outbox-count');
      if (!outboxCount) return;
      try {
        const items = await TalomsDB.getAllOutbox();
        const pending = items.filter(i => i.status === Taloms.Outbox.PENDING || i.status === Taloms.Outbox.FAILED).length;
        if (pending > 0) {
          outboxCount.textContent = pending + ' queued';
          outboxCount.style.display = 'inline-block';
        } else {
          outboxCount.style.display = 'none';
        }
      } catch (e) {
        // ignore
      }
    },

    async flushOutbox() {
      if (!this.isOnline) return;
      const items = await TalomsDB.getAllOutbox();
      const pending = items.filter(i => i.status === CONFIG.OUTBOX_STATUS.PENDING || i.status === CONFIG.OUTBOX_STATUS.FAILED);
      if (pending.length === 0) return;

      // Register background sync with service worker
      if ('serviceWorker' in navigator && 'SyncManager' in window) {
        try {
          const reg = await navigator.serviceWorker.ready;
          await reg.sync.register('taloms-outbox-sync');
        } catch (e) {
          console.warn('Background sync registration failed:', e);
        }
      }

      for (const item of pending) {
        await Taloms.Outbox.processItem(item);
      }
      this.updateBanner();
      this.updateOutboxCount();
    }
  };

  // ── Conflict Resolution UI ──────────────────────────────────────────
  const Conflict = {
    show(localVersion, serverVersion) {
      const modalHtml = `
        <div class="modal fade" id="conflictModal" tabindex="-1">
          <div class="modal-dialog modal-lg">
            <div class="modal-content">
              <div class="modal-header bg-warning">
                <h5 class="modal-title"><i class="bi bi-exclamation-triangle me-2"></i>Sync Conflict Detected</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
              </div>
              <div class="modal-body">
                <p class="mb-3">This parcel was modified by another user while you were offline. Please choose how to resolve the conflict:</p>
                <div class="row">
                  <div class="col-md-6">
                    <h6 class="fw-bold text-primary">Your Version (Offline)</h6>
                    <div class="card border-primary">
                      <div class="card-body">
                        <p><strong>Parcel Number:</strong> ${localVersion.parcelNumber || 'N/A'}</p>
                        <p><strong>Stand Number:</strong> ${localVersion.standNumber || 'N/A'}</p>
                        <p><strong>Status:</strong> ${localVersion.status || 'N/A'}</p>
                        <p><strong>Area:</strong> ${localVersion.areaM2 ? localVersion.areaM2.toFixed(2) + ' m²' : 'N/A'}</p>
                        <p><strong>Updated:</strong> ${localVersion.updatedAt ? new Date(localVersion.updatedAt).toLocaleString() : 'N/A'}</p>
                        <p><strong>Version:</strong> ${localVersion.version || 'N/A'}</p>
                      </div>
                    </div>
                  </div>
                  <div class="col-md-6">
                    <h6 class="fw-bold text-success">Server Version (Current)</h6>
                    <div class="card border-success">
                      <div class="card-body">
                        <p><strong>Parcel Number:</strong> ${serverVersion.parcelNumber || 'N/A'}</p>
                        <p><strong>Stand Number:</strong> ${serverVersion.standNumber || 'N/A'}</p>
                        <p><strong>Status:</strong> ${serverVersion.status || 'N/A'}</p>
                        <p><strong>Area:</strong> ${serverVersion.areaM2 ? serverVersion.areaM2.toFixed(2) + ' m²' : 'N/A'}</p>
                        <p><strong>Updated:</strong> ${serverVersion.updatedAt ? new Date(serverVersion.updatedAt).toLocaleString() : 'N/A'}</p>
                        <p><strong>Version:</strong> ${serverVersion.version || 'N/A'}</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal" onclick="Taloms.Conflict.keepServer()">
                  Keep Server Version
                </button>
                <button type="button" class="btn btn-primary" data-bs-dismiss="modal" onclick="Taloms.Conflict.keepLocal()">
                  Keep My Version
                </button>
                <button type="button" class="btn btn-warning" data-bs-dismiss="modal" onclick="Taloms.Conflict.merge()">
                  Merge (Newer Wins)
                </button>
              </div>
            </div>
          </div>
        </div>
      `;

      document.body.insertAdjacentHTML('beforeend', modalHtml);
      const modal = new bootstrap.Modal(document.getElementById('conflictModal'));
      modal.show();

      document.getElementById('conflictModal').addEventListener('hidden.bs.modal', () => {
        document.getElementById('conflictModal')?.remove();
      });
    },

    keepServer() {
      // Remove local version, keep server version
      TalomsDB.deleteParcel(this.localVersion.id).then(() => {
        TalomsDB.putParcel(this.serverVersion);
        Taloms.Network.updateOutboxCount();
      });
    },

    keepLocal() {
      // Force push local version with new version number
      this.localVersion.version = this.serverVersion.version + 1;
      TalomsDB.putParcel(this.localVersion);
      if (Taloms.Network.isOnline) {
        Taloms.Sync.pushLocalChanges();
      }
    },

    merge() {
      // Newer wins: compare timestamps
      const localTime = new Date(this.localVersion.updatedAt || 0).getTime();
      const serverTime = new Date(this.serverVersion.updatedAt || 0).getTime();
      if (localTime > serverTime) {
        this.keepLocal();
      } else {
        this.keepServer();
      }
    },

    localVersion: null,
    serverVersion: null
  };

  // ── Outbox Queue ────────────────────────────────────────────────────
  const Outbox = {
    async enqueue(method, url, body, headers) {
      const item = {
        id: generateId(),
        method: method.toUpperCase(),
        url: url,
        body: body,
        headers: headers || {},
        status: CONFIG.OUTBOX_STATUS.PENDING,
        retryCount: 0,
        createdAt: Date.now(),
        lastAttemptAt: null
      };
      await TalomsDB.enqueue(item);
      Network.updateOutboxCount();
      if (Network.isOnline) {
        setTimeout(() => this.processItem(item), 500);
      }
      return item;
    },

    async processItem(item) {
      item.status = CONFIG.OUTBOX_STATUS.SYNCING;
      await TalomsDB.enqueue(item);

      try {
        const response = await fetch(item.url, {
          method: item.method,
          headers: Object.assign({ 'Content-Type': 'application/json' }, item.headers),
          body: item.body,
          credentials: 'same-origin'
        });

        if (response.ok) {
          item.status = CONFIG.OUTBOX_STATUS.COMPLETED;
          await TalomsDB.enqueue(item);
          await TalomsDB.dequeue(item.id);
        } else if (response.status === 409 || response.status === 412) {
          // Conflict - mark as failed for user review
          item.status = CONFIG.OUTBOX_STATUS.FAILED;
          item.retryCount = (item.retryCount || 0) + 1;
          await TalomsDB.enqueue(item);

          // Try to parse conflict details
          try {
            const errorData = await response.json();
            if (errorData && errorData.data) {
              Conflict.localVersion = JSON.parse(item.body);
              Conflict.serverVersion = errorData.data;
              Conflict.show(Conflict.localVersion, Conflict.serverVersion);
            }
          } catch (e) {
            // ignore
          }
        } else {
          throw new Error('HTTP ' + response.status);
        }
      } catch (err) {
        item.retryCount = (item.retryCount || 0) + 1;
        if (item.retryCount >= CONFIG.MAX_RETRIES) {
          item.status = CONFIG.OUTBOX_STATUS.FAILED;
        } else {
          item.status = CONFIG.OUTBOX_STATUS.PENDING;
        }
        await TalomsDB.enqueue(item);
        const delay = getBackoffDelay(item.retryCount);
        setTimeout(() => this.processItem(item), delay);
      }

      Network.updateOutboxCount();
    }
  };

  // ── Sync Engine ─────────────────────────────────────────────────────
  const Sync = {
    lastSyncAt: null,

    async init() {
      this.lastSyncAt = await TalomsDB.getMetadata('lastSyncAt');
      if (window.TalomsDB) {
        TalomsDB.checkQuota().then(function(result) {
          if (result.percentUsed > 80) {
            console.warn('Storage quota critical:', result.percentUsed.toFixed(1) + '%');
          }
        });
      }
    },

    async pullDelta() {
      if (!navigator.onLine) return;
      const url = CONFIG.SYNC_URL + '?lastSyncAt=' + encodeURIComponent(this.lastSyncAt || '');
      try {
        const response = await fetch(url, { credentials: 'same-origin' });
        if (!response.ok) return;
        const data = await response.json();
        if (data && data.data) {
          for (const parcel of data.data) {
            await TalomsDB.putParcel(parcel);
            if (parcel.boundaries && parcel.boundaries.length) {
              await TalomsDB.putBoundaries(parcel.id, parcel.boundaries);
            }
          }
          if (data.serverTime) {
            this.lastSyncAt = data.serverTime;
            await TalomsDB.putMetadata('lastSyncAt', this.lastSyncAt);
          }
          if (window.TalomsDB) {
            TalomsDB.checkQuota();
          }
        }
      } catch (e) {
        console.warn('Delta sync failed:', e);
      }
    },

    async pushLocalChanges() {
      if (!navigator.onLine) return;
      const parcels = await TalomsDB.getAllParcels();
      const unsynced = parcels.filter(p => p._dirty);
      if (unsynced.length === 0) return;
      const payload = unsynced.map(p => ({
        id: p.id,
        parcelNumber: p.parcelNumber,
        standNumber: p.standNumber,
        villageId: p.villageId,
        status: p.status,
        parcelType: p.parcelType,
        areaM2: p.areaM2,
        areaHectares: p.areaHectares,
        centroidLat: p.centroidLat,
        centroidLng: p.centroidLng,
        perimeterM: p.perimeterM,
        notes: p.notes,
        captureMode: p.captureMode,
        chiefName: p.chiefName,
        headmanName: p.headmanName,
        version: p.version,
        updatedAt: p.updatedAt,
        boundaries: p.boundaries || []
      }));

      try {
        const response = await fetch(CONFIG.PUSH_URL, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ changes: payload }),
          credentials: 'same-origin'
        });
        if (response.ok) {
          for (const p of unsynced) {
            p._dirty = false;
            await TalomsDB.putParcel(p);
          }
        } else if (response.status === 412) {
          const errorData = await response.json();
          if (errorData && errorData.data) {
            Conflict.localVersion = payload[0];
            Conflict.serverVersion = errorData.data;
            Conflict.show(Conflict.localVersion, Conflict.serverVersion);
          }
        }
      } catch (e) {
        console.warn('Push sync failed:', e);
      }
    },

    async fullSync() {
      await this.pullDelta();
      await this.pushLocalChanges();
    }
  };

  // ── Fetch Interceptor ───────────────────────────────────────────────
  const OriginalFetch = window.fetch;
  window.fetch = function (resource, options) {
    const opts = options || {};
    const method = (opts.method || 'GET').toUpperCase();
    const isMutation = ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method);

    if (!isMutation || !navigator.onLine) {
      return OriginalFetch(resource, opts);
    }

    const url = typeof resource === 'string' ? resource : resource.url;
    const body = typeof opts.body === 'string' ? opts.body : JSON.stringify(opts.body);

    return OriginalFetch(resource, opts).catch((err) => {
      if (err && (err.name === 'TypeError' || err.message.includes('fetch'))) {
        return Taloms.Outbox.enqueue(method, url, body, opts.headers)
          .then(() => Promise.resolve(new Response(JSON.stringify({ success: false, queued: true }), {
            status: 202,
            headers: { 'Content-Type': 'application/json' }
          })));
      }
      return Promise.reject(err);
    });
  };

  // ── Public API ──────────────────────────────────────────────────────
  window.Taloms = window.Taloms || {};
  window.Taloms.Network = Network;
  window.Taloms.Outbox = Outbox;
  window.Taloms.Sync = Sync;
  window.Taloms.Conflict = Conflict;
  window.Taloms.CONFIG = CONFIG;

})();
