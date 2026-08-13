const FileManager = (() => {
  let currentPath = '/sdcard/';
  let panel = null;

  function init() {
    panel = document.getElementById('panel-filemanager');
    panel.innerHTML = `
      <div class="search-bar">
        <input type="text" id="fm-search" placeholder="Search files..." oninput="FileManager._onSearch(this.value)">
      </div>
      <div class="breadcrumb" id="fm-breadcrumb"></div>
      <div class="btn-group" style="margin-bottom:12px">
        <button class="btn btn-sm" onclick="FileManager._upload()">Upload</button>
        <button class="btn btn-sm btn-outline" onclick="FileManager._mkdir()">New Folder</button>
      </div>
      <input type="file" id="fm-upload-input" style="display:none" onchange="FileManager._onUpload(event)">
      <div class="table-container">
        <table id="fm-table">
          <thead><tr><th>Name</th><th>Size</th><th>Modified</th><th></th></tr></thead>
          <tbody id="fm-tbody"></tbody>
        </table>
      </div>
    `;
  }

  function refresh() {
    _loadDir(currentPath);
  }

  function _loadDir(path) {
    currentPath = path;
    API.send('file', { action: 'list', path }, (data, err) => {
      if (err) { App.toast('Failed to load directory', 'error'); return; }
      _renderBreadcrumb(path);
      _renderTable(data.files || []);
    });
  }

  function _renderBreadcrumb(path) {
    const bc = document.getElementById('fm-breadcrumb');
    if (!bc) return;
    const parts = path.split('/').filter(Boolean);
    let cum = '/';
    let html = `<span onclick="FileManager._navigate('/')">/</span>`;
    parts.forEach((p, i) => {
      cum += p + '/';
      html += `<span class="sep">›</span>`;
      if (i === parts.length - 1) {
        html += `<span class="current">${_esc(p)}</span>`;
      } else {
        html += `<span onclick="FileManager._navigate('${_escAttr(cum)}')">${_esc(p)}</span>`;
      }
    });
    bc.innerHTML = html;
  }

  function _renderTable(files) {
    const tbody = document.getElementById('fm-tbody');
    if (!tbody) return;
    if (!files || files.length === 0) {
      tbody.innerHTML = '<tr><td colspan="4" class="empty-state">Empty directory</td></tr>';
      return;
    }
    const sorted = files.sort((a, b) => {
      if (a.isDir && !b.isDir) return -1;
      if (!a.isDir && b.isDir) return 1;
      return a.name.localeCompare(b.name);
    });
    tbody.innerHTML = sorted.map(f => `
      <tr>
        <td style="cursor:pointer;color:${f.isDir ? 'var(--accent)' : 'var(--text-primary)'}"
            onclick="${f.isDir ? `FileManager._navigate('${_escAttr(currentPath + f.name + '/')}')` : `FileManager._download('${_escAttr(f.path || currentPath + f.name)}')`}">
          ${f.isDir ? '📁' : _fileIcon(f.name)} ${_esc(f.name)}
        </td>
        <td>${f.isDir ? '-' : _fmtSize(f.size || 0)}</td>
        <td>${_esc(f.modified || '')}</td>
        <td>
          <button class="btn btn-sm btn-danger" onclick="event.stopPropagation();FileManager._delete('${_escAttr(f.path || currentPath + f.name)}', ${f.isDir})">Del</button>
        </td>
      </tr>
    `).join('');
  }

  function _navigate(path) { _loadDir(path); }

  function _download(filePath) {
    App.toast('Downloading...', 'info');
    API.send('file', { action: 'download', path: filePath }, (data, err) => {
      if (err) { App.toast('Download failed', 'error'); return; }
      try {
        const byteChars = atob(data.base64);
        const byteNumbers = new Uint8Array(byteChars.length);
        for (let i = 0; i < byteChars.length; i++) {
          byteNumbers[i] = byteChars.charCodeAt(i);
        }
        const blob = new Blob([byteNumbers], { type: data.mimeType || 'application/octet-stream' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = data.fileName || filePath.split('/').pop();
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(url);
        App.toast('Downloaded', 'success');
      } catch (e) {
        App.toast('Download failed: ' + e.message, 'error');
      }
    });
  }

  function _delete(filePath, isDir) {
    if (!confirm(`Delete "${filePath.split('/').pop()}"?`)) return;
    API.send('file', { action: 'delete', path: filePath }, (data, err) => {
      if (err) { App.toast('Delete failed', 'error'); return; }
      App.toast('Deleted', 'success');
      _loadDir(currentPath);
    });
  }

  function _upload() {
    document.getElementById('fm-upload-input').click();
  }

  function _onUpload(event) {
    const file = event.target.files[0];
    if (!file) return;
    const MAX_UPLOAD_SIZE = 50 * 1024 * 1024; // 50MB
    if (file.size > MAX_UPLOAD_SIZE) {
      App.toast(`File too large (${(file.size / (1024*1024)).toFixed(1)}MB). Max: 50MB`, 'error');
      event.target.value = '';
      return;
    }
    App.toast(`Uploading ${file.name} (${_fmtSize(file.size)})...`, 'info');
    const reader = new FileReader();
    reader.onload = () => {
      const base64 = reader.result.split(',')[1];
      API.send('file', { action: 'upload', path: currentPath + file.name, data: base64 }, (data, err) => {
        if (err) { App.toast('Upload failed', 'error'); return; }
        App.toast('Uploaded', 'success');
        _loadDir(currentPath);
      });
    };
    reader.onerror = () => {
      App.toast('Failed to read file', 'error');
    };
    reader.readAsDataURL(file);
    event.target.value = ''; // Reset input
  }

  function _mkdir() {
    const name = prompt('Folder name:');
    if (!name) return;
    API.send('file', { action: 'mkdir', path: currentPath + name }, (data, err) => {
      if (err) { App.toast('Failed to create folder', 'error'); return; }
      App.toast('Folder created', 'success');
      _loadDir(currentPath);
    });
  }

  function _onSearch(query) {
    if (!query) { _loadDir(currentPath); return; }
    API.send('file', { action: 'search', path: currentPath, query }, (data, err) => {
      if (err) return;
      _renderTable(data.files || []);
    });
  }

  function _fileIcon(name) {
    const ext = name.split('.').pop().toLowerCase();
    const map = { jpg: '🖼', jpeg: '🖼', png: '🖼', gif: '🖼', webp: '🖼', mp4: '🎬', mp3: '🎵',
      pdf: '📄', doc: '📝', docx: '📝', zip: '📦', apk: '📱', txt: '📃', json: '📋', xml: '📋' };
    return map[ext] || '📄';
  }

  function _fmtSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
  }

  function _esc(s) { const d = document.createElement('div'); d.textContent = String(s); return d.innerHTML; }
  function _escAttr(s) { return String(s).replace(/'/g, "\\'").replace(/"/g, '&quot;'); }

  function destroy() {}

  return { init, refresh, destroy, _navigate, _download, _delete, _upload, _onUpload, _mkdir, _onSearch };
})();
