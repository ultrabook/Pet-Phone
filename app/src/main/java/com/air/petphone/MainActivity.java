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
    private static int unlockCounter = 0;
    private PowerManager.WakeLock wl;
    private String currentBatteryMessage = BatteryCheckService.BATTERY_POWER_OK;
    private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(BatteryCheckService.BATTERY_UI_MESSAGE);
            batteryLevelFaceDisplay(message);

            //log that the battery message has been sent in a txt
            //log the bounce count in logcat
            Log.i("BatteryReceive", "Battery Message: " + message);

            //log only if the battery status is something other than OK
            if(!message.equals(BatteryCheckService.BATTERY_POWER_OK))
            {
                loggingBattery(message);
            }

            Log.e("TAG", message);
        }
    };

    private BroadcastReceiver screenReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_USER_PRESENT)) {

                unlockCounter++;
                String[] cur_day = get_day();
                String payload = cur_day[0] + " Unlocked ";
                generateNoteOnSD(getApplicationContext(), "Phone-Lock-" + (cur_day[1] + ".txt"), payload + "\r\n");

                if(unlockCounter >= 4) {
                    NotificationCenter.sendNotification(131, MainActivity.this, MainActivity.class, ">___>", "Can't you see I'm sleeping?", "Oops! Sorry..");
                    //set the sleepy face
                    setFaceAndMessage(sleepy_face, "Zzzz...");
                    setButtonVisibility(View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.VISIBLE);

                    unlockCounter = 0;
                }
            }
        }
    };

    private BroadcastReceiver plugReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_POWER_CONNECTED)) {

                NotificationCenter.sendNotification(130, MainActivity.this, MainActivity.class, "^_____^", "Food Time!", "Sorry it took so long!");
                if(!currentBatteryMessage.equals(BatteryCheckService.BATTERY_POWER_OK)) {
                    batteryLevelFaceDisplay(BatteryCheckService.BATTERY_POWER_CHARGING);
                }
                //log that power was connected
                loggingBattery("power_connected");

            }

            if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
                Log.i("Plug", "unplugged");
                NotificationCenter.sendNotification(130, MainActivity.this, MainActivity.class, "T____T", "No more food", "Sorry! I'll charge you more next time!");
                batteryLevelFaceDisplay(BatteryCheckService.BATTERY_POWER_OK);
                //Log that power was disconnected
                loggingBattery("power_disconnected");
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
    private static final int sleepy_face = 0x1F62A;
    private static final int unamused_face = 0x1F612;

    private boolean forceNotification = false;
    private boolean hasPendingDropApology = false;

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

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Log.d("STAT", "SUCCESS");
        } else {
            Toast.makeText(this,"Sorry, your phone doesn't have accelerometer", Toast.LENGTH_LONG).show();
        }



        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MAINLOCK");
        wl.acquire();

        setFaceAndMessage(helloFace, "Hello!");

        startService(new Intent(this, KillNotificationService.class));

        Intent monitorIntent = new Intent(this, BatteryCheckService.class);
        startService(monitorIntent);

        setButtonVisibility(View.VISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE);

        registerReceiver(batteryReceiver,new IntentFilter(BatteryCheckService.BATTERY_UI_UPDATE));
        registerReceiver(screenReceiver, new IntentFilter(Intent.ACTION_USER_PRESENT));
        IntentFilter filter = new IntentFilter(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(plugReceiver, filter);


        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public float getAccelerationVector(){
        return (float) Math.sqrt(val[0]*val[0]+val[1]*val[1]+val[2]*val[2]);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        final Context c = this;
        val = lowPass(event.values.clone(), val);
        float vector = getAccelerationVector();

        //Log.d("Sensor", val[0] + "  "+val[1]+"  "+val[2]);
       // Log.d("Sensor", ""+vector);
        //if(Math.abs(val[2]) > 10){

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("TAG", "MANY COUNT " + eventCounter);
                        if ((eventCounter > 1 && eventCounter < 50) || forceNotification) {
                            setFaceAndMessage(light_drop_face, getString(R.string.light_drop_response));
                            setButtonVisibility(View.INVISIBLE, View.VISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE);

                            NotificationCenter.sendNotification(100, c, MainActivity.class, "HEY!!", "Gentle!!", " Sorry!");

                            //retrieve the date and create the payload of the data
                            String[] cur_day = get_day();
                            String payload = cur_day[0] + ": dropped";
                            //write the data to the txt of that day
                            generateNoteOnSD(getApplicationContext(), "Drop-count-" + (cur_day[1] + ".txt"), payload + "\r\n");
                            hasPendingDropApology = true;
                        }
                        eventCounter = -1;
                        forceNotification = false;
                    }
                });
            }
        };

        if(vector < 2f && eventCounter == -1){
            eventCounter = 1;
            Log.d("Sensor", ""+vector);
            Timer timer = new Timer("timer1");
            timer.schedule(task, 3000);
        }
        else if(vector > 16f && eventCounter != -1) {
            //Log.d("Sensor", "v: "+vector + " z: "+val[2]);
            Log.d("Sensor", "" + vector);
            eventCounter++;

        }

        if(vector >= 30f && eventCounter != -1) {
            forceNotification = true;
        }

    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Use home button instead", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_GAME);
        eventCounter = -1;
        Log.d("TAG", "App is resumed");

        Intent intent = getIntent();
        if(intent.hasExtra(NotificationCenter.NOTIFICATION_EXTRA)) {
            Boolean boo = intent.getBooleanExtra(NotificationCenter.NOTIFICATION_EXTRA, false);
            Log.e("TAG", "Resume with extra: " + boo);

            String[] cur_day = get_day();
            String payload = cur_day[0] + " - Tapped on notifcation button";
            //write the data to the txt of that day
            generateNoteOnSD(getApplicationContext(), "Apology-Count-" + (cur_day[1] + ".txt"), payload + "\r\n");
        }




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

    public void wakeup(View view) {
        setFaceAndMessage(helloFace, getString(R.string.hello));
        Toast.makeText(getApplicationContext(), "Fine!", Toast.LENGTH_SHORT).show();
        setButtonVisibility(View.VISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE);
    }

    public void saySorry(View view) {
        hasPendingDropApology = false;
        setFaceAndMessage(helloFace, getString(R.string.hello));

        Toast.makeText(getApplicationContext(), "You better be sorry >__<", Toast.LENGTH_SHORT).show();

        //null is no change
        setButtonVisibility(View.VISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE);

        batteryLevelFaceDisplay(currentBatteryMessage);
        //---log the apology to the phone --//
        //retrieve the date and create the payload of the data
        String[] cur_day = get_day();
        String payload = cur_day[0] + " - sorry for DROPPING";
        //write the data to the txt of that day
        generateNoteOnSD(getApplicationContext(), "Apology-Count-" + (cur_day[1] + ".txt"), payload + "\r\n");

    }

    public void cantFeedYou(View view) {

        setFaceAndMessage(unamused_face, getString(R.string.hungry));
        setButtonVisibility(View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.VISIBLE, View.INVISIBLE);
        //Display the phones response to your denial
        Toast.makeText(getApplicationContext(), "But Im so hungry...", Toast.LENGTH_SHORT).show();

        //---log the apology to the phone --//
        //retrieve the date and create the payload of the data
        String[] cur_day = get_day();
        String payload = cur_day[0] + " - sorry for NOT CHARGING";
        //write the data to the txt of that day
        generateNoteOnSD(getApplicationContext(), "Apology-Count-" + (cur_day[1] + ".txt"), payload + "\r\n");

    }
    public void nice2m(View view) {

        setFaceAndMessage(helloFace, getString(R.string.hello));
        setButtonVisibility(View.VISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE);

        Toast.makeText(getApplicationContext(), "Nice to meet you =^.^=", Toast.LENGTH_SHORT).show();
    }

    public void bonAppetite(View view) {

       // setFaceAndMessage(eating_face, getString(R.string.eating_s));
        //set the face to be happy hello face
        //setFaceAndMessage(helloFace, getString(R.string.hello));
        //Display phone gratitude
        Toast.makeText(getApplicationContext(), "Thank you for the food!", Toast.LENGTH_SHORT).show();
        setButtonVisibility(View.INVISIBLE, View.INVISIBLE, View.VISIBLE, View.INVISIBLE, View.INVISIBLE);


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

        if(hasPendingDropApology) {return;}
        currentBatteryMessage = input;

        switch (input) {
            case BatteryCheckService.BATTERY_POWER_BELOW_HALF:
                setFaceAndMessage(below_half_battery_face, getString(R.string.battery_below_half));
                //remove the greeting button
                setButtonVisibility(View.INVISIBLE, null, View.INVISIBLE, View.VISIBLE, View.INVISIBLE);
                break;
            case BatteryCheckService.BATTERY_POWER_LOW:
                setFaceAndMessage(low_battery_face, getString(R.string.battery_low));
                //remove the greeting button
                setButtonVisibility(View.INVISIBLE, null, View.INVISIBLE, View.VISIBLE, View.INVISIBLE);
                break;
            case BatteryCheckService.BATTERY_POWER_VERY_LOW:

                //remove the greeting button
                setFaceAndMessage(very_low_battery_face, getString(R.string.battery_very_low));
                setButtonVisibility(View.INVISIBLE, null, View.INVISIBLE, View.VISIBLE, View.INVISIBLE);
                break;
            case BatteryCheckService.BATTERY_POWER_CHARGING:

                setFaceAndMessage(battery_charging_face, getString(R.string.battery_charging));
                //t2.setText(R.string.battery_charging_face);
                setButtonVisibility(View.INVISIBLE, View.INVISIBLE, View.VISIBLE, View.INVISIBLE, View.INVISIBLE);
                break;
            default:

                setFaceAndMessage(helloFace, getString(R.string.hello));
                //Show hi button
                setButtonVisibility(View.VISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE);

                break;
        }
    }


    public void loggingBattery(String message) {
        //retrieve the date and create the payload of the data
        String[] cur_day = get_day();

            String payload = cur_day[0] + ": " + message;
            //write the data to the txt of that day
            generateNoteOnSD(getApplicationContext(), "Battery-Messages-" + (cur_day[1] + ".txt"), payload + "\r\n");


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

    private void setButtonVisibility(Integer hi, Integer sorry, Integer eating, Integer cantFeed, Integer wakeup) {

        Button btnSorry = (Button) findViewById(R.id.sorryButton);
        Button btnHi = (Button) findViewById(R.id.hiButton);
        Button btnEating = (Button) findViewById(R.id.eatingButton);
        Button btnCantFeed = (Button) findViewById(R.id.cantFeed);
        Button btnWakeup = (Button) findViewById(R.id.wakeupButton);


        //set visibility if not null
        //greeting button
        if (hi != null) btnHi.setVisibility(hi);
        //sorry button
        if (sorry != null) btnSorry.setVisibility(sorry);
        //eating button
        if (eating != null) btnEating.setVisibility(eating);
        //cant feed button
        if (cantFeed != null) btnCantFeed.setVisibility(cantFeed);
        if(btnWakeup != null) btnWakeup.setVisibility(wakeup);
    }

}

