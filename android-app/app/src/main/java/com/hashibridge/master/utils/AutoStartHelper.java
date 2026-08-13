package com.hashibridge.master.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Helper to manage OEM-specific background task killer & Auto-Start settings.
 * Aggressive Android vendors (Xiaomi/MIUI/HyperOS, Oppo/ColorOS/Realme, Vivo/Funtouch,
 * Samsung/OneUI, Huawei/EMUI, Asus, Transsion/Infinix/Tecno) kill background services
 * unless explicitly allowed in their custom power/autostart managers.
 */
public class AutoStartHelper {

    private static final List<Intent> AUTO_START_INTENTS = Arrays.asList(
            // Xiaomi / Redmi / POCO (MIUI / HyperOS)
            new Intent().setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            new Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT),

            // Oppo / Realme (ColorOS / RealmeUI)
            new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
            new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")),
            new Intent().setComponent(new ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
            new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startupapp.StartupAppListActivity")),
            new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startupmanager.StartupAppListActivity")),

            // Vivo / iQOO (FuntouchOS / OriginOS)
            new Intent().setComponent(new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
            new Intent().setComponent(new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.PurviewTabActivity")),
            new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
            new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),

            // Samsung (OneUI / Smart Manager)
            new Intent().setComponent(new ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")),
            new Intent().setComponent(new ComponentName("com.samsung.android.sm", "com.samsung.android.sm.app.dashboard.SmartManagerDashBoardActivity")),
            new Intent().setComponent(new ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity")),

            // Huawei / Honor (EMUI / MagicUI)
            new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
            new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")),
            new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")),

            // Asus (ZenUI)
            new Intent().setComponent(new ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity")),
            new Intent().setComponent(new ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.entry.FunctionActivity")),

            // Transsion (Infinix, Tecno, itel - HiOS / XOS)
            new Intent().setComponent(new ComponentName("com.transsion.phonemanager", "com.transsion.phonemanager.settings.autostart.AutoStartSettingsActivity")),
            new Intent().setComponent(new ComponentName("com.transsion.phonemanager", "com.transsion.phonemanager.view.AutoStartListActivity")),

            // Letv
            new Intent().setComponent(new ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")),

            // HTC
            new Intent().setComponent(new ComponentName("com.htc.pitroad", "com.htc.pitroad.landingpage.activity.LandingPageActivity")),

            // Generic / Fallback Application Details Settings
            new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + "com.hashibridge.master"))
    );

    /**
     * Check if the device belongs to a known aggressive OEM brand.
     */
    public static boolean isAggressiveOem() {
        String brand = Build.MANUFACTURER.toLowerCase(Locale.ROOT);
        return brand.contains("xiaomi")
                || brand.contains("redmi")
                || brand.contains("poco")
                || brand.contains("oppo")
                || brand.contains("realme")
                || brand.contains("oneplus")
                || brand.contains("vivo")
                || brand.contains("iqoo")
                || brand.contains("huawei")
                || brand.contains("honor")
                || brand.contains("samsung")
                || brand.contains("asus")
                || brand.contains("transsion")
                || brand.contains("infinix")
                || brand.contains("tecno");
    }

    /**
     * Attempts to open the OEM AutoStart / Power Management settings screen.
     * Returns true if an OEM-specific activity was successfully launched.
     */
    public static boolean requestAutoStartPermission(Context context) {
        PackageManager pm = context.getPackageManager();
        for (Intent intent : AUTO_START_INTENTS) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                if (intent.resolveActivity(pm) != null) {
                    context.startActivity(intent);
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }
}
