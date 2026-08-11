const Sms = (() => {
  let panel = null;

  function init() {
    panel = document.getElementById('panel-sms');
    if (!panel) return;
    panel.innerHTML = `
      <div class="breadcrumb" style="margin-bottom: 15px;">
        <span class="current">SMS Messages</span>
        <button class="btn btn-sm" onclick="Sms.refresh()" style="float: right;">Refresh</button>
      </div>
      <div class="table-container">
        <table id="sms-table" style="width: 100%; border-collapse: collapse;">
          <thead>
            <tr>
              <th style="text-align: left; padding: 8px; border-bottom: 1px solid var(--border);">Contact</th>
              <th style="text-align: left; padding: 8px; border-bottom: 1px solid var(--border);">Message</th>
              <th style="text-align: left; padding: 8px; border-bottom: 1px solid var(--border);">Date</th>
              <th style="text-align: left; padding: 8px; border-bottom: 1px solid var(--border);">Type</th>
            </tr>
          </thead>
          <tbody id="sms-tbody">
            <tr><td colspan="4" class="empty-state">Loading messages...</td></tr>
          </tbody>
        </table>
      </div>
    `;
  }

  function refresh() {
    App.toast('Loading SMS...', 'info');
    API.send('sms', { action: 'list', limit: 100 }, (data, err) => {
      if (err) {
        App.toast('Failed to load SMS', 'error');
        return;
      }
      _renderTable(data.messages || []);
    });
  }

  function _renderTable(messages) {
    const tbody = document.getElementById('sms-tbody');
    if (!tbody) return;
    
    if (messages.length === 0) {
      tbody.innerHTML = '<tr><td colspan="4" class="empty-state">No messages found.</td></tr>';
      return;
    }

    tbody.innerHTML = messages.map(msg => `
      <tr style="border-bottom: 1px solid var(--border-light);">
        <td style="padding: 10px 8px; vertical-align: top; font-weight: 500;">
          ${_esc(msg.address)}
        </td>
        <td style="padding: 10px 8px; vertical-align: top; max-width: 400px; word-wrap: break-word;">
          ${_esc(msg.body)}
        </td>
        <td style="padding: 10px 8px; vertical-align: top; color: var(--text-secondary); font-size: 0.9em;">
          ${_esc(msg.date)}
        </td>
        <td style="padding: 10px 8px; vertical-align: top;">
          <span style="padding: 2px 6px; border-radius: 4px; font-size: 0.8em; background: ${msg.type === 'sent' ? 'rgba(59,130,246,0.1)' : 'rgba(16,185,129,0.1)'}; color: ${msg.type === 'sent' ? '#3b82f6' : '#10b981'};">
            ${msg.type.toUpperCase()}
          </span>
        </td>
      </tr>
    `).join('');
  }

  function _esc(s) {
    return String(s || '').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  }

  return { init, refresh };
})();
