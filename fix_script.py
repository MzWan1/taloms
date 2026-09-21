import sys

with open("src/main/resources/templates/parcels/create.html", "r") as f:
    content = f.read()

# 1. Add loadExistingParcels
target1 = """    let villageBoundaryLayer = null; // outlines the selected village"""
replacement1 = """    let villageBoundaryLayer = null; // outlines the selected village
    let existingParcelsLayer = null; // existing parcels in the village

    function loadExistingParcels(villageId) {
        if (existingParcelsLayer) {
            map.removeLayer(existingParcelsLayer);
            existingParcelsLayer = null;
        }

        if (!villageId) return;

        fetch('/api/gis/parcels/village/' + villageId)
            .then(r => r.json())
            .then(res => {
                if (res.success && res.data) {
                    existingParcelsLayer = L.geoJSON(res.data, {
                        style: function (feature) {
                            return {
                                color: '#e74c3c',
                                weight: 2,
                                opacity: 0.8,
                                fillColor: '#e74c3c',
                                fillOpacity: 0.2
                            };
                        },
                        onEachFeature: function (feature, layer) {
                            if (feature.properties) {
                                let tooltipContent = `<b>Parcel ${feature.properties.parcelNumber || 'Unknown'}</b><br>Occupied`;
                                layer.bindTooltip(tooltipContent, { sticky: true });
                            }
                        }
                    }).addTo(map);
                }
            })
            .catch(err => console.error('Error loading existing parcels:', err));
    }"""
content = content.replace(target1, replacement1)

# 2. Add loadExistingParcels call
target2 = """            drawVillageBoundary(this.value);
        } else {
            infoDiv.style.display = 'none';
            document.getElementById('headmanName').value = '';
            document.getElementById('chiefName').value = '';
        }"""
replacement2 = """            drawVillageBoundary(this.value);
            loadExistingParcels(this.value);
        } else {
            infoDiv.style.display = 'none';
            document.getElementById('headmanName').value = '';
            document.getElementById('chiefName').value = '';
            if (existingParcelsLayer) {
                map.removeLayer(existingParcelsLayer);
                existingParcelsLayer = null;
            }
        }"""
content = content.replace(target2, replacement2)

# 3. Add BUSINESS_ERROR catch
target3 = """          return Promise.reject(new Error('Server error: ' + response.status));
        })
        .catch(function (err) {
          console.warn('Online save failed, queuing for sync:', err.message);"""
replacement3 = """          // Read error body from server
          return response.json().then(function(errData) {
              var errMsg = errData.message || 'Server error ' + response.status;
              throw new Error('BUSINESS_ERROR:' + errMsg);
          }).catch(function(e) {
              if (e.message && e.message.startsWith('BUSINESS_ERROR:')) {
                  throw e;
              }
              throw new Error('BUSINESS_ERROR:Server error ' + response.status);
          });
        })
        .catch(function (err) {
          if (err.message && err.message.startsWith('BUSINESS_ERROR:')) {
              // Server responded with a business validation error (e.g. 409 Overlap)
              var actualMessage = err.message.substring(15);
              alert(actualMessage);
              submitBtn.innerHTML = originalText;
              submitBtn.disabled = false;
              if (typeof isSubmitting !== 'undefined') isSubmitting = false;
              return;
          }
          console.warn('Online save failed, queuing for sync:', err.message);"""
content = content.replace(target3, replacement3)

with open("src/main/resources/templates/parcels/create.html", "w") as f:
    f.write(content)
