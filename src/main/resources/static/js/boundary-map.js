/**
 * TALOMS — interactive boundary shape editor & viewer (Leaflet, no plugins).
 *
 * Editor: starts as a circle of 10 draggable pins. The whole shape can be
 * dragged via the centre handle, every pin can be moved independently,
 * extra pins can be added (midpoint of the longest edge) and removed by
 * double-clicking a pin (minimum 3 pins). Only the shape is displayed —
 * coordinates are kept in a hidden input as JSON: [{"lat":..,"lng":..},...]
 */
(function () {
  'use strict';

  var DEFAULT_CENTER = [-28.8, 25.5];
  var DEFAULT_RADIUS_DEG = 0.05; // ~5.5 km circle
  var PIN_COUNT = 10;
  var MIN_PINS = 3;

  function circlePoints(center, radiusDeg, count) {
    var pts = [];
    for (var i = 0; i < count; i++) {
      var angle = (2 * Math.PI * i) / count;
      pts.push([
        center[0] + radiusDeg * Math.cos(angle),
        center[1] + radiusDeg * Math.sin(angle) * 1.4
      ]);
    }
    return pts;
  }

  function centroid(points) {
    var lat = 0, lng = 0;
    points.forEach(function (p) { lat += p[0]; lng += p[1]; });
    return [lat / points.length, lng / points.length];
  }

  /**
   * Creates an interactive boundary editor bound to a form.
   * opts: { mapId, hiddenInputId, existing (JSON string|null),
   *         center: [lat,lng]|null, contextPolygons: [{points:[[lat,lng]..], color, label}] }
   */
  function initEditor(opts) {
    var map = L.map(opts.mapId).setView(opts.center || DEFAULT_CENTER, 10);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(map);

    var hidden = document.getElementById(opts.hiddenInputId);
    var points;
    try {
      var existing = opts.existing ? JSON.parse(opts.existing) : null;
      points = (existing && existing.length >= MIN_PINS)
        ? existing.map(function (c) {
            // accept both {lat,lng} and {latitude,longitude} keys
            return [c.lat !== undefined ? c.lat : c.latitude,
                    c.lng !== undefined ? c.lng : c.longitude];
          })
        : null;
    } catch (e) { points = null; }
    if (!points) {
      points = circlePoints(opts.center || DEFAULT_CENTER, DEFAULT_RADIUS_DEG, PIN_COUNT);
    }

    (opts.contextPolygons || []).forEach(function (ctx) {
      if (ctx.points && ctx.points.length >= 3) {
        L.polygon(ctx.points, {
          color: ctx.color || '#6c757d', weight: 1.5,
          fillOpacity: 0.06, dashArray: '6 4'
        }).addTo(map).bindTooltip(ctx.label || '', { sticky: true });
      }
    });

    var polygon = L.polygon(points, {
      color: '#1B3A6B', weight: 2, fillOpacity: 0.18
    }).addTo(map);

    var vertexLayers = [];
    var handle = null;
    var syncing = false;

    function pinIcon(color) {
      return L.divIcon({
        className: '',
        iconSize: [14, 14],
        iconAnchor: [7, 7],
        html: '<span style="display:block;width:14px;height:14px;border-radius:50%;' +
          'background:' + color + ';border:2px solid #fff;box-shadow:0 1px 4px rgba(0,0,0,.5);' +
          'cursor:move;"></span>'
      });
    }

    function sync() {
      if (syncing) return;
      polygon.setLatLngs(points);
      if (hidden) {
        hidden.value = JSON.stringify(points.map(function (p) {
          return { lat: p[0], lng: p[1] };
        }));
        // let draft/persistence listeners know the boundary changed
        hidden.dispatchEvent(new Event('input', { bubbles: true }));
      }
      if (handle) handle.setLatLng(centroid(points));
      redrawPins();
    }

    function redrawPins() {
      vertexLayers.forEach(function (m) { map.removeLayer(m); });
      vertexLayers = [];
      points.forEach(function (p, idx) {
        var marker = L.marker(p, {
          draggable: true, icon: pinIcon('#1B3A6B'), zIndexOffset: 500
        }).addTo(map);
        marker.on('drag', function (e) {
          points[idx] = [e.target.getLatLng().lat, e.target.getLatLng().lng];
          polygon.setLatLngs(points);
          if (handle) handle.setLatLng(centroid(points));
        });
        marker.on('dragend', sync);
        marker.on('dblclick', function () {
          if (points.length <= MIN_PINS) return;
          points.splice(idx, 1);
          sync();
        });
        marker.bindTooltip('Drag to move \u2022 double-click to remove', { sticky: true });
        vertexLayers.push(marker);
      });
    }

    function addPoint() {
      // insert a new draggable pin at the midpoint of the longest edge
      var bestI = 0, bestD = -1;
      for (var i = 0; i < points.length; i++) {
        var a = points[i], b = points[(i + 1) % points.length];
        var d = Math.pow(a[0] - b[0], 2) + Math.pow(a[1] - b[1], 2);
        if (d > bestD) { bestD = d; bestI = i; }
      }
      var a = points[bestI], b = points[(bestI + 1) % points.length];
      points.splice(bestI + 1, 0, [(a[0] + b[0]) / 2, (a[1] + b[1]) / 2]);
      sync();
    }

    // Centre handle — dragging it moves the whole shape
    function makeHandle() {
      handle = L.marker(centroid(points), {
        draggable: true, icon: pinIcon('#D9534F'), zIndexOffset: 900
      }).addTo(map);
      handle.bindTooltip('Drag to move the whole shape', { sticky: true });
      var start = null, origin = null;
      handle.on('dragstart', function () {
        start = points.map(function (p) { return p.slice(); });
        origin = handle.getLatLng();
      });
      handle.on('drag', function (e) {
        var ll = e.target.getLatLng();
        var dLat = ll.lat - origin.lat, dLng = ll.lng - origin.lng;
        points = start.map(function (p) { return [p[0] + dLat, p[1] + dLng]; });
        polygon.setLatLngs(points);
        vertexLayers.forEach(function (m, i) {
          m.setLatLng(points[i]);
        });
      });
      handle.on('dragend', sync);
    }

    sync();
    makeHandle();
    map.fitBounds(polygon.getBounds().pad(0.35));

    var addBtn = document.getElementById(opts.addPointButtonId);
    if (addBtn) addBtn.addEventListener('click', addPoint);

    return {
      getJson: function () { return hidden ? hidden.value : null; }
    };
  }

  /**
   * Read-only viewer: draws saved polygons on a map.
   * opts: { mapId, polygons: [{points:[[lat,lng]..], color, label}] }
   */
  function renderViewer(opts) {
    var layers = [];
    (opts.polygons || []).forEach(function (p) {
      if (p.points && p.points.length >= 3) {
        layers.push(L.polygon(p.points, {
          color: p.color || '#1B3A6B', weight: 2, fillOpacity: 0.15
        }).bindTooltip(p.label || '', { sticky: true }));
      }
    });
    if (!layers.length) return null;

    var group = L.featureGroup(layers);
    var map = L.map(opts.mapId, { scrollWheelZoom: false });
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(map);
    layers.forEach(function (l) { l.addTo(map); });
    map.fitBounds(group.getBounds().pad(0.35));
    return { map: map, group: group };
  }

  window.TalomsBoundaryMap = {
    initEditor: initEditor,
    renderViewer: renderViewer
  };
})();
