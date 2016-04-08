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

    private NotificationCenter() {
        throw new AssertionError();
    }

    public static void sendNotification(int ID, Context context, Class passedClass, String title, String text, String buttonText){
        Intent intent = new Intent(context, passedClass);

        PendingIntent p = PendingIntent.getActivity(context, 0, intent, 0);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification noti = new Notification.Builder(context)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .addAction(R.mipmap.ic_launcher, buttonText, p)
                .setPriority(Notification.PRIORITY_MAX)
                .setVibrate(new long[]{0, 1000, 100, 1000, 100, 1000, 100})
                .setDefaults(Notification.DEFAULT_SOUND)
                //the code to auto close the notification after click -- begin//
                .setAutoCancel(true)
                .setContentIntent(p)
                //the code to auto close notification -- end//
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
