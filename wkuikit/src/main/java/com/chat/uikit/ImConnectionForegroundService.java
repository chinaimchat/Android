package com.chat.uikit;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.chat.base.config.WKConfig;
import com.chat.base.config.WKConstants;

/**
 * 使用 {@link ServiceInfo#FOREGROUND_SERVICE_TYPE_DATA_SYNC}：与服务器同步消息，符合「实时消息/数据同步」场景。
 */
public class ImConnectionForegroundService extends Service {

    static final int NOTIFICATION_ID = 0x7e01;

    @Override
    public void onCreate() {
        super.onCreate();
        ensureChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (TextUtils.isEmpty(WKConfig.getInstance().getToken())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        WKUIKitApplication.getInstance().initIM();
        WKUIKitApplication.getInstance().startChat();
        return START_STICKY;
    }

    @Nullable
    @Override
    public android.os.IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        super.onDestroy();
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    WKConstants.imConnectionFgsChannelId,
                    getString(R.string.im_fgs_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            ch.setDescription(getString(R.string.im_fgs_channel_desc));
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(ch);
            }
        }
    }

    private Notification buildNotification() {
        Intent relay = new Intent(this, NotifyRelayActivity.class);
        relay.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, relay,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        int icon = getApplicationInfo().icon;
        if (icon == 0) {
            icon = android.R.drawable.stat_notify_sync;
        }
        return new NotificationCompat.Builder(this, WKConstants.imConnectionFgsChannelId)
                .setContentTitle(getString(R.string.im_fgs_notification_title))
                .setContentText(getString(R.string.im_fgs_notification_text))
                .setSmallIcon(icon)
                .setOngoing(true)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }
}
