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
    PING_URL: '/api/parcels/ping',
    SYNC_URL: '/api/parcels/sync/delta',
    PUSH_URL: '/api/parcels/sync/push',
    PTO_SYNC_URL: '/api/ptos/sync/delta',
    PTO_PUSH_URL: '/api/ptos/sync/push',
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

  /**
   * Mask a South African ID number for display only. Preserves the first 6 and
   * last 3 characters and replaces the middle with '*', e.g.
   * "9001011234087" -> "900101****087". The full value is never modified.
   * Mirrors za.co.taloms.common.IdMasker#maskIdNumber on the server so the
   * masking rule lives in exactly one place per language.
   */
  function maskIdNumber(idNumber) {
    if (idNumber === null || idNumber === undefined) return null;
    const value = String(idNumber).trim();
    if (value.length === 0) return null;
    if (value.length < 10) return '*'.repeat(value.length);
    return value.slice(0, 6) + '*'.repeat(value.length - 9) + value.slice(value.length - 3);
  }

  // ── Network Monitor ─────────────────────────────────────────────────
  const Network = {
    isOnline: navigator.onLine,
    isSlow: isSlowConnection(),

    async init() {
      // Join the browser's connectivity events as a fast-path hint, but they
      // are NOT the source of truth (navigator.onLine is unreliable / often
      // undefined). The poll() loop below uses a real network probe instead.
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
      // Sync when page becomes visible again (handles sleep/wake, tab switching)
      document.addEventListener('visibilitychange', () => {
        if (!document.hidden) {
          this.poll();
        }
      });
      // Base connectivity on a real probe so we never rely on navigator.onLine.
      this.isOnline = await this.checkOnline();
      this.updateBanner();

      // If we opened a page already online (e.g. the parcels list right after
      // an offline save), flush any queued items immediately.
      if (this.isOnline) {
        this.flushOutbox();
      }

      // Poll connectivity + flush every 10s. This reliably detects the
      // offline->online transition even in browsers where navigator.onLine is
      // broken or never fires events.
      if (this._pollTimer) clearInterval(this._pollTimer);
      this._pollTimer = setInterval(() => this.poll(), 10000);
    },

    // Real, reliable connectivity check: reach /api/ping with a short timeout.
    // Returns true only when the server actually answers. Uses a timeout race
    // so it works even in browsers without the newer AbortController API.
    async checkOnline() {
      try {
        const timeout = new Promise((resolve) => setTimeout(() => resolve(false), 4000));
        // Append a cache-busting timestamp so the service worker never serves a
        // stale cached 200 — we need a REAL network round-trip to know.
        const probeUrl = CONFIG.PING_URL + '?t=' + Date.now();
        const probe = OriginalFetch(probeUrl, {
          credentials: 'same-origin',
          cache: 'no-store'
        }).then((res) => res.ok).catch(() => false);
        return await Promise.race([probe, timeout]);
      } catch (e) {
        return false;
      }
    },

    // One poll cycle: refresh connectivity and, if we are online, flush any
    // queued offline mutations so they are uploaded automatically.
    async poll() {
      const online = await this.checkOnline();
      if (online !== this.isOnline) {
        this.isOnline = online;
        this.updateBanner();
      }
      if (online) {
        await this.flushOutbox();
      }
      return online;
    },

    updateBanner() {
      let indicator = document.getElementById('connection-indicator');
      if (!indicator) {
        indicator = document.createElement('div');
        indicator.id = 'connection-indicator';
        document.body.appendChild(indicator);
      }

      if (!this.isOnline) {
        indicator.className = 'connection-indicator offline';
        indicator.innerHTML = '<span class="indicator-dot"></span><span class="indicator-text">Offline</span>';
        indicator.title = 'You are offline. Changes will sync when connectivity returns.';
        indicator.onclick = () => {
          indicator.classList.add('minimized');
          indicator.innerHTML = '<span class="indicator-dot"></span>';
          indicator.title = 'Offline — click to expand';
        };
      } else if (this.isSlow) {
        indicator.className = 'connection-indicator slow';
        indicator.innerHTML = '<span class="indicator-dot"></span><span class="indicator-text">Slow</span>';
        indicator.title = 'Low-bandwidth connection. Optimizing data usage.';
        indicator.onclick = () => {
          indicator.classList.add('minimized');
          indicator.innerHTML = '<span class="indicator-dot"></span>';
          indicator.title = 'Slow connection — click to expand';
        };
      } else {
        indicator.className = 'connection-indicator online';
        indicator.innerHTML = '<span class="indicator-dot"></span><span class="indicator-text">Online</span>';
        indicator.title = 'Connected';
        indicator.onclick = null;
        // Auto-hide after 3 seconds
        setTimeout(() => {
          if (indicator && indicator.classList.contains('online')) {
            indicator.classList.add('minimized');
            indicator.innerHTML = '<span class="indicator-dot"></span>';
          }
        }, 3000);
      }
    },

    async updateOutboxCount() {
      const outboxCount = document.getElementById('outbox-count');
      if (!outboxCount) return;
      try {
        const items = await TalomsDB.getAllOutbox();
        const pending = items.filter(i => i.status === CONFIG.OUTBOX_STATUS.PENDING || i.status === CONFIG.OUTBOX_STATUS.FAILED).length;
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
      const pending = items.filter(i => i.status === CONFIG.OUTBOX_STATUS.PENDING);
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
        // Use the ORIGINAL fetch directly (not the overridden window.fetch) so
        // the interceptor doesn't re-enqueue this item while we are replaying it.
        const response = await OriginalFetch(item.url, {
          method: item.method,
          headers: Object.assign({ 'Content-Type': 'application/json' }, item.headers),
          body: item.body,
          credentials: 'same-origin'
        });

        if (response.ok) {
          item.status = CONFIG.OUTBOX_STATUS.COMPLETED;
          await TalomsDB.enqueue(item);
          await TalomsDB.dequeue(item.id);
        } else if (response.status === 409 || response.status === 412 || response.status === 400 || response.status === 422) {
          // Conflict or Validation Error - mark as failed, do not retry
          item.status = CONFIG.OUTBOX_STATUS.FAILED;

          try {
            const errorData = await response.json();
            item.error = errorData.message || ('Server error ' + response.status);
            await TalomsDB.enqueue(item);

            if (errorData && errorData.data && (response.status === 409 || response.status === 412)) {
              Conflict.localVersion = JSON.parse(item.body);
              Conflict.serverVersion = errorData.data;
              Conflict.show(Conflict.localVersion, Conflict.serverVersion);
            }
          } catch (e) {
            item.error = 'Server error ' + response.status;
            await TalomsDB.enqueue(item);
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
      this.lastPtoSyncAt = await TalomsDB.getMetadata('lastPtoSyncAt');
      if (window.TalomsDB) {
        TalomsDB.checkQuota().then(function (result) {
          if (result.percentUsed > 80) {
            console.warn('Storage quota critical:', result.percentUsed.toFixed(1) + '%');
          }
        });
      }
    },

    async pullDelta() {
      if (!navigator.onLine) return;
      await this.pullParcelDelta();
      await this.pullPtoDelta();
    },

    async pullParcelDelta() {
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
        console.warn('Parcel delta sync failed:', e);
      }
    },

    async pullPtoDelta() {
      if (!navigator.onLine) return;
      const url = CONFIG.PTO_SYNC_URL + '?lastSyncAt=' + encodeURIComponent(this.lastPtoSyncAt || '');
      try {
        const response = await fetch(url, { credentials: 'same-origin' });
        if (!response.ok) return;
        const data = await response.json();
        if (data && data.data) {
          for (const pto of data.data) {
            if (pto.deleted) {
              await TalomsDB.deletePto(pto.id);
            } else {
              await TalomsDB.putPto(pto);
            }
          }
          if (data.serverTime) {
            this.lastPtoSyncAt = data.serverTime;
            await TalomsDB.putMetadata('lastPtoSyncAt', this.lastPtoSyncAt);
          }
          if (window.TalomsDB) {
            TalomsDB.checkQuota();
          }
        }
      } catch (e) {
        console.warn('PTO delta sync failed:', e);
      }
    },

    async pushLocalChanges() {
      if (!navigator.onLine) return;
      await this.pushLocalParcelChanges();
      await this.pushLocalPtoChanges();
    },

    async pushLocalParcelChanges() {
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

    async pushLocalPtoChanges() {
      if (!navigator.onLine) return;
      const ptos = await TalomsDB.getAllPtos();
      const unsynced = ptos.filter(p => p._dirty && !p.deleted);
      if (unsynced.length === 0) return;
      const payload = unsynced.map(p => ({
        id: p.id,
        ptoNumber: p.ptoNumber,
        ptoHolderName: p.ptoHolderName,
        idNumber: p.idNumber,
        contactPhone: p.contactPhone,
        contactEmail: p.contactEmail,
        purpose: p.purpose,
        status: p.status,
        issueDate: p.issueDate,
        expiryDate: p.expiryDate,
        notes: p.notes,
        villageId: p.villageId,
        traditionalAuthorityId: p.traditionalAuthorityId,
        parcelId: p.parcelId,
        approvedBy: p.approvedBy,
        approvedAt: p.approvedAt,
        approvalNotes: p.approvalNotes,
        revokedBy: p.revokedBy,
        revokedAt: p.revokedAt,
        revokeReason: p.revokeReason,
        allocatedBy: p.allocatedBy,
        allocationDate: p.allocationDate,
        standArea: p.standArea,
        surveyReference: p.surveyReference,
        boundaryDescription: p.boundaryDescription,
        allocationFeeReceipt: p.allocationFeeReceipt,
        taRecommendationRef: p.taRecommendationRef,
        communityResolutionRequired: p.communityResolutionRequired,
        createdBy: p.createdBy,
        createdAt: p.createdAt,
        updatedAt: p.updatedAt
      }));

      try {
        const response = await fetch(CONFIG.PTO_PUSH_URL, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ changes: payload }),
          credentials: 'same-origin'
        });
        if (response.ok) {
          for (const p of unsynced) {
            p._dirty = false;
            await TalomsDB.putPto(p);
          }
        }
      } catch (e) {
        console.warn('PTO push sync failed:', e);
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

    // If navigator.onLine is false (WiFi off / airplane mode), we know we're
    // offline immediately — no need to wait for the async probe. This matches
    // the original working behavior. When navigator.onLine is true but the
    // async probe later reveals no internet (macOS Ethernet quirk), the online
    // path's 5s timeout will catch it and fall back to the outbox.
    if (!isMutation || !navigator.onLine) {
      // Offline mutation: let normal GETs / non-mutations pass through, but
      // queue PTO + parcel + document writes so later sync can replay them.
      if (isMutation && navigator && !navigator.onLine) {
        var offUrl = typeof resource === 'string' ? resource : resource.url;
        var offBody = typeof opts.body === 'string' ? opts.body : JSON.stringify(opts.body);
        // Multipart FormData (PTO create/edit + file inputs) cannot be
        // serialised as JSON — stash the files so .catch() below can surface
        // a clear message instead of silently dropping them.
        var offIsForm = (typeof FormData !== 'undefined' && opts.body instanceof FormData)
          || (opts.headers && String(opts.headers['Content-Type'] || '').indexOf('multipart') !== -1);
        if (offUrl && (offUrl.indexOf('/api/ptos') !== -1
          || offUrl.indexOf('/api/parcels') !== -1
          || offUrl.indexOf('/api/documents') !== -1)) {
          if (offIsForm) {
            try {
              var names = [];
              opts.body.forEach(function (v, k) { if (v instanceof File) names.push(k + ':' + v.name); });
              offBody = JSON.stringify({ _multipart: true, _files: names, _note: 'files must be re-attached after sync' });
            } catch (fe) { offBody = JSON.stringify({ _multipart: true }); }
          }
          return Taloms.Outbox.enqueue(method, offUrl, offBody, opts.headers)
            .then(function () {
              return Promise.resolve(new Response(JSON.stringify({ success: false, queued: true }), {
                status: 202,
                headers: { 'Content-Type': 'application/json' }
              }));
            });
        }
      }
      return OriginalFetch(resource, opts);
    }

    const url = typeof resource === 'string' ? resource : resource.url;
    const body = typeof opts.body === 'string' ? opts.body : JSON.stringify(opts.body);

    // Online path: try the network with a 5-second timeout, fall back to the
    // outbox on failure. The timeout prevents indefinite hangs on macOS when
    // the physical link is up but there is no internet.
    return new Promise((resolve, reject) => {
      var timer = setTimeout(function () {
        reject(new Error('Network timeout'));
      }, 5000);
      OriginalFetch(resource, opts)
        .then(function (resp) {
          clearTimeout(timer);
          resolve(resp);
        })
        .catch(function (err) {
          clearTimeout(timer);
          reject(err);
        });
    }).catch(function (err) {
      if (err && (err.name === 'TypeError' || err.message.includes('fetch') || err.message === 'Network timeout')) {
        var qUrl = typeof resource === 'string' ? resource : resource.url;
        var qBody = typeof opts.body === 'string' ? opts.body : JSON.stringify(opts.body);
        return Taloms.Outbox.enqueue(method, qUrl, qBody, opts.headers)
          .then(function () {
            return Promise.resolve(new Response(JSON.stringify({ success: false, queued: true }), {
              status: 202,
              headers: { 'Content-Type': 'application/json' }
            }));
          });
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
  // Single source of truth for ID-number display masking in the browser.
  window.Taloms.Mask = { maskIdNumber: maskIdNumber };

  // ── Cache Warming (post-login) ──────────────────────────────────────
  window.Taloms.warmCache = function (onProgress) {
    if (!('serviceWorker' in navigator)) return Promise.resolve(false);
    return navigator.serviceWorker.ready.then(function (reg) {
      if (!reg.active) return false;
      return new Promise(function (resolve) {
        var channel = new MessageChannel();
        channel.port1.onmessage = function (event) {
          if (event.data && event.data.type === 'CACHE_WARMED') {
            resolve(event.data.payload);
          }
        };
        reg.active.postMessage({ type: 'WARM_CACHE' }, [channel.port2]);
        // Timeout after 30 seconds
        setTimeout(function () { resolve(null); }, 30000);
      });
    });
  };

})();
