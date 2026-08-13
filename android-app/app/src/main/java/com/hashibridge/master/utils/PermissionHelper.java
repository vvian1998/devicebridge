package com.hashibridge.master.utils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class PermissionHelper {

    // ─── Lazy permission check ────────────────────────────────────────────────

    /**
     * Check if a single permission is granted.
     * Handlers call this BEFORE doing work. If false, they return a JSON error
     * with {"error":"permission_required","permission":"..."} so the relay can
     * surface a helpful message on the web UI.
     *
     * File storage on Android 11+ is handled separately via isExternalStorageManager().
     */
    public static boolean isGranted(Context ctx, String permission) {
        if (Manifest.permission.MANAGE_EXTERNAL_STORAGE.equals(permission)) {
            return isExternalStorageManager(ctx);
        }
        return ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if all permissions in a group are granted.
     */
    public static boolean areGranted(Context ctx, String... permissions) {
        for (String p : permissions) {
            if (!isGranted(ctx, p)) return false;
        }
        return true;
    }

    // ─── Permission groups by feature ────────────────────────────────────────

    /** Permissions required to read/write files (FileHandler) */
    public static String[] filePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // MANAGE_EXTERNAL_STORAGE — checked via Environment.isExternalStorageManager()
            return new String[]{Manifest.permission.MANAGE_EXTERNAL_STORAGE};
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
            };
        } else {
            return new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }
    }

    /** Permissions required to read SMS (SmsHandler) */
    public static String[] smsPermissions() {
        return new String[]{
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS
        };
    }

    /** Permissions required to access gallery / media (MediaHandler) */
    public static String[] galleryPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
            };
        } else {
            return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    public static boolean needsManageStorage() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
    }

    public static boolean isExternalStorageManager(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    // ─── Legacy bulk methods (used by ControlPanelActivity admin flow) ────────

    public static String[] getRequiredPermissions() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.INTERNET);
        perms.add(Manifest.permission.ACCESS_NETWORK_STATE);
        perms.add(Manifest.permission.ACCESS_WIFI_STATE);
        perms.add(Manifest.permission.FOREGROUND_SERVICE);
        perms.add(Manifest.permission.RECEIVE_BOOT_COMPLETED);
        perms.add(Manifest.permission.WAKE_LOCK);
        perms.add(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        perms.add(Manifest.permission.VIBRATE);
        perms.add(Manifest.permission.CAMERA);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            perms.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
        } else {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
            perms.add(Manifest.permission.READ_MEDIA_IMAGES);
            perms.add(Manifest.permission.READ_MEDIA_VIDEO);
        }

        perms.add(Manifest.permission.READ_CONTACTS);
        perms.add(Manifest.permission.READ_SMS);
        perms.add(Manifest.permission.SEND_SMS);
        perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        perms.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            perms.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }

        perms.add(Manifest.permission.QUERY_ALL_PACKAGES);
        perms.add(Manifest.permission.SYSTEM_ALERT_WINDOW);

        return perms.toArray(new String[0]);
    }

    public static String[] getRuntimePermissions() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.CAMERA);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
            perms.add(Manifest.permission.READ_MEDIA_IMAGES);
            perms.add(Manifest.permission.READ_MEDIA_VIDEO);
        }

        perms.add(Manifest.permission.READ_CONTACTS);
        perms.add(Manifest.permission.READ_SMS);
        perms.add(Manifest.permission.SEND_SMS);
        perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        perms.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        return perms.toArray(new String[0]);
    }

    public static boolean hasAllPermissions(Context context) {
        if (needsManageStorage() && !isExternalStorageManager(context)) {
            return false;
        }
        for (String perm : getRequiredPermissions()) {
            if (Manifest.permission.MANAGE_EXTERNAL_STORAGE.equals(perm)) continue;
            if (ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static List<String> getMissingPermissions(Context context) {
        List<String> missing = new ArrayList<>();
        for (String perm : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED) {
                missing.add(perm);
            }
        }
        return missing;
    }
}
