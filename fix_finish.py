import sys

with open("src/main/resources/templates/parcels/create.html", "r") as f:
    content = f.read()

target = """        if (validPoints.length < 3) {
            alert('Please capture at least 3 valid points before closing the loop.');
            return;
        }"""
replacement = """        if (validPoints.length < 3) {
            alert('Please capture at least 3 valid points before closing the loop.');
            return;
        }

        let hasOverlap = false;
        let geoJson = typeof existingParcelsLayer !== 'undefined' && existingParcelsLayer ? existingParcelsLayer.toGeoJSON() : null;
        if (geoJson) {
            for (let p of validPoints) {
                if (typeof isPointInPolygons === 'function' && isPointInPolygons(p.latitude, p.longitude, geoJson)) {
                    hasOverlap = true;
                    break;
                }
            }
        }
        
        if (hasOverlap) {
            if (!confirm('Warning: One or more points overlap with an existing parcel! The system may reject this submission. Do you still want to close the loop?')) {
                return;
            }
        }"""

content = content.replace(target, replacement)

with open("src/main/resources/templates/parcels/create.html", "w") as f:
    f.write(content)
