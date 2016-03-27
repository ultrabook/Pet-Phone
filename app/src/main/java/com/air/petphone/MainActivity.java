package com.air.petphone;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private int eventCounter = -1;
    private PowerManager.WakeLock wl;
    private BroadcastReceiver batteryReceiver;
    private ServiceConnection mConnection;

    private int bounceCount = 0;

    protected float[] val;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null) {
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            Log.d("STAT", "SUCCESS");
        }

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MAINLOCK");
        wl.acquire();


        mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className,
                                           IBinder binder) {
                ((KillNotificationService.KillBinder) binder).service.startService(new Intent(
                        MainActivity.this, KillNotificationService.class));
                //Generate permanent notification
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                Notification noti = new Notification.Builder(MainActivity.this)
                        .setContentTitle("^____^")
                        .setContentText("I'm Your Pet!")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setOngoing(true)
                        .build();
                notificationManager.notify(KillNotificationService.NOTIFICATION_ID, noti);
            }

            public void onServiceDisconnected(ComponentName className) {
            }

        };
        bindService(new Intent(MainActivity.this,
                        KillNotificationService.class), mConnection,
                Context.BIND_AUTO_CREATE);



        Intent monitorIntent = new Intent(this, BatteryCheckService.class);
        startService(monitorIntent);

        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String message = intent.getStringExtra(BatteryCheckService.BATTERY_UI_MESSAGE);
                batteryLevelFaceDisplay(message);
                Log.e("TAG", message);
            }
        };

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        final Context c = this;
        val = lowPass(event.values.clone(), val);
        final TextView t = (TextView) findViewById(R.id.msg1);
        final TextView t2 = (TextView) findViewById(R.id.face);
        if (val[2] > 1.0f || val[2] < -1.0f) {
           // Log.d("TAG", Float.toString(val[2]));

            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("TAG", "MANY COUNT " + eventCounter);
                            if (eventCounter < 20) {
                                t.setText(R.string.light_drop_response);
                                t2.setText(R.string.shocked_face);
                                //sendNotification("HEY!!", "YOU DROPPED ME!!", ":(");

                                NotificationCenter.sendNotification(c, MainActivity.class, "HEY!!", "YOU DROPPED ME!!", ":(");
                            }
                            eventCounter = -1;
                        }
                    });
                }
            };

            if (val[2] < -15.0f) {
                bounceCount++;
            }

            if (val[2] < -15.0f && eventCounter == -1) {
                eventCounter = 1;
                Timer timer = new Timer("timer1");
                timer.schedule(task, 3000);

                Log.d("Counter", "Counter Loaded");
            } else if (val[2] < -15.0f && eventCounter != -1) {
               // Log.d("TAG", Float.toString(val[2]));
                eventCounter++;
               // Log.d("TAG", "2");
            }

//            if(val[2] < -15.0f && (System.currentTimeMillis()/1000) - now > 3 ){
//                t.setText(R.string.light_drop_response);
//                sendNotification();
//            }
        }

    }


    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_GAME);
        eventCounter = -1;
        Log.d("TAG", "App is resumed");
    }

    @Override
    protected void onStart() {
        super.onStart();

        LocalBroadcastManager.getInstance(this).registerReceiver((batteryReceiver),
                new IntentFilter(BatteryCheckService.BATTERY_UI_UPDATE)
        );


        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        wl.acquire();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.air.petphone/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        mSensorManager.unregisterListener(this);
        Intent monitorIntent = new Intent(this, BatteryCheckService.class);
        stopService(monitorIntent);

        Log.e("TAG", "DESTORY");
    }

    //Low pass filter to filter out unwanted signals
    float ALPHA = 0.5f;

    protected float[] lowPass(float[] input, float[] output) {
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    public void saySorry(View view) {
        TextView t1 = (TextView) findViewById(R.id.msg1);
        TextView t2 = (TextView) findViewById(R.id.face);

        t1.setText(R.string.hello);
        t2.setText(R.string.happy_face);

        Toast.makeText(getApplicationContext(), "You better be sorry >__<", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(batteryReceiver);
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.air.petphone/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    public void batteryLevelFaceDisplay(String input){
        TextView t1 = (TextView) findViewById(R.id.msg1);
        TextView t2 = (TextView) findViewById(R.id.face);

        switch (input){
            case BatteryCheckService.BATTERY_POWER_BELOW_HALF:
                t1.setText(R.string.battery_below_half);
                t2.setText(R.string.battery_below_half_face);
                break;
            case BatteryCheckService.BATTERY_POWER_LOW:
                t1.setText(R.string.battery_low);
                t2.setText(R.string.battery_low_face);
                break;
            case BatteryCheckService.BATTERY_POWER_VERY_LOW:
                t1.setText(R.string.battery_very_low);
                t2.setText(R.string.battery_very_low_face);
                break;
            case BatteryCheckService.BATTERY_POWER_CHARGING:
                t1.setText(R.string.battery_charging);
                t2.setText(R.string.battery_charging_face);
                break;
            default:
                t1.setText(R.string.hello);
                t2.setText(R.string.happy_face);
                break;
        }
    }
}
