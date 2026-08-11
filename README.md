# DeviceBridge

Remote device control via WebSocket relay.

## Structure

| Component | Tech | Description |
|:----------|:-----|:------------|
| `android-app/` | Java / Android SDK 24-34 | APK with stealth service, HTTP server, relay client |
| `relay-server/` | Node.js / ws | WebSocket relay + static file server |
| `web-client/` | Vanilla JS / CSS | SPA dashboard for remote control |

## Quick Start

### 1. Relay Server

```bash
cd relay-server
npm install
node server.js
# Runs on port 3000
```

### 2. Android App

1. Open `android-app/` in Android Studio
2. Copy `web-client/` into `android-app/app/src/main/assets/web/`
3. Build APK → install on device
4. Enter relay URL + device ID → tap Start

### 3. Web Client

Access the web client via the relay server or open `web-client/index.html` directly (connects via WebSocket).

Pass `?relay=URL&id=DEVICE_ID` query params or enter them in the prompts.

## Features

- **Dashboard** — Battery, RAM, storage, CPU temp, device info
- **File Manager** — Browse, upload, download, delete, search
- **Gallery** — Photo/video browser with thumbnails
- **Terminal** — Remote shell access (xterm.js)
- **Contacts** — Read contacts list
- **Notifications** — Mirror device notifications
- **Location** — GPS coordinates + map
- **Camera** — Remote photo capture
- **App List** — Installed applications
- **Device Controls** — Vibrate, torch, screenshot, ring, clipboard

## Deploy Relay Server

[![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy?repo=https://github.com/your-org/devicebridge)

## Architecture

```
Device (Android) ──WebSocket──▶ Relay Server ◀──WebSocket── Web Client
                      │
                      └── HTTP :8080 (NanoHTTPD)
                            └── /api/* (JSON API)
                            └── Serves web-client static files
```

## Permissions

The app requests extensive permissions for full remote control. All handlers gracefully degrade when permissions are missing.
