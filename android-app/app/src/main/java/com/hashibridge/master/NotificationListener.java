package com.hashibridge.master;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.util.ArrayList;
import java.util.List;

public class NotificationListener extends NotificationListenerService {

    private static final List<StatusBarNotification> activeNotifications = new ArrayList<>();

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        synchronized (activeNotifications) {
            activeNotifications.removeIf(n -> n.getKey().equals(sbn.getKey()));
            activeNotifications.add(0, sbn);
            if (activeNotifications.size() > 100) {
                activeNotifications.remove(activeNotifications.size() - 1);
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        synchronized (activeNotifications) {
            activeNotifications.removeIf(n -> n.getKey().equals(sbn.getKey()));
        }
    }

    public static List<StatusBarNotification> getCachedNotifications() {
        synchronized (activeNotifications) {
            return new ArrayList<>(activeNotifications);
        }
    }
}
