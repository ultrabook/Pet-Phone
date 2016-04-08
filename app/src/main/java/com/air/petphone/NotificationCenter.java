package com.air.petphone;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.PowerManager;
import android.util.DebugUtils;
import android.view.View;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Randy on 16-03-20.
 */

//Need to extend intent, but cant due it it being conflicting with current architecture.
public final class NotificationCenter {
    //public static String action = "";
   // public static int actionID;
    private NotificationCenter() {
        throw new AssertionError();
    }

    //Added typeN to add the type of notificaiton to be used for logging the button presses
    public static void sendNotification(int ID, Context context, Class passedClass, String title, String text, String buttonText, String typeN){
        //action = typeN;
        //actionID = ID;
        //Intent intent = new Intent(context, passedClass).setAction(action);
        Intent intent = new Intent(context, passedClass);
        PendingIntent p = PendingIntent.getActivity(context, 0, intent, 0);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification noti = new Notification.Builder(context)
                //.setAutoCancel(true)
               // .setContentIntent(PendingIntent.getActivity(context, 0, intent, 0))
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
//
//    public static class NotificationActionService extends IntentService {
//        public NotificationActionService() {
//            super(NotificationActionService.class.getSimpleName());
//        }
//
//        @Override
//        protected void onHandleIntent(Intent intent) {
//            String action = intent.getAction();
//
//            //Log the different cases
//            switch (action) {
//                case "frequent_unlock":
//                    LogNotButton("Unlocked-Too-Frequently");
//
//                    break;
//
//                case "power_connected":
//                    LogNotButton("Power-Was-Connected");
//
//                    break;
//
//                case "power_disconnected":
//                    LogNotButton("Power-Was-Disconnected");
//
//                    break;
//
//                case "drop_sorry":
//                    LogNotButton("Apologized-For-Sorry");
//
//                    break;
//
//                case "CPU_Overworked":
//                    LogNotButton("Apologized for CPU working hard");
//
//                    break;
//
//                case "hungry":
//                    LogNotButton("Battery below 50 NOT CHARGING - apology");
//
//                    break;
//
//                case "feeding":
//                    LogNotButton("Battery below 50 but CHARGING");
//
//                    break;
//
//            }
//
//        }
//
//        public void LogNotButton(String message) {
//            //retrieve the date and create the payload of the data
//            String[] cur_day = get_day();
//
//            //Gnerarate the message payload
//            String payload = cur_day[0] + " - the button that was pressed is: " + message;
//            //write the data to the txt of that day
//            generateNoteOnSD("Logs-From-Notifications- " + (cur_day[1] + ".txt"), payload + "\r\n");
//        }
//
//        public void generateNoteOnSD(String sFileName, String sBody) {
//            try {
//
//                File root = new File(Environment.getExternalStorageDirectory(), "Pet-phone-logs");
//                if (!root.exists()) {
//                    root.mkdirs();
//                }
//                File gpxfile = new File(root, sFileName);
//                FileWriter writer = new FileWriter(gpxfile, true);
//                writer.append(sBody);
//                writer.flush();
//                writer.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//
//        }
//
//
//        private String[] get_day() {
//            //array to hold two types of dates
//            String[] reportDate = new String[2];
//
//            // Create an instance of SimpleDateFormat used for formatting
//            // the string representation of date (month/day/year)
//            DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
//
//            // Get the date today using Calendar object.
//            Date today1 = Calendar.getInstance().getTime();
//            // Using DateFormat format method we can create a string
//            // representation of a date with the defined format.
//
//            reportDate[0] = df.format(today1);
//
//            DateFormat df2 = new SimpleDateFormat("MM-dd-yyyy");
//            Date today2 = Calendar.getInstance().getTime();
//            reportDate[1] = df2.format(today2);
//
//
//            return reportDate;
//        }
//
//    }

}
