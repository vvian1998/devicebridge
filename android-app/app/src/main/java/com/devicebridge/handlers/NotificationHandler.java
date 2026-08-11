package com.devicebridge.handlers;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.service.notification.StatusBarNotification;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.devicebridge.NotificationListener;
import com.devicebridge.utils.JsonHelper;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationHandler {

    private final Context context;

    public NotificationHandler(Context context) {
        this.context = context;
    }

    public String handle(String action, JsonObject payload) {
        switch (action) {
            case "list": return listNotifications();
            default: return JsonHelper.error("Unknown notification action: " + action);
        }
    }

    private String listNotifications() {
        JsonArray notifications = new JsonArray();

        try {
            List<StatusBarNotification> active = NotificationListener.getCachedNotifications();
            PackageManager pm = context.getPackageManager();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

            for (StatusBarNotification sbn : active) {
                JsonObject notif = new JsonObject();

                String packageName = sbn.getPackageName();
                notif.addProperty("packageName", packageName);

                try {
                    ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                    notif.addProperty("appName", pm.getApplicationLabel(appInfo).toString());

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
                        notif.addProperty("icon", "data:image/png;base64," +
                                Base64.getEncoder().encodeToString(baos.toByteArray()));
                        bitmap.recycle();
                    }
                } catch (Exception e) {
                    notif.addProperty("appName", packageName);
                }

                if (sbn.getNotification().extras != null) {
                    CharSequence title = sbn.getNotification().extras.getCharSequence("android.title");
                    CharSequence text = sbn.getNotification().extras.getCharSequence("android.text");
                    notif.addProperty("title", title != null ? title.toString() : "");
                    notif.addProperty("text", text != null ? text.toString() : "");
                }

                notif.addProperty("time", sdf.format(new Date(sbn.getPostTime())));
                notif.addProperty("key", sbn.getKey());

                notifications.add(notif);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        JsonObject result = new JsonObject();
        result.add("notifications", notifications);
        return JsonHelper.success(result);
    }
}
