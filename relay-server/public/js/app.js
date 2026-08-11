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
    const relayUrl = params.get('relay') || localStorage.getItem('db_relay') || prompt('Relay server URL:', 'ws://localhost:3000');
    const deviceId = params.get('id') || localStorage.getItem('db_device') || prompt('Device ID:');

    if (!relayUrl || !deviceId) {
      document.getElementById('loading').innerHTML = '<p style="color:#ef4444">Relay URL and Device ID are required.</p>';
      return;
    }

    localStorage.setItem('db_relay', relayUrl);
    localStorage.setItem('db_device', deviceId);

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
    if (currentPanel && panels[currentPanel] && panels[currentPanel].destroy) {
      panels[currentPanel].destroy();
    }
    currentPanel = panelId;
    if (panels[panelId] && panels[panelId].init) {
      panels[panelId].init();
      panels[panelId].refresh();
    }
  }

  function toast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const el = document.createElement('div');
    el.className = `toast ${type}`;
    el.textContent = message;
    container.appendChild(el);
    setTimeout(() => { el.remove(); }, 3000);
  }

  return { init, navigate, toast };
})();

document.addEventListener('DOMContentLoaded', () => App.init());
