const Camera = (() => {
  let panel = null;

  function init() {
    panel = document.getElementById('panel-camera');
    panel.innerHTML = `
      <div class="btn-group" style="margin-bottom:16px">
        <button class="btn btn-sm" onclick="Camera._capture()">Capture Photo</button>
        <button class="btn btn-sm btn-outline" onclick="Camera._toggleCamera()">Flip Camera</button>
      </div>
      <div id="camera-result">
        <p style="color:var(--text-muted)">Click Capture to take a photo from the device camera.</p>
      </div>
    `;
  }

  function refresh() {}

  function _capture() {
    API.send('camera', { action: 'capture' }, (data, err) => {
      if (err) { App.toast('Camera capture failed', 'error'); return; }
      const result = document.getElementById('camera-result');
      if (data && data.base64) {
        result.innerHTML = `<img src="data:image/jpeg;base64,${data.base64}" class="camera-preview" alt="Captured">
          <br><button class="btn btn-sm" onclick="Camera._downloadCapture('${data.base64}')">Download</button>`;
      } else {
        result.innerHTML = '<p style="color:var(--danger)">No image data received</p>';
      }
    });
  }

  function _toggleCamera() {
    API.send('camera', { action: 'toggle' }, (data, err) => {
      if (err) { App.toast('Failed to toggle camera', 'error'); return; }
      App.toast('Camera switched', 'success');
    });
  }

  function _downloadCapture(base64) {
    const a = document.createElement('a');
    a.href = 'data:image/jpeg;base64,' + base64;
    a.download = 'capture_' + Date.now() + '.jpg';
    a.click();
  }

  function destroy() {}

  return { init, refresh, destroy, _capture, _toggleCamera, _downloadCapture };
})();
