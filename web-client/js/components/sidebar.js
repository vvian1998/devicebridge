const Sidebar = (() => {
  const icons = {
    dashboard: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>',
    folder: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z"/></svg>',
    image: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>',
    terminal: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="4 17 10 11 4 5"/><line x1="12" y1="19" x2="20" y2="19"/></svg>',
    users: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/></svg>',
    bell: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 01-3.46 0"/></svg>',
    'map-pin': '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></svg>',
    camera: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M23 19a2 2 0 01-2 2H3a2 2 0 01-2-2V8a2 2 0 012-2h4l2-3h6l2 3h4a2 2 0 012 2z"/><circle cx="12" cy="13" r="4"/></svg>',
    grid: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>',
    sliders: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="4" y1="21" x2="4" y2="14"/><line x1="4" y1="10" x2="4" y2="3"/><line x1="12" y1="21" x2="12" y2="12"/><line x1="12" y1="8" x2="12" y2="3"/><line x1="20" y1="21" x2="20" y2="16"/><line x1="20" y1="12" x2="20" y2="3"/><line x1="1" y1="14" x2="7" y2="14"/><line x1="9" y1="8" x2="15" y2="8"/><line x1="17" y1="16" x2="23" y2="16"/></svg>'
  };

  function render(navItems, deviceId) {
    const sidebar = document.getElementById('sidebar');
    const content = document.getElementById('content');

    let navHtml = '';
    navItems.forEach(item => {
      navHtml += `<button class="nav-item" data-panel="${item.id}">
        <span class="icon">${icons[item.icon] || ''}</span>
        ${item.label}
      </button>`;
    });

    navItems.forEach(item => {
      const panelDiv = document.createElement('div');
      panelDiv.className = 'panel';
      panelDiv.id = `panel-${item.id}`;
      content.appendChild(panelDiv);
    });

    sidebar.innerHTML = `
      <div class="sidebar-header">
        <h2>DeviceBridge</h2>
        <div class="device-id">${_escapeHtml(deviceId)}</div>
      </div>
      <nav class="sidebar-nav">${navHtml}</nav>
      <div class="sidebar-status">
        <span class="status-dot offline"></span>
        <span class="status-text">Disconnected</span>
      </div>
    `;

    sidebar.querySelectorAll('.nav-item').forEach(btn => {
      btn.addEventListener('click', () => {
        const panelId = btn.dataset.panel;
        setActive(panelId);
        App.navigate(panelId);
      });
    });
  }

  function setActive(panelId) {
    document.querySelectorAll('.nav-item').forEach(b => b.classList.remove('active'));
    const btn = document.querySelector(`.nav-item[data-panel="${panelId}"]`);
    if (btn) btn.classList.add('active');

    document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
    const panel = document.getElementById(`panel-${panelId}`);
    if (panel) panel.classList.add('active');
  }

  function setStatus(online) {
    const dot = document.querySelector('.status-dot');
    const text = document.querySelector('.status-text');
    if (dot) {
      dot.className = 'status-dot ' + (online ? 'online' : 'offline');
    }
    if (text) {
      text.textContent = online ? 'Connected' : 'Disconnected';
    }
  }

  function _escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
  }

  return { render, setActive, setStatus };
})();
