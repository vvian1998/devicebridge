# 📁 DeviceBridge — Complete Project Tree

> Semua path relatif terhadap `/root/devicebridge/`
> Total: **48 files** | 3 komponen

---

## Full Tree

```
devicebridge/
│
│── PROJECT_TREE.md                                                          # File ini
│── README.md                                                                # Setup guide (Bahasa Indonesia)
│
├── .github/
│   └── workflows/
│       └── build-apk.yml                                                    # GitHub Actions: auto-build APK
│
├── android-app/
│   ├── build.gradle                                                         # Project-level Gradle config
│   ├── settings.gradle                                                      # Gradle settings (project name, modules)
│   ├── gradle.properties                                                    # JVM args, Android settings
│   ├── gradlew                                                              # Gradle wrapper script (Linux)
│   ├── gradlew.bat                                                          # Gradle wrapper script (Windows)
│   ├── gradle/
│   │   └── wrapper/
│   │       ├── gradle-wrapper.jar                                           # Gradle wrapper binary
│   │       └── gradle-wrapper.properties                                    # Gradle version config
│   │
│   └── app/
│       ├── build.gradle                                                     # App-level Gradle (dependencies, SDK)
│       ├── proguard-rules.pro                                               # ProGuard rules for release build
│       │
│       └── src/main/
│           ├── AndroidManifest.xml                                          # App manifest (permissions, components)
│           │
│           ├── java/com/devicebridge/
│           │   │
│           │   ├── MainActivity.java                                        # Main UI: pairing code, permissions, status
│           │   ├── BridgeService.java                                       # Stealth service (zero notif, wakelock ON)
│           │   ├── LocalServer.java                                         # NanoHTTPD HTTP server (:8080)
│           │   ├── RelayClient.java                                         # WebSocket client → relay server
│           │   ├── RequestRouter.java                                       # Maps API actions → handlers
│           │   ├── NotificationListener.java                                # NotificationListenerService impl
│           │   │
│           │   ├── handlers/
│           │   │   ├── SystemHandler.java                                   # Battery, RAM, CPU, storage, device info
│           │   │   ├── FileHandler.java                                     # List dirs, download files, delete
│           │   │   ├── MediaHandler.java                                    # Gallery listing, thumbnails (MediaStore)
│           │   │   ├── ContactHandler.java                                  # Read contacts list
│           │   │   ├── TerminalHandler.java                                 # Execute shell commands
│           │   │   ├── ScreenshotHandler.java                               # Capture screen (MediaProjection)
│           │   │   ├── NotificationHandler.java                             # Read active notifications
│           │   │   ├── ClipboardHandler.java                                # Read clipboard
│           │   │   ├── LocationHandler.java                                 # GPS coordinates
│           │   │   ├── CameraHandler.java                                   # Remote camera capture
│           │   │   ├── AppListHandler.java                                  # Installed apps list
│           │   │   └── DeviceControlHandler.java                            # Vibrate, flashlight, ring
│           │   │
│           │   └── utils/
│           │       ├── PermissionHelper.java                                # Runtime permission manager (API 24-34)
│           │       ├── ThumbnailCache.java                                  # 3-tier thumbnail cache (mem/disk/OS)
│           │       └── JsonHelper.java                                      # JSON response builder utilities
│           │
│           ├── res/
│           │   ├── layout/
│           │   │   └── activity_main.xml                                    # MainActivity layout XML
│           │   ├── values/
│           │   │   ├── strings.xml                                          # String resources
│           │   │   ├── colors.xml                                           # Color definitions
│           │   │   └── themes.xml                                           # App theme (dark)
│           │   ├── drawable/
│           │   │   ├── ic_transparent.xml                                   # 1x1 transparent icon (stealth notif)
│           │   │   └── ic_launcher_foreground.xml                           # App icon foreground
│           │   └── mipmap-xxxhdpi/
│           │       ├── ic_launcher.webp                                     # App launcher icon
│           │       └── ic_launcher_round.webp                               # Round launcher icon
│           │
│           └── assets/
│               └── web/                                                     # ← web-client/ copied here during build
│                   └── (auto-generated from web-client/)
│
├── relay-server/
│   ├── package.json                                                         # Node.js dependencies (ws, express)
│   ├── server.js                                                            # WebSocket relay + static file server
│   └── render.yaml                                                          # Render.com one-click deploy config
│
└── web-client/
    ├── index.html                                                           # SPA shell (single page app entry)
    ├── css/
    │   └── style.css                                                        # Complete design system (dark, glass)
    └── js/
        ├── app.js                                                           # Router, state, connection manager
        ├── api.js                                                           # API abstraction (HTTP + WebSocket)
        └── components/
            ├── sidebar.js                                                   # Navigation sidebar + bottom tab
            ├── dashboard.js                                                 # System overview (battery, RAM, storage)
            ├── filemanager.js                                               # File browser (list, download, preview)
            ├── gallery.js                                                   # Photo/video gallery (grid, lightbox)
            ├── terminal.js                                                  # xterm.js remote terminal
            ├── contacts.js                                                  # Contact list viewer
            ├── notifications.js                                             # Notification mirror (read-only)
            ├── location.js                                                  # Device location map
            ├── camera.js                                                    # Remote camera capture
            ├── applist.js                                                   # Installed apps list
            └── controls.js                                                  # Device controls (vibrate, torch, ring)
```

---

## File Count Per Component

| Component | Files | Lines (est.) |
|:----------|:------|:-------------|
| **Android APK** | 28 files | ~3500 lines |
| **Relay Server** | 3 files | ~200 lines |
| **Web Client** | 16 files | ~2500 lines |
| **CI/CD + Docs** | 3 files | ~150 lines |
| **Total** | **48 files** | **~6350 lines** |

---

## Absolute Paths — Android APK

| # | File | Full Path |
|:--|:-----|:----------|
| 1 | `build.gradle` (project) | `/root/devicebridge/android-app/build.gradle` |
| 2 | `settings.gradle` | `/root/devicebridge/android-app/settings.gradle` |
| 3 | `gradle.properties` | `/root/devicebridge/android-app/gradle.properties` |
| 4 | `gradlew` | `/root/devicebridge/android-app/gradlew` |
| 5 | `gradlew.bat` | `/root/devicebridge/android-app/gradlew.bat` |
| 6 | `gradle-wrapper.jar` | `/root/devicebridge/android-app/gradle/wrapper/gradle-wrapper.jar` |
| 7 | `gradle-wrapper.properties` | `/root/devicebridge/android-app/gradle/wrapper/gradle-wrapper.properties` |
| 8 | `build.gradle` (app) | `/root/devicebridge/android-app/app/build.gradle` |
| 9 | `proguard-rules.pro` | `/root/devicebridge/android-app/app/proguard-rules.pro` |
| 10 | `AndroidManifest.xml` | `/root/devicebridge/android-app/app/src/main/AndroidManifest.xml` |
| 11 | `MainActivity.java` | `/root/devicebridge/android-app/app/src/main/java/com/devicebridge/MainActivity.java` |
| 12 | `BridgeService.java` | `/root/devicebridge/android-app/app/src/main/java/com/devicebridge/BridgeService.java` |
| 13 | `LocalServer.java` | `/root/devicebridge/android-app/app/src/main/java/com/devicebridge/LocalServer.java` |
| 14 | `RelayClient.java` | `/root/devicebridge/android-app/app/src/main/java/com/devicebridge/RelayClient.java` |
| 15 | `RequestRouter.java` | `/root/devicebridge/android-app/app/src/main/java/com/devicebridge/RequestRouter.java` |
| 16 | `NotificationListener.java` | `/root/devicebridge/android-app/app/src/main/java/com/devicebridge/NotificationListener.java` |
| 17 | `SystemHandler.java` | `/root/devicebridge/android-app/app/src/main/java/com/devicebridge/handlers/SystemHandler.java` |
| 18 | `FileHandler.java` | `/root/devicebridge/android-app/app/src/main/java/com/devicebridge/handlers/FileHandler.java` |
| 19 | `MediaHandler.java` | `/root/devicebridge/android-app/app/src/main/java/com/devicebridge/handlers/MediaHandler.java` |
| 20 | `ContactHandler.java` | `/root/devicebridge/android-app/app/src/main/java/com/devicebridge/handlers/ContactHandler.java` |
| 21 | `TerminalHandler.java` | `/root/devicebridge/android-app/app/src/main/java/com/devicebridge/handlers/TerminalHandler.java` |
| 22 | `ScreenshotHandler.java` | `/root/devicebridge/android-app/app/src/main/java/com/devicebridge/handlers/ScreenshotHandler.java` |
| 23 | `NotificationHandler.java` | `/root/devicebridge/android-app/app/src/main/java/com/devicebridge/handlers/NotificationHandler.java` |
| 24 | `ClipboardHandler.java` | `/root/devicebridge/android-app/app/src/main/java/com/devicebridge/handlers/ClipboardHandler.java` |
| 25 | `LocationHandler.java` | `/root/devicebridge/android-app/app/src/main/java/com/devicebridge/handlers/LocationHandler.java` |
| 26 | `CameraHandler.java` | `/root/devicebridge/android-app/app/src/main/java/com/devicebridge/handlers/CameraHandler.java` |
| 27 | `AppListHandler.java` | `/root/devicebridge/android-app/app/src/main/java/com/devicebridge/handlers/AppListHandler.java` |
| 28 | `DeviceControlHandler.java` | `/root/devicebridge/android-app/app/src/main/java/com/devicebridge/handlers/DeviceControlHandler.java` |
| 29 | `PermissionHelper.java` | `/root/devicebridge/android-app/app/src/main/java/com/devicebridge/utils/PermissionHelper.java` |
| 30 | `ThumbnailCache.java` | `/root/devicebridge/android-app/app/src/main/java/com/devicebridge/utils/ThumbnailCache.java` |
| 31 | `JsonHelper.java` | `/root/devicebridge/android-app/app/src/main/java/com/devicebridge/utils/JsonHelper.java` |
| 32 | `activity_main.xml` | `/root/devicebridge/android-app/app/src/main/res/layout/activity_main.xml` |
| 33 | `strings.xml` | `/root/devicebridge/android-app/app/src/main/res/values/strings.xml` |
| 34 | `colors.xml` | `/root/devicebridge/android-app/app/src/main/res/values/colors.xml` |
| 35 | `themes.xml` | `/root/devicebridge/android-app/app/src/main/res/values/themes.xml` |
| 36 | `ic_transparent.xml` | `/root/devicebridge/android-app/app/src/main/res/drawable/ic_transparent.xml` |
| 37 | `ic_launcher_foreground.xml` | `/root/devicebridge/android-app/app/src/main/res/drawable/ic_launcher_foreground.xml` |

---

## Absolute Paths — Relay Server

| # | File | Full Path |
|:--|:-----|:----------|
| 38 | `package.json` | `/root/devicebridge/relay-server/package.json` |
| 39 | `server.js` | `/root/devicebridge/relay-server/server.js` |
| 40 | `render.yaml` | `/root/devicebridge/relay-server/render.yaml` |

---

## Absolute Paths — Web Client

| # | File | Full Path |
|:--|:-----|:----------|
| 41 | `index.html` | `/root/devicebridge/web-client/index.html` |
| 42 | `style.css` | `/root/devicebridge/web-client/css/style.css` |
| 43 | `app.js` | `/root/devicebridge/web-client/js/app.js` |
| 44 | `api.js` | `/root/devicebridge/web-client/js/api.js` |
| 45 | `sidebar.js` | `/root/devicebridge/web-client/js/components/sidebar.js` |
| 46 | `dashboard.js` | `/root/devicebridge/web-client/js/components/dashboard.js` |
| 47 | `filemanager.js` | `/root/devicebridge/web-client/js/components/filemanager.js` |
| 48 | `gallery.js` | `/root/devicebridge/web-client/js/components/gallery.js` |
| 49 | `terminal.js` | `/root/devicebridge/web-client/js/components/terminal.js` |
| 50 | `contacts.js` | `/root/devicebridge/web-client/js/components/contacts.js` |
| 51 | `notifications.js` | `/root/devicebridge/web-client/js/components/notifications.js` |
| 52 | `location.js` | `/root/devicebridge/web-client/js/components/location.js` |
| 53 | `camera.js` | `/root/devicebridge/web-client/js/components/camera.js` |
| 54 | `applist.js` | `/root/devicebridge/web-client/js/components/applist.js` |
| 55 | `controls.js` | `/root/devicebridge/web-client/js/components/controls.js` |

---

## Absolute Paths — CI/CD & Docs

| # | File | Full Path |
|:--|:-----|:----------|
| 56 | `build-apk.yml` | `/root/devicebridge/.github/workflows/build-apk.yml` |
| 57 | `README.md` | `/root/devicebridge/README.md` |
| 58 | `PROJECT_TREE.md` | `/root/devicebridge/PROJECT_TREE.md` |
