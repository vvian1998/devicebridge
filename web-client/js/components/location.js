const Location = (() => {
  let map = null;
  let marker = null;
  let panel = null;
  let _unsubscribe = null;

  function init() {
    panel = document.getElementById('panel-location');
    panel.innerHTML = `
      <div class="btn-group" style="margin-bottom:12px">
        <button class="btn btn-sm" onclick="Location.refresh()">Get Location</button>
        <button class="btn btn-sm btn-outline" onclick="Location._startTracking()">Track</button>
        <button class="btn btn-sm btn-outline" onclick="Location._stopTracking()">Stop</button>
      </div>
      <div id="location-coords" style="font-size:0.875rem;color:var(--text-secondary);margin-bottom:12px">
        Waiting for location...
      </div>
      <div id="map"></div>
    `;
  }

  function refresh() {
    _initMap();
    API.send('location', { action: 'get' }, (data, err) => {
      if (err) {
        document.getElementById('location-coords').textContent = 'Location unavailable. Check permissions.';
        return;
      }
      _updateLocation(data.latitude, data.longitude, data.provider, data.accuracy);
    });
  }

  function _initMap() {
    if (map) return;
    const el = document.getElementById('map');
    if (!el || typeof L === 'undefined') return;
    map = L.map('map').setView([0, 0], 2);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap'
    }).addTo(map);
  }

  function _updateLocation(lat, lng, provider, accuracy) {
    if (!map) _initMap();
    if (!map) return;

    const latlng = [lat, lng];
    if (marker) {
      marker.setLatLng(latlng);
    } else {
      marker = L.marker(latlng).addTo(map);
    }
    map.setView(latlng, 15);

    const coordsEl = document.getElementById('location-coords');
    coordsEl.innerHTML = '';
    const line1 = document.createElement('span');
    line1.textContent = `Lat: ${lat.toFixed(6)} | Lng: ${lng.toFixed(6)}`;
    coordsEl.appendChild(line1);
    coordsEl.appendChild(document.createElement('br'));
    const line2 = document.createElement('span');
    line2.textContent = `Provider: ${provider || 'unknown'} | Accuracy: ${accuracy ? accuracy.toFixed(1) + 'm' : 'N/A'}`;
    coordsEl.appendChild(line2);
  }

  function _startTracking() {
    API.send('location', { action: 'startTracking' });
    App.toast('Location tracking started', 'info');
    if (_unsubscribe) _unsubscribe();
    _unsubscribe = API.onMessage((msg) => {
      if (msg.type === 'event' && msg.payload && msg.payload.type === 'location_update') {
        let inner = msg.payload.data;
        if (typeof inner === 'string') {
          try { inner = JSON.parse(inner); } catch (e) {}
        }
        if (inner && inner.latitude != null) {
          _updateLocation(inner.latitude, inner.longitude, inner.provider, inner.accuracy);
        }
      }
    });
  }

  function _stopTracking() {
    API.send('location', { action: 'stopTracking' });
    if (_unsubscribe) { _unsubscribe(); _unsubscribe = null; }
    App.toast('Location tracking stopped', 'info');
  }

  function destroy() {
    if (_unsubscribe) { _unsubscribe(); _unsubscribe = null; }
    if (map) { map.remove(); map = null; marker = null; }
  }

  return { init, refresh, destroy, _startTracking, _stopTracking };
})();
