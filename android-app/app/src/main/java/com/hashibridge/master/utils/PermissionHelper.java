package com.hashibridge.master.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class PermissionHelper {

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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

        return perms.toArray(new String[0]);
    }

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
