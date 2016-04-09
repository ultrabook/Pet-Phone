package com.air.petphone;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

/**
 * Created by Randy on 16-03-20.
 */
public final class NotificationCenter {

    public static final String NOTIFICATION_EXTRA = "notification_extra";
    private NotificationCenter() {
        throw new AssertionError();
    }

    public static void sendNotification(int ID, Context context, Class passedClass, String title, String text, String buttonText){
        Intent intent = new Intent(context, passedClass);
        intent.putExtra(NOTIFICATION_EXTRA, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent p = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification noti = new Notification.Builder(context)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .addAction(R.mipmap.ic_launcher, buttonText, p)
                .setPriority(Notification.PRIORITY_MAX)
                .setVibrate(new long[]{0, 1000, 100, 1000, 100, 1000, 100})
                .setDefaults(Notification.DEFAULT_SOUND)
                .build();
        notificationManager.notify(ID, noti);

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        boolean isScreenOn = pm.isInteractive();

        if (!isScreenOn) {
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE | PowerManager.ACQUIRE_CAUSES_WAKEUP, "example");
            wl.acquire(10000);

        }
    }

}
