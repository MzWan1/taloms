/**
 * TALOMS — draft persistence for "create" forms.
 * Saves field values (including the mapped boundary) to localStorage as the
 * user types, restores them when the page is (re)loaded, and clears the
 * draft when the form is actually submitted.
 *
 * fields: array of "inputId" or { id: "inputId", labelSpanId: "spanId" } —
 * the labelSpan (user-search display name) is saved/restored alongside.
 */
(function () {
  'use strict';

  window.TalomsFormDraft = {
    attach: function (storageKey, fields) {
      var entries = fields.map(function (f) {
        return typeof f === 'string' ? { id: f } : f;
      });
      var els = entries
        .map(function (e) { return document.getElementById(e.id); })
        .filter(Boolean);
      if (!els.length) return;

      function save() {
        var data = {};
        entries.forEach(function (e) {
          var el = document.getElementById(e.id);
          if (el) data[e.id] = el.value;
          if (e.labelSpanId) {
            var span = document.getElementById(e.labelSpanId);
            if (span) data[e.id + '__label'] = span.textContent;
          }
        });
        try { localStorage.setItem(storageKey, JSON.stringify(data)); }
        catch (err) { /* storage unavailable — ignore */ }
      }

      function restore() {
        var data;
        try { data = JSON.parse(localStorage.getItem(storageKey)); }
        catch (err) { return; }
        if (!data) return;
        entries.forEach(function (e) {
          var el = document.getElementById(e.id);
          if (el && data[e.id] && !el.value) {
            el.value = data[e.id];
          }
          if (e.labelSpanId && data[e.id + '__label']) {
            var span = document.getElementById(e.labelSpanId);
            var sel = document.getElementById(e.id.replace(/Id$/, 'Selected'));
            if (span && span.textContent.trim() === '') {
              span.textContent = data[e.id + '__label'];
              if (sel) sel.style.display = 'block';
            }
          }
        });
      }

      function clear() {
        try { localStorage.removeItem(storageKey); } catch (err) { /* ignore */ }
      }

      document.addEventListener('input', function (ev) {
        if (els.indexOf(ev.target) !== -1) save();
      });

      els[0].closest('form').addEventListener('submit', clear);

      // restore after the map editor (and templates) initialised
      if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', restore);
      } else {
        restore();
      }
    }
  };
})();
