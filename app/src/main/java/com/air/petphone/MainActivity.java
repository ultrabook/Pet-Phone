package com.air.petphone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private int eventCounter = -1;
    private PowerManager.WakeLock wl;
    private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(BatteryCheckService.BATTERY_UI_MESSAGE);
            batteryLevelFaceDisplay(message);

            //log that the battery message has been sent in a txt
            //log the bounce count in logcat
            Log.i("BatteryReceive", "Battery Message: " + message);

            //retrieve the date and create the payload of the data
            String[] cur_day = get_day();
            String payload = cur_day[0] + " battery: " + message;
            //write the data to the txt of that day
            generateNoteOnSD(getApplicationContext(), "Battery-message-" + (cur_day[1] + ".txt"), payload + "\r\n");

            Log.e("TAG", message);
        }
    };

    private BroadcastReceiver screenReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_USER_PRESENT)) {
                Log.i("Screen", "UNLOCKED");
                String[] cur_day = get_day();
                String payload = cur_day[0] + " Unlocked ";
                generateNoteOnSD(getApplicationContext(), "Phone-Lock-" + (cur_day[1] + ".txt"), payload + "\r\n");
            }
        }
    };

    private static final int helloFace = 0x1F601;
    private static final int low_battery_face = 0x1F635;
    private static final int very_low_battery_face = 0x1F616;
    private static final int below_half_battery_face = 0x1F610;
    private static final int battery_charging_face = 0x1F60F;
    private static final int light_drop_face = 0x1F613;
    private static final int eating_face = 0x1F60B;


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

        Log.e("TAG", "onCREATE");

        //I want to insert the conditional statement here and crate two xml-s basically one owuld be to say sorry, and one wold be to say hi.
        //The xml to display will be based on whether any events are trigegred.
        //Button btn = (Button)findViewById(R.id.thebuttonid);
        //btn.setVisibility(View.VISIBLE); //View.GONE, View.INVISIBLE are available too.

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null) {
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            Log.d("STAT", "SUCCESS");
        }

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MAINLOCK");
        wl.acquire();

        setFaceAndMessage(helloFace, "Hello");

        startService(new Intent(this, KillNotificationService.class));

        Intent monitorIntent = new Intent(this, BatteryCheckService.class);
        startService(monitorIntent);

        setButtonVisibility(View.VISIBLE, View.INVISIBLE, View.INVISIBLE);

        registerReceiver(batteryReceiver,
                new IntentFilter(BatteryCheckService.BATTERY_UI_UPDATE)
        );

        registerReceiver(screenReceiver, new IntentFilter(Intent.ACTION_USER_PRESENT));

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
                                setFaceAndMessage(light_drop_face, getString(R.string.light_drop_response));
                                setButtonVisibility(View.INVISIBLE, View.INVISIBLE, View.VISIBLE);

                                NotificationCenter.sendNotification(100, c, MainActivity.class, "HEY!!", "YOU DROPPED ME!!", ":(");
                                //log the bounce count in logcat
                                Log.i("Counter", "Bounce count: " + bounceCount);
                                //retrieve the date and create the payload of the data
                                String[] cur_day = get_day();
                                String payload = cur_day[0] + " bounced: " + bounceCount;
                                //write the data to the txt of that day
                                generateNoteOnSD(getApplicationContext(), "Drop-count-" + (cur_day[1] + ".txt"), payload + "\r\n");
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
    public void onBackPressed() {
        Toast.makeText(this, "Use home button instead", Toast.LENGTH_SHORT).show();
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
        setFaceAndMessage(helloFace, getString(R.string.hello));

        Toast.makeText(getApplicationContext(), "You better be sorry >__<", Toast.LENGTH_SHORT).show();

        //null is no change
        setButtonVisibility(View.VISIBLE, null, View.INVISIBLE);

    }

    public void nice2m(View view) {

        setFaceAndMessage(helloFace, getString(R.string.hello));

        Toast.makeText(getApplicationContext(), "Nice to meet you =^.^=", Toast.LENGTH_SHORT).show();
    }

    public void bonAppetite(View view) {

        setFaceAndMessage(eating_face, getString(R.string.eating_s));

        Toast.makeText(getApplicationContext(), "Thank you!", Toast.LENGTH_SHORT).show();
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

    public void batteryLevelFaceDisplay(String input) {

        switch (input) {
            case BatteryCheckService.BATTERY_POWER_BELOW_HALF:
                setFaceAndMessage(below_half_battery_face, getString(R.string.battery_below_half));
                //remove the greeting button
                setButtonVisibility(View.INVISIBLE, null, View.INVISIBLE);

                break;
            case BatteryCheckService.BATTERY_POWER_LOW:
                setFaceAndMessage(low_battery_face, getString(R.string.battery_low));
                //remove the greeting button
                setButtonVisibility(View.INVISIBLE, null, View.INVISIBLE);

                break;
            case BatteryCheckService.BATTERY_POWER_VERY_LOW:

                //remove the greeting button
                setFaceAndMessage(very_low_battery_face, getString(R.string.battery_very_low));
                setButtonVisibility(View.INVISIBLE, null, View.INVISIBLE);

                break;
            case BatteryCheckService.BATTERY_POWER_CHARGING:

                setFaceAndMessage(battery_charging_face, getString(R.string.battery_charging));
                //t2.setText(R.string.battery_charging_face);
                setButtonVisibility(View.INVISIBLE, View.INVISIBLE, View.VISIBLE);


                break;
            default:

                setFaceAndMessage(helloFace, getString(R.string.hello));
                //Show hi button
                setButtonVisibility(View.VISIBLE, View.INVISIBLE, View.INVISIBLE);

                break;
        }
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

    //function to get the unicode emojis
    public String getEmijoByUnicode(int unicode) {
        return new String(Character.toChars(unicode));
    }

    private void setFaceAndMessage(int faceUnicode, String message) {
        TextView t1 = (TextView) findViewById(R.id.msg1);
        TextView t2 = (TextView) findViewById(R.id.face);

        String face = getEmijoByUnicode(faceUnicode);

        t1.setText(message);
        t2.setText(face);
    }

    private void setButtonVisibility(Integer hi, Integer sorry, Integer eating) {

        Button btnSorry = (Button) findViewById(R.id.sorryButton);
        Button btnHi = (Button) findViewById(R.id.hiButton);
        Button btnEating = (Button) findViewById(R.id.eatingButton);


        //set visibility if not null
        //greeting button
        if (hi != null) btnHi.setVisibility(hi);
        //sorry button
        if (sorry != null) btnSorry.setVisibility(sorry);
        //eating button
        if (eating != null) btnEating.setVisibility(eating);
    }

}

