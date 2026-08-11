const Gallery = (() => {
  let panel = null;
  let cursor = null;
  let loading = false;

  function init() {
    panel = document.getElementById('panel-gallery');
    panel.innerHTML = `
      <div class="btn-group" style="margin-bottom:16px">
        <button class="btn btn-sm" onclick="Gallery._toggleType('images')">Photos</button>
        <button class="btn btn-sm btn-outline" onclick="Gallery._toggleType('videos')">Videos</button>
        <button class="btn btn-sm btn-outline" onclick="Gallery._toggleType('all')">All</button>
      </div>
      <div class="gallery-grid" id="gallery-grid"></div>
      <div id="gallery-loading" style="text-align:center;padding:16px;display:none">
        <span class="loading-spinner"></span> Loading...
      </div>
    `;
  }

  function refresh() {
    cursor = null;
    _load('images');
  }

  function _toggleType(type) {
    document.querySelectorAll('#panel-gallery .btn').forEach(b => b.classList.add('btn-outline'));
    event.target.classList.remove('btn-outline');
    cursor = null;
    document.getElementById('gallery-grid').innerHTML = '';
    _load(type);
  }

  function _load(type) {
    if (loading) return;
    loading = true;
    document.getElementById('gallery-loading').style.display = 'block';

    API.send('media', { action: 'list', type, cursor, limit: 40 }, (data, err) => {
      loading = false;
      document.getElementById('gallery-loading').style.display = 'none';
      if (err) { App.toast('Failed to load media', 'error'); return; }
      const items = data.items || [];
      cursor = data.cursor || null;
      _renderItems(items, type === 'videos');
    });
  }

  function _renderItems(items, isVideo) {
    const grid = document.getElementById('gallery-grid');
    items.forEach(item => {
      const div = document.createElement('div');
      div.className = 'gallery-item';
      div.onclick = () => _openLightbox(item.thumbnail || item.path || '', isVideo);

      const thumbSrc = item.thumbnail
        ? _streamUrl(item.thumbnail)
        : (item.dataUri || '');

      if (thumbSrc) {
        div.innerHTML = `<img src="${thumbSrc}" alt="" loading="lazy">`;
      }
      if (isVideo) {
        div.innerHTML += '<span class="video-badge">▶</span>';
      }
      grid.appendChild(div);
    });

    if (cursor === null) {
      const sentinel = document.createElement('div');
      sentinel.id = 'gallery-sentinel';
      sentinel.style.height = '1px';
      grid.appendChild(sentinel);
    }
  }

  function _streamUrl(path) {
    if (!path) return '';
    if (path.startsWith('http') || path.startsWith('data:')) return path;
    return path;
  }

  function _openLightbox(src, isVideo) {
    const lb = document.getElementById('lightbox');
    const img = document.getElementById('lightbox-img');
    img.src = src;
    lb.classList.remove('hidden');
  }

  function destroy() {}

  return { init, refresh, destroy, _toggleType };
})();

document.addEventListener('click', (e) => {
  if (e.target.classList.contains('lightbox-close') || e.target.id === 'lightbox') {
    document.getElementById('lightbox').classList.add('hidden');
  }
});

document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') {
    document.getElementById('lightbox').classList.add('hidden');
  }
});
