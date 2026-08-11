package com.devicebridge.handlers;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.devicebridge.utils.JsonHelper;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;

public class AppListHandler {

    private final Context context;

    public AppListHandler(Context context) {
        this.context = context;
    }

    public String handle(String action, JsonObject payload) {
        switch (action) {
            case "list": return listApps(null);
            case "search": return listApps(payload.has("query") ? payload.get("query").getAsString() : "");
            default: return JsonHelper.error("Unknown applist action: " + action);
        }
    }

    private String listApps(String query) {
        JsonArray apps = new JsonArray();
        PackageManager pm = context.getPackageManager();

        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo appInfo : packages) {
            try {
                String appName = pm.getApplicationLabel(appInfo).toString();
                String packageName = appInfo.packageName;

                if (query != null && !query.isEmpty()) {
                    String q = query.toLowerCase();
                    if (!appName.toLowerCase().contains(q) && !packageName.toLowerCase().contains(q)) {
                        continue;
                    }
                }

                JsonObject app = new JsonObject();
                app.addProperty("name", appName);
                app.addProperty("packageName", packageName);

                try {
                    PackageInfo pInfo = pm.getPackageInfo(packageName, 0);
                    app.addProperty("version", pInfo.versionName);
                } catch (Exception ignored) {}

                app.addProperty("isSystem", (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);

                try {
                    Drawable icon = pm.getApplicationIcon(appInfo);
                    if (icon != null) {
                        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(
                                icon.getIntrinsicWidth(), icon.getIntrinsicHeight(),
                                android.graphics.Bitmap.Config.ARGB_8888);
                        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
                        icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                        icon.draw(canvas);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 80, baos);
                        app.addProperty("icon", "data:image/png;base64," +
                                Base64.getEncoder().encodeToString(baos.toByteArray()));
                        bitmap.recycle();
                    }
                } catch (Exception ignored) {}

                apps.add(app);
            } catch (Exception ignored) {}
        }

        JsonObject result = new JsonObject();
        result.add("apps", apps);
        return JsonHelper.success(result);
    }
}
