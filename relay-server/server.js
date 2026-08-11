const express = require('express');
const http = require('http');
const { WebSocketServer } = require('ws');
const path = require('path');
const crypto = require('crypto');

const PORT = process.env.PORT || 3000;
const WS_PORT = process.env.WS_PORT || PORT;

// Password for web client access. Set WEB_PASSWORD env var. Disabled if empty.
const WEB_PASSWORD = process.env.WEB_PASSWORD || '';

const app = express();
app.use(express.json());

function authRequired(req, res, next) {
  if (!WEB_PASSWORD) return next();
  const token = req.headers['x-auth-token'];
  const provided = req.query.token;
  const ok = (token && token === WEB_PASSWORD) || (provided && provided === WEB_PASSWORD);
  if (!ok) return res.status(401).json({ error: 'unauthorized' });
  return next();
}

// Static assets are public; the app JS itself is harmless. Device data stays gated behind auth.
app.use(express.static(path.join(__dirname, 'public')));

app.get('/health', (_req, res) => res.json({ status: 'ok' }));

app.post('/api/login', (req, res) => {
  if (!WEB_PASSWORD) {
    return res.json({ ok: true, token: '' });
  }
  const { password } = req.body || {};
  if (password === WEB_PASSWORD) {
    return res.json({ ok: true, token: WEB_PASSWORD });
  }
  return res.status(401).json({ ok: false, error: 'invalid password' });
});

app.get('/api/devices', authRequired, (_req, res) => {
  const online = [];
  for (const [key, ws] of clients.entries()) {
    if (key.endsWith(':device') && ws.readyState === 1) {
      online.push({ deviceId: key.replace(/:device$/, '') });
    }
  }
  online.sort((a, b) => a.deviceId.localeCompare(b.deviceId));
  res.json({ devices: online });
});

const server = http.createServer(app);

const wss = new WebSocketServer({ server });

const clients = new Map();

// In-flight binary proxy requests: requestId -> { res, timer, headersSent }
const pendingProxy = new Map();
let proxySeq = 0;

function getQueryString(req) {
  const idx = req.url.indexOf('?');
  return idx >= 0 ? req.url.substring(idx) : '';
}

// Forward only headers that matter for local streaming (Range for seeking, etc.)
const FORWARD_HEADERS = ['range', 'if-range', 'accept', 'accept-encoding', 'user-agent', 'referer'];

function setProxyResponseHeaders(res, headers) {
  if (!headers || typeof headers !== 'object') return;
  for (const [k, v] of Object.entries(headers)) {
    const lk = String(k).toLowerCase();
    if (lk === 'transfer-encoding' || lk === 'connection' || lk === 'keep-alive' || lk === 'date') continue;
    try { res.setHeader(k, v); } catch (e) {}
  }
}

function handleProxyBinary(buf) {
  const sep = buf.indexOf(0);
  if (sep <= 0) return;

  let header;
  try { header = JSON.parse(buf.slice(0, sep).toString('utf8')); } catch (e) { return; }
  if (!header.requestId) return;

  const pending = pendingProxy.get(header.requestId);
  if (!pending) return;

  const chunk = buf.slice(sep + 1);

  if (header.isEof) {
    clearTimeout(pending.timer);
    pendingProxy.delete(header.requestId);
    if (!pending.headersSent && header.status) {
      pending.res.status(header.status || 500);
      setProxyResponseHeaders(pending.res, header.proxyHeaders);
    }
    try { pending.res.end(chunk.length > 0 ? chunk : undefined); } catch (e) {}
    return;
  }

  if (!pending.headersSent) {
    pending.headersSent = true;
    pending.res.status(header.status || 200);
    setProxyResponseHeaders(pending.res, header.proxyHeaders);
  }
  try { pending.res.write(chunk); } catch (e) {}
}

// Streaming proxy: browser hits this, we push it to the device over WS binary,
// and pipe the device's binary chunks back into the HTTP response.
app.all('/proxy/:deviceId/*', (req, res) => {
  const deviceId = req.params.deviceId;
  const deviceWs = clients.get(deviceId + ':device');

  if (!deviceWs || deviceWs.readyState !== 1) {
    return res.status(502).json({ error: 'device offline' });
  }

  const path = '/' + req.params[0] + getQueryString(req);
  const requestId = 'proxy_' + (++proxySeq);

  const headers = {};
  for (const h of FORWARD_HEADERS) {
    if (req.headers[h]) headers[h] = req.headers[h];
  }

  const pending = {
    res,
    timer: null,
    headersSent: false
  };
  pending.timer = setTimeout(() => {
    if (pendingProxy.delete(requestId) && !res.headersSent) {
      try { res.status(504).json({ error: 'proxy timeout' }); } catch (e) {}
    }
  }, 60000);
  pendingProxy.set(requestId, pending);

  res.on('close', () => {
    if (pendingProxy.delete(requestId)) clearTimeout(pending.timer);
  });

  deviceWs.send(JSON.stringify({
    target: deviceId,
    type: 'proxy',
    requestId,
    payload: { method: req.method, path, headers }
  }));
});

// Heartbeat: detect and clean up stale connections
const HEARTBEAT_INTERVAL = 30000;
const heartbeat = setInterval(() => {
  wss.clients.forEach((ws) => {
    if (ws.isAlive === false) {
      console.log(`[x] Stale connection removed: ${ws.deviceId}:${ws.role}`);
      clients.delete(ws.deviceId + ':' + ws.role);
      return ws.terminate();
    }
    ws.isAlive = false;
    ws.ping();
  });
}, HEARTBEAT_INTERVAL);

wss.on('close', () => clearInterval(heartbeat));

wss.on('connection', (ws, req) => {
  const deviceId = new URL(req.url, `http://${req.headers.host}`).searchParams.get('id') || 'unknown';
  const role = new URL(req.url, `http://${req.headers.host}`).searchParams.get('role') || 'device';

  ws.deviceId = deviceId;
  ws.role = role;
  ws.isAlive = true;

  ws.on('pong', () => { ws.isAlive = true; });

  // Close old socket with same deviceId+role to prevent orphans
  const key = deviceId + ':' + role;
  const existing = clients.get(key);
  if (existing && existing !== ws && existing.readyState === 1) {
    console.log(`[!] Closing orphaned ${role} socket for ${deviceId}`);
    existing.close(1000, 'Replaced by new connection');
  }
  clients.set(key, ws);
  console.log(`[+] ${role} connected: ${deviceId} (total: ${clients.size})`);

  ws.on('message', (data, isBinary) => {
    if (isBinary) {
      handleProxyBinary(data);
      return;
    }

    let msg;
    try { msg = JSON.parse(data.toString()); } catch (e) { return; }

    if (msg.target) {
      const targetWs = clients.get(msg.target + ':' + (role === 'device' ? 'client' : 'device'));
      if (targetWs && targetWs.readyState === 1) {
        targetWs.send(JSON.stringify({
          from: deviceId,
          type: msg.type,
          payload: msg.payload,
          requestId: msg.requestId
        }));
      }
    } else if (msg.broadcast) {
      // Only broadcast to clients with the SAME deviceId (not all clients)
      wss.clients.forEach((c) => {
        if (c !== ws && c.readyState === 1 && c.deviceId === deviceId) {
          c.send(JSON.stringify({
            from: deviceId,
            type: msg.type,
            payload: msg.payload,
            requestId: msg.requestId
          }));
        }
      });
    }
  });

  ws.on('close', () => {
    clients.delete(deviceId + ':' + role);
    console.log(`[-] ${role} disconnected: ${deviceId} (total: ${clients.size})`);
  });

  ws.on('error', (err) => {
    console.error(`[!] ${role} error (${deviceId}):`, err.message);
  });

  ws.send(JSON.stringify({ type: 'connected', payload: { deviceId, role } }));
});

server.listen(PORT, () => {
  console.log(`DeviceBridge Relay running on port ${PORT}`);
  console.log(`WebSocket available at ws://0.0.0.0:${PORT}`);
});
