const App = (() => {
  const panels = {};
  let currentPanel = null;

  const navItems = [
    { id: 'dashboard', label: 'Dashboard', icon: 'dashboard' },
    { id: 'filemanager', label: 'Files', icon: 'folder' },
    { id: 'gallery', label: 'Gallery', icon: 'image' },
    { id: 'terminal', label: 'Terminal', icon: 'terminal' },
    { id: 'contacts', label: 'Contacts', icon: 'users' },
    { id: 'notifications', label: 'Notifications', icon: 'bell' },
    { id: 'location', label: 'Location', icon: 'map-pin' },
    { id: 'camera', label: 'Camera', icon: 'camera' },
    { id: 'applist', label: 'Apps', icon: 'grid' },
    { id: 'controls', label: 'Controls', icon: 'sliders' }
  ];

  function init() {
    const params = new URLSearchParams(window.location.search);
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const defaultRelay = `${protocol}//${window.location.host}`;

    let relayUrl = params.get('relay') || localStorage.getItem('db_relay') || defaultRelay;
    let deviceId = params.get('id') || localStorage.getItem('db_device');

    if (!deviceId) {
      showConnectModal(relayUrl);
    } else {
      _start(relayUrl, deviceId);
    }
  }

  function showConnectModal(defaultRelayUrl) {
    const modal = document.getElementById('connect-modal');
    const inputDevice = document.getElementById('connect-device-id');
    const inputRelay = document.getElementById('connect-relay-url');
    const btnConnect = document.getElementById('connect-btn');

    if (!modal) return;

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const fallbackRelay = defaultRelayUrl || localStorage.getItem('db_relay') || `${protocol}//${window.location.host}`;

    inputRelay.value = fallbackRelay;
    inputDevice.value = localStorage.getItem('db_device') || '';
    modal.style.display = 'flex';

    btnConnect.onclick = () => {
      const devId = inputDevice.value.trim();
      const relUrl = inputRelay.value.trim();
      if (!devId) {
        toast('Please enter a Target Device ID', 'warning');
        return;
      }
      modal.style.display = 'none';
      _start(relUrl, devId);
    };
  }

  function switchDevice() {
    const currentRelay = localStorage.getItem('db_relay');
    showConnectModal(currentRelay);
  }

  function _start(relayUrl, deviceId) {
    localStorage.setItem('db_relay', relayUrl);
    localStorage.setItem('db_device', deviceId);

    const loading = document.getElementById('loading');
    if (loading) loading.style.display = 'none';

    Sidebar.render(navItems, deviceId);
    _createPanels();
    Sidebar.setActive('dashboard');
    navigate('dashboard');

    API.connect(relayUrl, deviceId, (online) => {
      Sidebar.setStatus(online);
    });

    API.onMessage((msg) => {
      if (msg.type === 'event' && panels[currentPanel] && panels[currentPanel].onEvent) {
        panels[currentPanel].onEvent(msg.payload);
      }
    });
  }

  function _createPanels() {
    panels.dashboard = Dashboard;
    panels.filemanager = FileManager;
    panels.gallery = Gallery;
    panels.terminal = Terminal;
    panels.contacts = Contacts;
    panels.notifications = Notifications;
    panels.location = Location;
    panels.camera = Camera;
    panels.applist = AppList;
    panels.controls = Controls;
  }

  function navigate(panelId) {
    currentPanel = panelId;
    const target = panels[panelId];
    if (target) {
      if (target.init && !target._initialized) {
        target.init();
        target._initialized = true;
      }
      if (target.refresh) {
        target.refresh();
      }
    }
  }

  function toast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;
    const el = document.createElement('div');
    el.className = `toast ${type}`;
    el.textContent = message;
    container.appendChild(el);
    setTimeout(() => { el.remove(); }, 3000);
  }

  return { init, navigate, switchDevice, showConnectModal, toast };
})();

document.addEventListener('DOMContentLoaded', () => App.init());
