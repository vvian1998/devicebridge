const Location = (() => {
  let map = null;
  let marker = null;
  let panel = null;

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

    document.getElementById('location-coords').innerHTML = `
      Lat: ${lat.toFixed(6)} | Lng: ${lng.toFixed(6)}<br>
      Provider: ${provider || 'unknown'} | Accuracy: ${accuracy ? accuracy.toFixed(1) + 'm' : 'N/A'}
    `;
  }

  function _startTracking() {
    API.send('location', { action: 'startTracking' });
    App.toast('Location tracking started', 'info');
    API.onMessage((msg) => {
      if (msg.type === 'event' && msg.payload && msg.payload.type === 'location') {
        _updateLocation(msg.payload.latitude, msg.payload.longitude, msg.payload.provider, msg.payload.accuracy);
      }
    });
  }

  function _stopTracking() {
    API.send('location', { action: 'stopTracking' });
    App.toast('Location tracking stopped', 'info');
  }

  function destroy() {
    if (map) { map.remove(); map = null; marker = null; }
  }

  return { init, refresh, destroy, _startTracking, _stopTracking };
})();
