package com.hashibridge.master.handlers;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import android.app.ActivityManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import com.google.gson.JsonObject;
import com.hashibridge.master.utils.JsonHelper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class SystemHandler {

    private final Context context;

    public SystemHandler(Context context) {
        this.context = context;
    }

    public String handle(String action, JsonObject payload) {
        switch (action) {
            case "info":
                return getSystemInfo();
            default:
                return JsonHelper.error("Unknown system action: " + action);
        }
    }

    private String getSystemInfo() {
        JsonObject info = new JsonObject();

        info.addProperty("manufacturer", Build.MANUFACTURER);
        info.addProperty("model", Build.MODEL);
        info.addProperty("brand", Build.BRAND);
        info.addProperty("androidVersion", Build.VERSION.RELEASE);
        info.addProperty("sdk", Build.VERSION.SDK_INT);

        info.addProperty("uptime", SystemClock.elapsedRealtime());

        try {
            BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            if (bm != null) {
                info.addProperty("battery", bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY));
                int status = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS);
                info.addProperty("batteryCharging",
                        status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL);
            }
        } catch (Exception e) {
            info.addProperty("battery", 0);
        }

        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(memInfo);
                long totalMem = getTotalRam();
                info.addProperty("ramTotal", totalMem);
                info.addProperty("ramUsed", totalMem - memInfo.availMem);
                info.addProperty("ramPct", totalMem > 0 ? (int)((totalMem - memInfo.availMem) * 100 / totalMem) : 0);
            }
        } catch (Exception ignored) {}

        try {
            StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
            long totalBytes = statFs.getTotalBytes();
            long availableBytes = statFs.getAvailableBytes();
            info.addProperty("storageTotal", totalBytes);
            info.addProperty("storageUsed", totalBytes - availableBytes);
            info.addProperty("storagePct", totalBytes > 0 ? (int)((totalBytes - availableBytes) * 100 / totalBytes) : 0);
        } catch (Exception ignored) {}

        try {
            String temp = readCpuTemp();
            info.addProperty("cpuTemp", temp);
        } catch (Exception e) {
            info.addProperty("cpuTemp", "N/A");
        }

        info.addProperty("cpuModel", getCpuModel());

        try {
            int w = context.getResources().getDisplayMetrics().widthPixels;
            int h = context.getResources().getDisplayMetrics().heightPixels;
            int dpi = context.getResources().getDisplayMetrics().densityDpi;
            info.addProperty("screenWidth", w);
            info.addProperty("screenHeight", h);
            info.addProperty("density", dpi);
        } catch (Exception ignored) {}

        try {
            WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                WifiInfo wi = wm.getConnectionInfo();
                int ip = wi.getIpAddress();
                String ipStr = (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." +
                        ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
                info.addProperty("ipAddress", ipStr);
            }
        } catch (Exception e) {
            info.addProperty("ipAddress", "Unknown");
        }

        info.addProperty("isRooted", checkRoot());

        return JsonHelper.success(info);
    }

    private long getTotalRam() {
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/meminfo"));
            String line = br.readLine();
            br.close();
            if (line != null) {
                String[] parts = line.split("\\s+");
                return Long.parseLong(parts[1]) * 1024;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private String readCpuTemp() {
        String[] paths = {
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/devices/virtual/thermal/thermal_zone0/temp"
        };
        for (String path : paths) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(path));
                String line = br.readLine();
                br.close();
                if (line != null && !line.isEmpty()) {
                    long temp = Long.parseLong(line.trim());
                    return temp > 1000 ? String.valueOf(temp / 1000.0) : String.valueOf(temp);
                }
            } catch (SecurityException se) {
                // Ignore SecurityException (SELinux blocks this on Android 13+)
                continue;
            } catch (Exception ignored) {
                // Ignore other read errors
            }
        }
        return "N/A";
    }

    private String getCpuModel() {
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("Hardware")) {
                    br.close();
                    return line.split(":")[1].trim();
                }
            }
            br.close();
        } catch (Exception ignored) {}
        return "Unknown";
    }

    private boolean checkRoot() {
        String[] paths = {"/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
                "/system/xbin/su", "/data/local/xbin/su", "/system/su"};
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }
}
