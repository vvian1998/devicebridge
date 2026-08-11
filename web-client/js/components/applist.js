const AppList = (() => {
  let panel = null;

  function init() {
    panel = document.getElementById('panel-applist');
    panel.innerHTML = `
      <div class="search-bar">
        <input type="text" id="app-search" placeholder="Search installed apps..." oninput="AppList._onSearch(this.value)">
      </div>
      <div class="table-container">
        <table id="app-table">
          <thead><tr><th></th><th>Name</th><th>Package</th><th>Version</th></tr></thead>
          <tbody id="app-tbody"></tbody>
        </table>
      </div>
      <div id="app-empty" class="empty-state" style="display:none">
        <div class="icon-lg">📱</div>
        <p>No apps found</p>
      </div>
    `;
  }

  function refresh() {
    API.send('applist', { action: 'list' }, (data, err) => {
      if (err) { App.toast('Failed to load app list', 'error'); return; }
      _render(data.apps || []);
    });
  }

  function _render(apps) {
    const tbody = document.getElementById('app-tbody');
    const empty = document.getElementById('app-empty');
    if (!tbody) return;
    if (apps.length === 0) {
      tbody.innerHTML = '';
      if (empty) empty.style.display = 'block';
      return;
    }
    if (empty) empty.style.display = 'none';

    const sorted = apps.sort((a, b) => (a.name || a.packageName).localeCompare(b.name || b.packageName));
    tbody.innerHTML = sorted.map(a => `
      <tr>
        <td>${a.icon ? `<img src="${_esc(a.icon)}" width="28" height="28" style="border-radius:6px" alt="">` : '📱'}</td>
        <td>${_esc(a.name || a.packageName || 'Unknown')}</td>
        <td style="font-family:monospace;font-size:0.75rem">${_esc(a.packageName || '-')}</td>
        <td>${_esc(a.version || '-')}</td>
      </tr>
    `).join('');
  }

  function _onSearch(query) {
    API.send('applist', { action: 'search', query }, (data, err) => {
      if (err) return;
      _render(data.apps || []);
    });
  }

  function _esc(s) { return String(s).replace(/</g, '&lt;').replace(/>/g, '&gt;'); }

  function destroy() {}

  return { init, refresh, destroy, _onSearch };
})();
