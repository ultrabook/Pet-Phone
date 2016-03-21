package com.air.petphone;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

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


    private LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && intent.hasExtra(BATTERY_UPDATE)){
            new BatteryCheckAsync().execute();
        }
        else {
            AlarmReceiver.startAlarms(BatteryCheckService.this.getApplicationContext());
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void requireActivityUpdate(String message) {
        Intent intent = new Intent(BATTERY_UI_UPDATE);
        if(message != null)
            intent.putExtra(BATTERY_UI_MESSAGE, message);
        broadcastManager.sendBroadcast(intent);
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

            double batteryThreshold = 0.99;

            if(batteryLevel < batteryThreshold && !isCharging) {
                NotificationCenter.sendNotification(BatteryCheckService.this,BatteryCheckService.class,title, detail, "Charging now!");
            }
            else if (batteryLevel < batteryThreshold && isCharging){
                NotificationCenter.sendNotification(BatteryCheckService.this,BatteryCheckService.class,":D", "Eating eating", "Dismiss");
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
}
