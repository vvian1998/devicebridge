const Contacts = (() => {
  let panel = null;

  function init() {
    panel = document.getElementById('panel-contacts');
    panel.innerHTML = `
      <div class="search-bar">
        <input type="text" id="contact-search" placeholder="Search contacts..." oninput="Contacts._onSearch(this.value)">
      </div>
      <div class="table-container">
        <table id="contact-table">
          <thead><tr><th>Name</th><th>Phone</th><th>Email</th></tr></thead>
          <tbody id="contact-tbody"></tbody>
        </table>
      </div>
      <div id="contact-empty" class="empty-state" style="display:none">
        <div class="icon-lg">👥</div>
        <p>No contacts found</p>
      </div>
    `;
  }

  function refresh() {
    API.send('contacts', { action: 'list' }, (data, err) => {
      if (err) { App.toast('Failed to load contacts', 'error'); return; }
      _render(data.contacts || []);
    });
  }

  function _render(contacts) {
    const tbody = document.getElementById('contact-tbody');
    const empty = document.getElementById('contact-empty');
    if (!tbody) return;
    if (contacts.length === 0) {
      tbody.innerHTML = '';
      if (empty) empty.style.display = 'block';
      return;
    }
    if (empty) empty.style.display = 'none';
    tbody.innerHTML = contacts.map(c => `
      <tr>
        <td>${_esc(c.name || 'Unknown')}</td>
        <td style="font-family:monospace">${_esc(c.phone || '-')}</td>
        <td>${_esc(c.email || '-')}</td>
      </tr>
    `).join('');
  }

  function _onSearch(query) {
    API.send('contacts', { action: 'search', query }, (data, err) => {
      if (err) return;
      _render(data.contacts || []);
    });
  }

  function _esc(s) { return String(s).replace(/</g, '&lt;').replace(/>/g, '&gt;'); }

  function destroy() {}

  return { init, refresh, destroy, _onSearch };
})();
