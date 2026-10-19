package com.example.glowring;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class NotifService extends NotificationListenerService {
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;
        if (getPackageName().equals(sbn.getPackageName())) return;
        if (sbn.isOngoing()) return;
        Notification n = sbn.getNotification();
        if (n != null && (n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;
        OverlayService.trigger(this);
    }
}
