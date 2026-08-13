const Notifications = (() => {
  let panel = null;

  function init() {
    panel = document.getElementById('panel-notifications');
    panel.innerHTML = `
      <div style="display:flex;align-items:center;gap:12px;margin-bottom:16px">
        <h3 style="font-size:1rem">Notifications</h3>
        <button class="btn btn-sm btn-outline" onclick="Notifications.refresh()">Refresh</button>
      </div>
      <div id="notif-list"></div>
      <div id="notif-empty" class="empty-state">
        <div class="icon-lg">🔔</div>
        <p>No notifications. Ensure Notification Listener is enabled in Settings.</p>
      </div>
    `;
  }

  function refresh() {
    API.send('notifications', { action: 'list' }, (data, err) => {
      if (err) { App.toast('Failed to load notifications', 'error'); return; }
      _render(data.notifications || []);
    });
  }

  function _render(notifications) {
    const list = document.getElementById('notif-list');
    const empty = document.getElementById('notif-empty');
    if (!list) return;

    if (notifications.length === 0) {
      list.innerHTML = '';
      if (empty) empty.style.display = 'block';
      return;
    }
    if (empty) empty.style.display = 'none';

    list.innerHTML = notifications.map(n => `
      <div class="card" style="margin-bottom:8px">
        <div style="display:flex;align-items:center;gap:8px;margin-bottom:4px">
          ${n.icon ? `<img src="${_esc(n.icon)}" width="24" height="24" style="border-radius:4px" alt="">` : ''}
          <strong style="font-size:0.875rem">${_esc(n.appName || n.packageName || 'Unknown')}</strong>
          <span style="font-size:0.7rem;color:var(--text-muted);margin-left:auto">${_esc(n.time || '')}</span>
        </div>
        <div style="font-size:0.875rem;font-weight:500;margin-bottom:2px">${_esc(n.title || '')}</div>
        <div style="font-size:0.8125rem;color:var(--text-secondary)">${_esc(n.text || '')}</div>
      </div>
    `).join('');
  }

  function _esc(s) { const d = document.createElement('div'); d.textContent = String(s); return d.innerHTML; }

  function destroy() {}

  return { init, refresh, destroy };
})();
