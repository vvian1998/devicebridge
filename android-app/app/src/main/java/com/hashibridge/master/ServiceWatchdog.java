package com.hashibridge.master;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

/**
 * Watchdog that periodically checks if BridgeService is running.
 * If the service was killed by the OS battery optimizer or OEM task killer,
 * the watchdog restarts it automatically.
 *
 * Strategy:
 *  - Android 6+ (Doze era): setExactAndAllowWhileIdle so the alarm fires even
 *    in Doze windows, and we chain-reschedule on every fire.
 *  - Android <6: setExact is fine (Doze doesn't exist).
 *  - Interval: 10 min (600 s). Android 12+ may delay inexact alarms by up to
 *    10+ minutes, so we use exact alarms with explicit rescheduling instead of
 *    setInexactRepeating which would be useless on Doze devices.
 */
public class ServiceWatchdog extends BroadcastReceiver {

    private static final String ACTION_WATCHDOG = "com.hashibridge.master.WATCHDOG";
    /** How often the watchdog fires, in ms. */
    private static final long INTERVAL_MS = 10 * 60 * 1000L; // 10 minutes

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_WATCHDOG.equals(intent.getAction())) return;

        // Restart service if it is not running
        if (!BridgeService.isRunning()) {
            String relayUrl = com.hashibridge.master.utils.Config.getRelayUrl(context);
            if (relayUrl.isEmpty()) {
                relayUrl = context.getString(R.string.default_relay_url);
            }
            String deviceId = com.hashibridge.master.utils.Config.getOrCreateDeviceId(context);

            if (relayUrl.isEmpty() || deviceId.isEmpty()) {
                // Can't start — reschedule anyway
                schedule(context);
                return;
            }

            Intent service = new Intent(context, BridgeService.class);
            service.putExtra("relayUrl", relayUrl);
            service.putExtra("deviceId", deviceId);

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(service);
                } else {
                    context.startService(service);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Chain: reschedule next watchdog alarm so the cycle continues.
        // BridgeService.onCreate() also calls schedule(), but if the service
        // was already running we'd never reschedule without this.
        schedule(context);
    }

    /**
     * Schedule (or re-schedule) the watchdog alarm.
     * Safe to call multiple times — cancels existing alarm first.
     * Called from:
     *   - BridgeService.onCreate()
     *   - BootReceiver.onReceive()
     *   - ServiceWatchdog.onReceive()  (chain reschedule)
     */
    public static void schedule(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        PendingIntent pi = buildPendingIntent(context);

        long triggerAt = SystemClock.elapsedRealtime() + INTERVAL_MS;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // setExactAndAllowWhileIdle fires even during Doze mode.
            // Available since API 23 (Android 6.0).
            am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
        } else {
            am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
        }
    }

    /**
     * Cancel the watchdog alarm (e.g., when user explicitly stops the service
     * and does not want auto-restart).
     */
    public static void cancel(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        am.cancel(buildPendingIntent(context));
    }

    private static PendingIntent buildPendingIntent(Context context) {
        Intent intent = new Intent(context, ServiceWatchdog.class);
        intent.setAction(ACTION_WATCHDOG);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, 0, intent, flags);
    }
}
