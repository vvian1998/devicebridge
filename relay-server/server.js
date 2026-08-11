const express = require('express');
const http = require('http');
const { WebSocketServer } = require('ws');
const path = require('path');

const PORT = process.env.PORT || 3000;
const WS_PORT = process.env.WS_PORT || PORT;

const app = express();

app.use(express.static(path.join(__dirname, 'public')));

app.get('/health', (_req, res) => res.json({ status: 'ok' }));

const server = http.createServer(app);

const wss = new WebSocketServer({ server });

const clients = new Map();

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

  ws.on('message', (data) => {
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
