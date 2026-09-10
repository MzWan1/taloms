/**
 * TALOMS — minimal IndexedDB layer for offline-first operation.
 *
 * Stores:
 *   parcels       – parcel metadata (id, parcelNumber, status, updatedAt, version, …)
 *   boundaries    – individual boundary points keyed by parcelId + sequence
 *   outbox        – queued mutations awaiting sync
 *   metadata      – app metadata (lastSyncAt, deviceId, …)
 *
 * No external dependencies. Uses native IndexedDB API with promise wrappers.
 */
(function () {
  'use strict';

  const DB_NAME = 'taloms-offline';
  const DB_VERSION = 1;

  function open() {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open(DB_NAME, DB_VERSION);
      request.onerror = () => reject(request.error);
      request.onsuccess = () => resolve(request.result);
      request.onupgradeneeded = (event) => {
        const db = event.target.result;
        if (!db.objectStoreNames.contains('parcels')) {
          const ps = db.createObjectStore('parcels', { keyPath: 'id' });
          ps.createIndex('parcelNumber', 'parcelNumber', { unique: true });
          ps.createIndex('villageId', 'villageId', { unique: false });
          ps.createIndex('status', 'status', { unique: false });
          ps.createIndex('updatedAt', 'updatedAt', { unique: false });
        }
        if (!db.objectStoreNames.contains('boundaries')) {
          const bs = db.createObjectStore('boundaries', { keyPath: ['parcelId', 'sequence'] });
          bs.createIndex('parcelId', 'parcelId', { unique: false });
        }
        if (!db.objectStoreNames.contains('outbox')) {
          const os = db.createObjectStore('outbox', { keyPath: 'id' });
          os.createIndex('status', 'status', { unique: false });
          os.createIndex('createdAt', 'createdAt', { unique: false });
        }
        if (!db.objectStoreNames.contains('metadata')) {
          db.createObjectStore('metadata', { keyPath: 'key' });
        }
        if (!db.objectStoreNames.contains('form-drafts')) {
          db.createObjectStore('form-drafts', { keyPath: 'key' });
        }
      };
    });
  }

  function tx(db, storeName, mode) {
    const transaction = db.transaction(storeName, mode);
    return transaction.objectStore(storeName);
  }

  function promisifyRequest(request) {
    return new Promise((resolve, reject) => {
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });
  }

  const TalomsDB = {
    async getDb() {
      return open();
    },

    // ── parcels ─────────────────────────────────────────────────────────
    async putParcel(parcel) {
      const db = await this.getDb();
      const store = tx(db, 'parcels', 'readwrite');
      return promisifyRequest(store.put(parcel));
    },

    async getParcel(id) {
      const db = await this.getDb();
      const store = tx(db, 'parcels', 'readonly');
      return promisifyRequest(store.get(id));
    },

    async getAllParcels() {
      const db = await this.getDb();
      const store = tx(db, 'parcels', 'readonly');
      return promisifyRequest(store.getAll());
    },

    async getParcelsByVillage(villageId) {
      const db = await this.getDb();
      const store = tx(db, 'parcels', 'readonly');
      const index = store.index('villageId');
      return promisifyRequest(index.getAll(villageId));
    },

    async deleteParcel(id) {
      const db = await this.getDb();
      const store = tx(db, 'parcels', 'readwrite');
      return promisifyRequest(store.delete(id));
    },

    async clearParcels() {
      const db = await this.getDb();
      const store = tx(db, 'parcels', 'readwrite');
      return promisifyRequest(store.clear());
    },

    // ── boundaries ──────────────────────────────────────────────────────
    async putBoundary(parcelId, sequence, boundary) {
      const db = await this.getDb();
      const store = tx(db, 'boundaries', 'readwrite');
      const record = Object.assign({ parcelId, sequence }, boundary);
      return promisifyRequest(store.put(record));
    },

    async putBoundaries(parcelId, boundaries) {
      const db = await this.getDb();
      const store = tx(db, 'boundaries', 'readwrite');
      for (const b of boundaries) {
        const record = Object.assign({ parcelId, sequence: b.sequence }, b);
        store.put(record);
      }
      return new Promise((resolve, reject) => {
        store.transaction.oncomplete = () => resolve();
        store.transaction.onerror = () => reject(store.transaction.error);
      });
    },

    async getBoundariesByParcel(parcelId) {
      const db = await this.getDb();
      const store = tx(db, 'boundaries', 'readonly');
      const index = store.index('parcelId');
      return promisifyRequest(index.getAll(parcelId));
    },

    async deleteBoundariesByParcel(parcelId) {
      const db = await this.getDb();
      const store = tx(db, 'boundaries', 'readwrite');
      const index = store.index('parcelId');
      const request = index.openCursor(parcelId);
      return new Promise((resolve, reject) => {
        request.onsuccess = (event) => {
          const cursor = event.target.result;
          if (cursor) {
            cursor.delete();
            cursor.continue();
          } else {
            resolve();
          }
        };
        request.onerror = () => reject(request.error);
      });
    },

    // ── outbox ──────────────────────────────────────────────────────────
    async enqueue(item) {
      const db = await this.getDb();
      const store = tx(db, 'outbox', 'readwrite');
      return promisifyRequest(store.put(item));
    },

    async dequeue(id) {
      const db = await this.getDb();
      const store = tx(db, 'outbox', 'readwrite');
      return promisifyRequest(store.delete(id));
    },

    async getOutboxByStatus(status) {
      const db = await this.getDb();
      const store = tx(db, 'outbox', 'readonly');
      const index = store.index('status');
      return promisifyRequest(index.getAll(status));
    },

    async getAllOutbox() {
      const db = await this.getDb();
      const store = tx(db, 'outbox', 'readonly');
      return promisifyRequest(store.getAll());
    },

    async updateOutboxStatus(id, status, retryCount) {
      const db = await this.getDb();
      const store = tx(db, 'outbox', 'readwrite');
      const getRequest = store.get(id);
      return new Promise((resolve, reject) => {
        getRequest.onsuccess = () => {
          const item = getRequest.result;
          if (!item) return resolve(null);
          item.status = status;
          item.retryCount = retryCount;
          item.lastAttemptAt = Date.now();
          const putRequest = store.put(item);
          putRequest.onsuccess = () => resolve(item);
          putRequest.onerror = () => reject(putRequest.error);
        };
        getRequest.onerror = () => reject(getRequest.error);
      });
    },

    // ── metadata ────────────────────────────────────────────────────────
    async putMetadata(key, value) {
      const db = await this.getDb();
      const store = tx(db, 'metadata', 'readwrite');
      return promisifyRequest(store.put({ key, value }));
    },

    async getMetadata(key) {
      const db = await this.getDb();
      const store = tx(db, 'metadata', 'readonly');
      const result = await promisifyRequest(store.get(key));
      return result ? result.value : null;
    },

    async deleteMetadata(key) {
      const db = await this.getDb();
      const store = tx(db, 'metadata', 'readwrite');
      return promisifyRequest(store.delete(key));
    },

    // ── form drafts ─────────────────────────────────────────────────────
    async putDraft(key, data) {
      const db = await this.getDb();
      const store = tx(db, 'form-drafts', 'readwrite');
      return promisifyRequest(store.put({ key, data, savedAt: Date.now() }));
    },

    async getDraft(key) {
      const db = await this.getDb();
      const store = tx(db, 'form-drafts', 'readonly');
      const result = await promisifyRequest(store.get(key));
      return result ? result.data : null;
    },

    async deleteDraft(key) {
      const db = await this.getDb();
      const store = tx(db, 'form-drafts', 'readwrite');
      return promisifyRequest(store.delete(key));
    },

    // ── utilities ───────────────────────────────────────────────────────
    async count(storeName) {
      const db = await this.getDb();
      const store = tx(db, storeName, 'readonly');
      return promisifyRequest(store.count());
    },

    async clearAll() {
      const db = await this.getDb();
      const names = ['parcels', 'boundaries', 'outbox', 'metadata'];
      for (const name of names) {
        tx(db, name, 'readwrite').clear();
      }
      return Promise.resolve();
    },

    // ── quota management ────────────────────────────────────────────────
    async checkQuota() {
      if (!navigator.storage || !navigator.storage.estimate) {
        return { usage: 0, quota: 0, percentUsed: 0 };
      }
      const estimate = await navigator.storage.estimate();
      const usage = estimate.usage || 0;
      const quota = estimate.quota || 0;
      const percentUsed = quota > 0 ? (usage / quota) * 100 : 0;

      if (percentUsed > 80) {
        console.warn('IndexedDB quota usage high:', percentUsed.toFixed(1) + '%');
        await this.evictOldestOutbox();
      }

      return { usage, quota, percentUsed };
    },

    async evictOldestOutbox() {
      try {
        const items = await this.getAllOutbox();
        const completed = items.filter(i => i.status === 'COMPLETED' || i.status === 'FAILED');
        if (completed.length === 0) return;

        completed.sort((a, b) => (a.createdAt || 0) - (b.createdAt || 0));
        const toEvict = completed.slice(0, Math.max(1, Math.floor(completed.length / 4)));
        for (const item of toEvict) {
          await this.dequeue(item.id);
        }
        console.log('Evicted', toEvict.length, 'old outbox items to free storage');
      } catch (e) {
        console.warn('Outbox eviction failed:', e);
      }
    }
  };

  window.TalomsDB = TalomsDB;
})();
