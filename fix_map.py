import sys

with open("src/main/resources/templates/parcels/create.html", "r") as f:
    content = f.read()

# 1. Add Unlock button
target1 = """                            <button type="button" class="btn btn-sm btn-success" id="finishBtn" style="display:none;" onclick="finishCapture()">
                                <i class="bi bi-check-circle me-1"></i>Finish
                            </button>
                        </div>"""
replacement1 = """                            <button type="button" class="btn btn-sm btn-success" id="finishBtn" style="display:none;" onclick="finishCapture()">
                                <i class="bi bi-check-circle me-1"></i>Finish
                            </button>
                            <button type="button" class="btn btn-sm btn-outline-warning" id="unlockBtn" style="display:none;" onclick="unlockCapture()">
                                <i class="bi bi-unlock me-1"></i>Edit Points
                            </button>
                        </div>"""
content = content.replace(target1, replacement1)

# 2. Add isPointInPolygons and unlockCapture functions before updateMap
target2 = """    // ── Update Map ──────────────────────────────────────────────────────"""
replacement2 = """    // ── Validation Helpers ───────────────────────────────────────────────
    function isPointInPolygons(lat, lng, geoJsonFeatureCollection) {
        if (!geoJsonFeatureCollection || !geoJsonFeatureCollection.features) return false;
        
        function pointInPolygon(point, vs) {
            var x = point[0], y = point[1]; // GeoJSON is [lng, lat]
            var inside = false;
            for (var i = 0, j = vs.length - 1; i < vs.length; j = i++) {
                var xi = vs[i][0], yi = vs[i][1];
                var xj = vs[j][0], yj = vs[j][1];
                var intersect = ((yi > y) != (yj > y))
                    && (x < (xj - xi) * (y - yi) / (yj - yi) + xi);
                if (intersect) inside = !inside;
            }
            return inside;
        }

        const pt = [lng, lat];
        
        for (let feature of geoJsonFeatureCollection.features) {
            const geom = feature.geometry;
            if (!geom) continue;
            
            if (geom.type === 'Polygon') {
                for (let ring of geom.coordinates) {
                    if (pointInPolygon(pt, ring)) return true;
                }
            } else if (geom.type === 'MultiPolygon') {
                for (let poly of geom.coordinates) {
                    for (let ring of poly) {
                        if (pointInPolygon(pt, ring)) return true;
                    }
                }
            }
        }
        return false;
    }

    function unlockCapture() {
        if (!loopClosed) return;
        
        loopClosed = false;
        
        // Remove closure point if any
        const idx = gpsPoints.findIndex(p => p.isClosurePoint);
        if (idx > -1) {
            const seq = gpsPoints[idx].sequence;
            gpsPoints.splice(idx, 1);
            const card = document.getElementById('pointCard' + seq);
            if (card) card.remove();
        }
        
        document.getElementById('loopClosedBadge').style.display = 'none';
        document.getElementById('loopStatus').textContent = 'Loop: Open';
        document.getElementById('loopStatus').className = 'text-warning';
        document.getElementById('finishBtn').style.display = 'inline-block';
        document.getElementById('unlockBtn').style.display = 'none';
        document.getElementById('addPointBtn').disabled = false;
        document.getElementById('submitBtn').disabled = true;
        
        updateDisplay();
        document.querySelectorAll('[id^="captureGpsBtn"]').forEach(btn => {
            btn.disabled = false;
        });
    }

    // ── Update Map ──────────────────────────────────────────────────────"""
content = content.replace(target2, replacement2)

# 3. Modify markers.forEach loop in updateMap
target3 = """        sortedPoints.forEach((p, i) => {
            const isClosure = p.isClosurePoint;
            const icon = L.divIcon({
                className: 'point-marker' + (isClosure ? ' point-marker-closed' : ''),
                html: isClosure ? '✓' : (p.sequence || i + 1),
                iconSize: [24, 24],
                iconAnchor: [12, 12]
            });
            const marker = L.marker([p.latitude, p.longitude], {
                icon: icon,
                draggable: false
            }).addTo(map);
            const label = isClosure ? 'Closure Point' : `Point ${p.sequence || i + 1}`;
            const accText = p.accuracy ? `Accuracy: ±${Math.round(p.accuracy)}m` : 'Manual entry';
            marker.bindPopup(`${label}<br>Lat: ${p.latitude.toFixed(8)}<br>Lng: ${p.longitude.toFixed(8)}<br>${accText}`);
            markers.push(marker);
        });"""
replacement3 = """        let geoJson = existingParcelsLayer ? existingParcelsLayer.toGeoJSON() : null;

        sortedPoints.forEach((p, i) => {
            const isClosure = p.isClosurePoint;
            
            // Check for overlap
            let overlaps = false;
            if (geoJson) {
                overlaps = isPointInPolygons(p.latitude, p.longitude, geoJson);
            }
            let extraClass = overlaps ? ' point-marker-overlap blink-warning' : '';

            const icon = L.divIcon({
                className: 'point-marker' + (isClosure ? ' point-marker-closed' : '') + extraClass,
                html: isClosure ? '✓' : (p.sequence || i + 1),
                iconSize: [24, 24],
                iconAnchor: [12, 12]
            });
            const marker = L.marker([p.latitude, p.longitude], {
                icon: icon,
                draggable: false
            }).addTo(map);
            const label = isClosure ? 'Closure Point' : `Point ${p.sequence || i + 1}`;
            const accText = p.accuracy ? `Accuracy: ±${Math.round(p.accuracy)}m` : 'Manual entry';
            const warningText = overlaps ? '<br><span class="text-danger mt-1 d-block"><i class="bi bi-exclamation-triangle"></i> Point overlaps existing parcel!</span>' : '';
            marker.bindPopup(`${label}<br>Lat: ${p.latitude.toFixed(8)}<br>Lng: ${p.longitude.toFixed(8)}<br>${accText}${warningText}`);
            markers.push(marker);
        });"""
content = content.replace(target3, replacement3)

# 4. Modify finishCapture to hide finishBtn and show unlockBtn
target4 = """        document.getElementById('loopClosedBadge').style.display = 'inline-block';
        document.getElementById('loopStatus').textContent = 'Loop: Closed ✓';
        document.getElementById('loopStatus').className = 'text-success';
        document.getElementById('finishBtn').style.display = 'none';
        document.getElementById('addPointBtn').disabled = true;"""
replacement4 = """        document.getElementById('loopClosedBadge').style.display = 'inline-block';
        document.getElementById('loopStatus').textContent = 'Loop: Closed ✓';
        document.getElementById('loopStatus').className = 'text-success';
        document.getElementById('finishBtn').style.display = 'none';
        document.getElementById('unlockBtn').style.display = 'inline-block';
        document.getElementById('addPointBtn').disabled = true;"""
content = content.replace(target4, replacement4)

with open("src/main/resources/templates/parcels/create.html", "w") as f:
    f.write(content)
