const API = (() => {
  let ws = null;
  let deviceId = null;
  let relayUrl = null;
  let pendingRequests = {};
  let pendingTimers = {};
  let requestIdCounter = 0;
  let onMessageCallback = null;
  let onStatusChange = null;
  let reconnectTimer = null;
  let connected = false;

  function connect(url, id, statusCb) {
    relayUrl = url;
    deviceId = id;
    onStatusChange = statusCb;
    _doConnect();
  }

  function _doConnect() {
    if (ws) {
      try { ws.close(); } catch (e) {}
    }

    const wsUrl = relayUrl.replace(/^http/, 'ws');
    ws = new WebSocket(`${wsUrl}?id=${encodeURIComponent(deviceId)}&role=client`);

    ws.onopen = () => {
      connected = true;
      if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null; }
      if (onStatusChange) onStatusChange(true);
      _flushPending();
    };

    ws.onmessage = (event) => {
      let msg;
      try { msg = JSON.parse(event.data); } catch (e) { return; }
      if (msg.requestId && pendingRequests[msg.requestId]) {
        if (pendingTimers[msg.requestId]) {
          clearTimeout(pendingTimers[msg.requestId]);
          delete pendingTimers[msg.requestId];
        }
        let payload = msg.payload;
        if (typeof payload === 'string') {
          try { payload = JSON.parse(payload); } catch (e) {}
        }
        pendingRequests[msg.requestId](payload, null);
        delete pendingRequests[msg.requestId];
      }
      if (onMessageCallback) onMessageCallback(msg);
    };

    ws.onclose = () => {
      connected = false;
      if (onStatusChange) onStatusChange(false);
      if (!reconnectTimer) {
        reconnectTimer = setTimeout(() => {
          reconnectTimer = null;
          _doConnect();
        }, 5000);
      }
    };

    ws.onerror = () => {};
  }

  function _flushPending() {
    Object.keys(pendingRequests).forEach(id => {
      const cb = pendingRequests[id];
      _sendDirect(id, { type: 'ping', payload: {} }, cb);
    });
  }

  function _send(msg, cb) {
    const requestId = 'r' + (++requestIdCounter);
    msg.requestId = requestId;
    if (cb) {
      pendingRequests[requestId] = cb;
      pendingTimers[requestId] = setTimeout(() => {
        if (pendingRequests[requestId]) {
          pendingRequests[requestId](null, new Error('Request timed out'));
          delete pendingRequests[requestId];
        }
        delete pendingTimers[requestId];
      }, 30000);
    }
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify(msg));
    } else {
      if (cb) cb(null, new Error('Not connected'));
    }
    return requestId;
  }

  function _sendDirect(requestId, msg, cb) {
    msg.requestId = requestId;
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify(msg));
    }
  }

  function send(type, payload, cb) {
    return _send({ target: deviceId, type, payload }, cb);
  }

  function onMessage(cb) {
    onMessageCallback = cb;
  }

  function isConnected() {
    return connected;
  }

  function disconnect() {
    if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null; }
    if (ws) { ws.close(); ws = null; }
    connected = false;
    if (onStatusChange) onStatusChange(false);
  }

  function getDeviceId() { return deviceId; }
  function getProxyUrl(path) {
    if (!relayUrl || !deviceId) return "";
    const base = relayUrl.replace(/^ws/, "http");
    const normalizedPath = path.startsWith("/") ? path.substring(1) : path;
    return `${base}/proxy/${encodeURIComponent(deviceId)}/${normalizedPath}`;
  }

  return { getProxyUrl, connect, send, onMessage, isConnected, disconnect, getDeviceId };
})();
