package com.air.petphone;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


/**
 * Created by Randy on 16-03-20.
 */
public class BatteryCheckService extends Service {
    private static final String TAG = "BatteryService";
    public static final String BATTERY_UPDATE = "battery";
    public static final String BATTERY_UI_UPDATE = "battery_ui_update";
    public static final String BATTERY_UI_MESSAGE = "battery_ui_message";

    public static final String BATTERY_POWER_VERY_LOW = "battery_very_low";
    public static final String BATTERY_POWER_LOW = "battery_low";
    public static final String BATTERY_POWER_BELOW_HALF = "battery_below_half";
    public static final String BATTERY_POWER_CHARGING = "battery_charging";
    public static final String BATTERY_POWER_OK = "battery_ok";

    private static Integer masterCounter = 10;
    private static Integer cpuCounter = 4;

    Intent updateUIIntent;
    private final Handler handler = new Handler();

//    private LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);


    @Override
    public void onCreate() {
        super.onCreate();
        updateUIIntent = new Intent(BATTERY_UI_UPDATE);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        if (intent != null && intent.hasExtra(BATTERY_UPDATE)){
            masterCounter++;
            new CPUCheckAsync().execute();
            new BatteryCheckAsync().execute();
        }
        else {
            AlarmReceiver.startAlarms(BatteryCheckService.this.getApplicationContext());
        }

        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void requireActivityUpdate(final String message) {

        Runnable sendUpdatesToUI = new Runnable() {
            public void run() {
                sendMessgeToUI();
            }

            private void sendMessgeToUI() {
                if(message != null)
                    updateUIIntent.putExtra(BATTERY_UI_MESSAGE, message);
                sendBroadcast(updateUIIntent);

            }
        };
        handler.removeCallbacks(sendUpdatesToUI);
        handler.postDelayed(sendUpdatesToUI, 100);

    }

    private class CPUCheckAsync extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... params) {

            cpuCounter++;
            if(cpuCounter < 4)  return null;
            cpuCounter = 0;

            Integer[] cpu = getCpuUsageStatistic();
            Integer usage = cpu[0]+cpu[1];
            Log.i("CPU", "CPU Message: "+ usage);
            String[] cur_day = get_day();
            String payload = cur_day[0] + " CPU %: " + usage;

            if(cpu[0] + cpu[1] >= 30){
                Integer cpu_f = cpu[0]+cpu[1];
                Log.i("CPU", "CPU Speed: "+ cpu_f);
                NotificationCenter.sendNotification(120, BatteryCheckService.this,MainActivity.class,">____<\"\"", "CPU is doing work", "Sorry for making you work so hard!");
                payload = payload + " (noti)";
            }
            generateNoteOnSD(getApplicationContext(), "CPU-" + (cur_day[1] + ".txt"), payload + "\r\n");
            return null;
        }
    }

    private class BatteryCheckAsync extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... arg0) {

            String uiMessage = BATTERY_POWER_BELOW_HALF;

            //Battery State check - create log entries of current battery state
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = BatteryCheckService.this.registerReceiver(null, ifilter);

            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;
            Log.i("BatteryInfo", "Battery is charging: " + isCharging);


            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            Log.i("BatteryInfo", "level: " + level +" scale: " + scale);
            Log.i("BatteryInfo", "Battery charge level: " + (level / (float)scale));

            float batteryLevel = (level / (float)scale);

            String detail = "Getting kind of hungry";
            String title = "0____0\"\"";

            if(batteryLevel <= 0.1) {
                detail = "Too weak... Need food...";
                title = "X___X";
                uiMessage = BATTERY_POWER_VERY_LOW;
            }
            else if (batteryLevel <= 0.3){
                detail = "Feed me please";
                title = "@___@";
                uiMessage = BATTERY_POWER_LOW;
            }

            double batteryThreshold = 0.5;

            if(batteryLevel < batteryThreshold && !isCharging) {
                if(masterCounter >= 10) {
                    NotificationCenter.sendNotification(121, BatteryCheckService.this, MainActivity.class, title, detail, "Sorry for starving you!");
                    masterCounter = 0;
                }
            }
            else if (batteryLevel < batteryThreshold && isCharging){
                if(masterCounter >= 10) {
                    NotificationCenter.sendNotification(121, BatteryCheckService.this, MainActivity.class, ":D", "What took you so long?", "Sorry that it took so long!");
                    masterCounter = 0;
                }
                uiMessage = BATTERY_POWER_CHARGING;
            }
            else if (batteryLevel > batteryThreshold) {
                uiMessage = BATTERY_POWER_OK;
            }

            onPostExecute(uiMessage);


            return null;
        }

        protected void onPostExecute(String message){
            BatteryCheckService.this.requireActivityUpdate(message);
            BatteryCheckService.this.stopSelf();
        }
    }

    private Integer[] getCpuUsageStatistic() {

        String tempString = executeTop();

        tempString = tempString.replaceAll(",", "");
        tempString = tempString.replaceAll("User", "");
        tempString = tempString.replaceAll("System", "");
        tempString = tempString.replaceAll("IOW", "");
        tempString = tempString.replaceAll("IRQ", "");
        tempString = tempString.replaceAll("%", "");
        for (int i = 0; i < 10; i++) {
            tempString = tempString.replaceAll("  ", " ");
        }
        tempString = tempString.trim();
        String[] myString = tempString.split(" ");
        Integer[] cpuUsageAsInt = new Integer[myString.length];
        for (int i = 0; i < myString.length; i++) {
            myString[i] = myString[i].trim();
            cpuUsageAsInt[i] = Integer.parseInt(myString[i]);
        }
        return cpuUsageAsInt;
    }

    private String executeTop() {
        java.lang.Process p = null;
        BufferedReader in = null;
        String returnString = null;
        try {
            p = Runtime.getRuntime().exec("top -n 1");
            in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while (returnString == null || returnString.contentEquals("")) {
                returnString = in.readLine();
            }
        } catch (IOException e) {
            Log.e("executeTop", "error in getting first line of top");
            e.printStackTrace();
        } finally {
            try {
                in.close();
                p.destroy();
            } catch (IOException e) {
                Log.e("executeTop",
                        "error in closing and destroying top process");
                e.printStackTrace();
            }
        }
        return returnString;
    }

    public void generateNoteOnSD(Context context, String sFileName, String sBody) {
        try {

            File root = new File(Environment.getExternalStorageDirectory(), "Pet-phone-logs");
            if (!root.exists()) {
                root.mkdirs();
            }
            File gpxfile = new File(root, sFileName);
            FileWriter writer = new FileWriter(gpxfile, true);
            writer.append(sBody);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String[] get_day() {
        //array to hold two types of dates
        String[] reportDate = new String[2];

        // Create an instance of SimpleDateFormat used for formatting
        // the string representation of date (month/day/year)
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

        // Get the date today using Calendar object.
        Date today1 = Calendar.getInstance().getTime();
        // Using DateFormat format method we can create a string
        // representation of a date with the defined format.

        reportDate[0] = df.format(today1);

        DateFormat df2 = new SimpleDateFormat("MM-dd-yyyy");
        Date today2 = Calendar.getInstance().getTime();
        reportDate[1] = df2.format(today2);


        return reportDate;
    }
}
