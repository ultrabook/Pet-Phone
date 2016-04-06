package com.air.petphone;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;


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

    private static Integer masterCounter = 3;

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
                Log.i("BatterySend", "Battery Message: "+ message);
                sendBroadcast(updateUIIntent);

            }
        };
        handler.removeCallbacks(sendUpdatesToUI);
        handler.postDelayed(sendUpdatesToUI, 100);

    }

    private class CPUCheckAsync extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... params) {
            Integer[] cpu = getCpuUsageStatistic();
            if(cpu[0] + cpu[1] > 75){
                Integer cpu_f = cpu[0]+cpu[1];
                Log.i("Date"+DateFormat.getDateTimeInstance(), "CPU Message: "+ cpu_f);
                NotificationCenter.sendNotification(120, BatteryCheckService.this,BatteryCheckService.class,">____<\"\"", "CPU is doing so much work", "Ok");
            }
            return null;
        }
    }

    private class BatteryCheckAsync extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... arg0) {

            if(masterCounter < 3) return null;
            masterCounter = 0;

            String uiMessage = BATTERY_POWER_BELOW_HALF;

            //Battery State check - create log entries of current battery state
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = BatteryCheckService.this.registerReceiver(null, ifilter);

            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;
            Log.i("BatteryInfo", "Battery is charging: " + isCharging);
            Log.i("Date"+DateFormat.getDateTimeInstance(), "Battery charging: "+ isCharging);


            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            Log.i("BatteryInfo", "level: " + level +" scale: " + scale);
            Log.i("BatteryInfo", "Battery charge level: " + (level / (float)scale));
            Log.i("Date"+DateFormat.getDateTimeInstance(), "Battery charge level: "+ (level / (float)scale));

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
                NotificationCenter.sendNotification(121,BatteryCheckService.this,BatteryCheckService.class,title, detail, "Charging now!");
            }
            else if (batteryLevel < batteryThreshold && isCharging){
                NotificationCenter.sendNotification(121,BatteryCheckService.this,BatteryCheckService.class,":D", "Eating eating", "Dismiss");
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
}
