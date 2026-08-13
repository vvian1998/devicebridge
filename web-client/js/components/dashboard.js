const Dashboard = (() => {
  let intervalId = null;
  let panel = null;

  function init() {
    panel = document.getElementById('panel-dashboard');
    panel.innerHTML = `
      <div class="stat-grid" id="dash-stats"></div>
      <div class="card" style="margin-top:16px">
        <div class="card-header">Device Info</div>
        <div id="dash-device-info" style="color:var(--text-secondary);font-size:0.875rem;line-height:1.8">Loading...</div>
      </div>
    `;
  }

  function refresh() {
    API.send('system', { action: 'info' }, (data, err) => {
      if (err) return;
      _renderSystemInfo(data);
    });
    if (!intervalId) {
      intervalId = setInterval(() => {
        API.send('system', { action: 'info' }, (data) => {
          if (data) _renderSystemInfo(data);
        });
      }, 5000);
    }
  }

  function _renderSystemInfo(data) {
    const stats = document.getElementById('dash-stats');
    if (!stats) return;

    const batteryPct = data.battery || 0;
    const batteryColor = batteryPct > 60 ? 'green' : batteryPct > 20 ? 'yellow' : 'red';

    stats.innerHTML = `
      <div class="stat-card">
        <div class="stat-label">Battery</div>
        <div class="stat-value">${batteryPct}%</div>
        <div class="stat-sub">${data.batteryCharging ? 'Charging' : 'Discharging'}</div>
        <div class="bar-container"><div class="bar-fill ${batteryColor}" style="width:${batteryPct}%"></div></div>
      </div>
      <div class="stat-card">
        <div class="stat-label">RAM</div>
        <div class="stat-value">${_fmtMB(data.ramUsed || 0)}</div>
        <div class="stat-sub">/ ${_fmtMB(data.ramTotal || 0)} total</div>
        <div class="bar-container"><div class="bar-fill blue" style="width:${data.ramPct || 0}%"></div></div>
      </div>
      <div class="stat-card">
        <div class="stat-label">Storage</div>
        <div class="stat-value">${_fmtGB(data.storageUsed || 0)}</div>
        <div class="stat-sub">/ ${_fmtGB(data.storageTotal || 0)} total</div>
        <div class="bar-container"><div class="bar-fill yellow" style="width:${data.storagePct || 0}%"></div></div>
      </div>
      <div class="stat-card">
        <div class="stat-label">CPU Temp</div>
        <div class="stat-value">${data.cpuTemp || 'N/A'}°C</div>
        <div class="stat-sub">${_esc(data.cpuModel || '')}</div>
      </div>
    `;

    const info = document.getElementById('dash-device-info');
    if (info) {
      info.innerHTML = `
        <div><strong>Model:</strong> ${_esc(data.model || 'Unknown')}</div>
        <div><strong>Manufacturer:</strong> ${_esc(data.manufacturer || 'Unknown')}</div>
        <div><strong>Android:</strong> ${_esc(data.androidVersion || 'Unknown')} (SDK ${_esc(data.sdk || '?')})</div>
        <div><strong>Screen:</strong> ${data.screenWidth || '?'}x${data.screenHeight || '?'} @ ${data.density || '?'}dpi</div>
        <div><strong>Uptime:</strong> ${_fmtUptime(data.uptime || 0)}</div>
        <div><strong>IP:</strong> ${_esc(data.ipAddress || 'Unknown')}</div>
        <div><strong>Rooted:</strong> ${data.isRooted ? 'Yes' : 'No'}</div>
      `;
    }
  }

  function _fmtMB(bytes) { return (bytes / (1024 * 1024)).toFixed(1) + ' MB'; }
  function _fmtGB(bytes) { return (bytes / (1024 * 1024 * 1024)).toFixed(1) + ' GB'; }
  function _fmtUptime(ms) {
    const s = Math.floor(ms / 1000);
    const d = Math.floor(s / 86400);
    const h = Math.floor((s % 86400) / 3600);
    const m = Math.floor((s % 3600) / 60);
    return `${d}d ${h}h ${m}m`;
  }

  function _esc(s) {
    const div = document.createElement('div');
    div.textContent = String(s);
    return div.innerHTML;
  }

  function destroy() {
    if (intervalId) { clearInterval(intervalId); intervalId = null; }
  }

  return { init, refresh, destroy };
})();
