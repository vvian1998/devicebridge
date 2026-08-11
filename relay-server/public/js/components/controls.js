const Controls = (() => {
  let panel = null;

  function init() {
    panel = document.getElementById('panel-controls');
    panel.innerHTML = `
      <div class="card" style="margin-bottom:16px">
        <div class="card-header">Device Controls</div>
        <div class="btn-group" style="margin-top:8px">
          <button class="btn" onclick="Controls._vibrate(500)">Vibrate</button>
          <button class="btn btn-outline" onclick="Controls._vibrate(2000)">Long Vibrate</button>
          <button class="btn btn-outline" onclick="Controls._ring()">Ring</button>
          <button class="btn btn-outline" onclick="Controls._torch()">Toggle Torch</button>
        </div>
      </div>
      <div class="card" style="margin-bottom:16px">
        <div class="card-header">Screen</div>
        <div class="btn-group" style="margin-top:8px">
          <button class="btn btn-outline" onclick="Controls._screenOn()">Screen On</button>
          <button class="btn btn-outline" onclick="Controls._screenOff()">Screen Off</button>
          <button class="btn btn-outline" onclick="Controls._screenshot()">Screenshot</button>
        </div>
      </div>
      <div class="card" style="margin-bottom:16px">
        <div class="card-header">Clipboard</div>
        <div style="margin-top:8px">
          <textarea id="clipboard-text" rows="3" placeholder="Clipboard content will appear here..." readonly></textarea>
          <div class="btn-group" style="margin-top:8px">
            <button class="btn btn-sm" onclick="Controls._readClipboard()">Read</button>
            <button class="btn btn-sm btn-outline" onclick="Controls._setClipboard()">Set</button>
          </div>
        </div>
      </div>
      <div class="card">
        <div class="card-header">Quick Actions</div>
        <div class="btn-group" style="margin-top:8px">
          <button class="btn btn-outline" onclick="Controls._openApp()">Open App</button>
          <button class="btn btn-outline" onclick="Controls._playAudio()">Play Audio</button>
          <button class="btn btn-outline" onclick="Controls._lockScreen()">Lock Screen</button>
        </div>
      </div>
    `;
  }

  function refresh() {}

  function _vibrate(duration) {
    API.send('devicecontrol', { action: 'vibrate', duration });
    App.toast(`Vibrating ${duration}ms`, 'info');
  }

  function _ring() {
    API.send('devicecontrol', { action: 'ring' });
    App.toast('Ringing device', 'info');
  }

  function _torch() {
    API.send('devicecontrol', { action: 'torch' }, (data, err) => {
      if (err) { App.toast('Torch toggle failed', 'error'); return; }
      App.toast(data.on ? 'Torch ON' : 'Torch OFF', 'info');
    });
  }

  function _screenOn() {
    API.send('devicecontrol', { action: 'screenOn' });
  }

  function _screenOff() {
    API.send('devicecontrol', { action: 'screenOff' });
  }

  function _screenshot() {
    API.send('screenshot', { action: 'capture' }, (data, err) => {
      if (err) { App.toast('Screenshot failed', 'error'); return; }
      if (data && data.base64) {
        const a = document.createElement('a');
        a.href = 'data:image/png;base64,' + data.base64;
        a.download = 'screenshot_' + Date.now() + '.png';
        a.click();
        App.toast('Screenshot downloaded', 'success');
      }
    });
  }

  function _readClipboard() {
    API.send('clipboard', { action: 'read' }, (data, err) => {
      if (err) { App.toast('Failed to read clipboard', 'error'); return; }
      document.getElementById('clipboard-text').value = data.text || '';
    });
  }

  function _setClipboard() {
    const text = document.getElementById('clipboard-text').value;
    if (!text) return;
    API.send('clipboard', { action: 'set', text }, (data, err) => {
      if (err) { App.toast('Failed to set clipboard', 'error'); return; }
      App.toast('Clipboard updated', 'success');
    });
  }

  function _openApp() {
    const pkg = prompt('Package name:');
    if (!pkg) return;
    API.send('devicecontrol', { action: 'openApp', package: pkg });
  }

  function _playAudio() {
    const url = prompt('Audio URL:');
    if (!url) return;
    API.send('devicecontrol', { action: 'playAudio', url });
    App.toast('Playing audio', 'info');
  }

  function _lockScreen() {
    API.send('devicecontrol', { action: 'lockScreen' });
    App.toast('Screen locked', 'info');
  }

  function destroy() {}

  return { init, refresh, destroy, _vibrate, _ring, _torch, _screenOn, _screenOff,
    _screenshot, _readClipboard, _setClipboard, _openApp, _playAudio, _lockScreen };
})();
