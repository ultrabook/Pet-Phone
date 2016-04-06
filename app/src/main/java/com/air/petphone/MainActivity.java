package com.air.petphone;

import android.content.BroadcastReceiver;
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




        startService(new Intent(this, KillNotificationService.class));

        Intent monitorIntent = new Intent(this, BatteryCheckService.class);
        startService(monitorIntent);

        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String message = intent.getStringExtra(BatteryCheckService.BATTERY_UI_MESSAGE);
                batteryLevelFaceDisplay(message);

                //log that the battery message has been sent in a txt
                //log the bounce count in logcat
                Log.i("Battery", "Battery Message: " +message);

                //retrieve the date and create the payload of the data
                String[] cur_day = get_day();
                String payload = cur_day[0]+" is: "+message;
                //write the data to the txt of that day
                generateNoteOnSD(getApplicationContext(), "Battery-message-" + (cur_day[1] + ".txt"), "Battery message on " + (payload + "\r\n"));

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
                                int unic = 0x1F613;
                                String face =  getEmijoByUnicode(unic);
                                t2.setText(face);
                                //------set up the buttons-----//
                                Button btnHi = (Button)findViewById(R.id.hiButton);
                                Button btnEating = (Button)findViewById(R.id.eatingButton);
                                Button btnSorry = (Button)findViewById(R.id.sorryButton);
                                //---remove the non-sorry buttons---//
                                btnHi.setVisibility(View.INVISIBLE); //remove the greeting button
                                btnEating.setVisibility(View.INVISIBLE); // remove the eating button too

                                //--show the apology button ---//
                                btnSorry.setVisibility(View.VISIBLE);


                               // t2.setText(R.string.shocked_face);
                                //sendNotification("HEY!!", "YOU DROPPED ME!!", ":(");

                                NotificationCenter.sendNotification(100, c, MainActivity.class, "HEY!!", "YOU DROPPED ME!!", ":(");
                                //log the bounce count in logcat
                                Log.i("Counter", "Number of times dropped: " + bounceCount);
                                //retrieve the date and create the payload of the data
                                String[] cur_day = get_day();
                                String payload = cur_day[0]+": is "+bounceCount;
                                //write the data to the txt of that day
                                generateNoteOnSD(getApplicationContext(), "Drop-count-" + (cur_day[1] + ".txt"), "number of times dropped on " + (payload + "\r\n"));
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
//        mSensorManager.unregisterListener(this);
//        Intent monitorIntent = new Intent(this, BatteryCheckService.class);
//        stopService(monitorIntent);

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
        int unic = 0x1F601;
        String face =  getEmijoByUnicode(unic);
        t2.setText(face);

        //t2.setText(R.string.happy_face);

        Toast.makeText(getApplicationContext(), "You better be sorry >__<", Toast.LENGTH_SHORT).show();
        //------set up the buttons-----//
       Button btnHi = (Button)findViewById(R.id.hiButton);
       // Button btnEating = (Button)findViewById(R.id.eatingButton);
        Button btnSorry = (Button)findViewById(R.id.sorryButton);
        //---Show the greeting buttons---//
        btnHi.setVisibility(View.VISIBLE); //remove the greeting button

        //--Hide the apology button ---//
        btnSorry.setVisibility(View.INVISIBLE);


    }

    public void nice2m(View view) {
        TextView t1 = (TextView) findViewById(R.id.msg1);
        TextView t2 = (TextView) findViewById(R.id.face);

        t1.setText(R.string.hello);
        int unic = 0x1F601;
        String face =  getEmijoByUnicode(unic);
        t2.setText(face);

        //t2.setText(R.string.happy_face);

        Toast.makeText(getApplicationContext(), "Nice to meet you =^.^=", Toast.LENGTH_SHORT).show();
    }

    public void bonAppetite(View view) {
        TextView t1 = (TextView) findViewById(R.id.msg1);
        TextView t2 = (TextView) findViewById(R.id.face);

        t1.setText(R.string.eating_s);
        int unic = 0x1F60B;
        String face =  getEmijoByUnicode(unic);
        t2.setText(face);

        //t2.setText(R.string.happy_face);

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

    public void batteryLevelFaceDisplay(String input){
        TextView t1 = (TextView) findViewById(R.id.msg1);
        TextView t2 = (TextView) findViewById(R.id.face);
        String face ="";
        int unic;
        Button btnSorry = (Button)findViewById(R.id.sorryButton);
        Button btnHi = (Button)findViewById(R.id.hiButton);
        Button btnEating = (Button)findViewById(R.id.eatingButton);



        switch (input){
            case BatteryCheckService.BATTERY_POWER_BELOW_HALF:
                t1.setText(R.string.battery_below_half);
                unic = 0x1F610;
                face =  getEmijoByUnicode(unic);
                t2.setText(face);
                //remove the greeting button
                btnHi.setVisibility(View.INVISIBLE); //View.GONE, View.INVISIBLE are available too.
                btnEating.setVisibility(View.INVISIBLE); // remove the eating button too

                //t2.setText(R.string.battery_below_half_face);

                break;
            case BatteryCheckService.BATTERY_POWER_LOW:
                t1.setText(R.string.battery_low);
                unic = 0x1F635;
                face =  getEmijoByUnicode(unic);
                t2.setText(face);
                //t2.setText(R.string.battery_low_face);
                //remove the greeting button
                btnHi.setVisibility(View.INVISIBLE); //View.GONE, View.INVISIBLE are available too.
                btnEating.setVisibility(View.INVISIBLE); // remove the eating button too

                break;
            case BatteryCheckService.BATTERY_POWER_VERY_LOW:
                t1.setText(R.string.battery_very_low);
                unic = 0x1F616;
                face =  getEmijoByUnicode(unic);
                t2.setText(face);
                //t2.setText(R.string.battery_very_low_face);
                //remove the greeting button
                btnHi.setVisibility(View.INVISIBLE); //View.GONE, View.INVISIBLE are available too.
                btnEating.setVisibility(View.INVISIBLE); //remove the eating button too

                break;
            case BatteryCheckService.BATTERY_POWER_CHARGING:
                t1.setText(R.string.battery_charging);
                unic = 0x1F60F;
                face =  getEmijoByUnicode(unic);
                t2.setText(face);
                //t2.setText(R.string.battery_charging_face);
                //remove the greeting button
                btnHi.setVisibility(View.INVISIBLE); //View.GONE, View.INVISIBLE are available too.
                //remove the sorry button
                btnSorry.setVisibility(View.INVISIBLE);
                //Show the eating button
                btnEating.setVisibility(View.VISIBLE);


                break;
            default:
                t1.setText(R.string.hello);
                //standard smiling face
                unic = 0x1F601;
                face =  getEmijoByUnicode(unic);
                t2.setText(face);
                //Show hi button
                btnHi.setVisibility(View.VISIBLE);
                //remove the sorry button
                btnSorry.setVisibility(View.INVISIBLE);
                //remove the eating button
                btnEating.setVisibility(View.INVISIBLE);
               // t2.setText(R.string.happy_face);
                break;
        }
    }


    public void generateNoteOnSD(Context context, String sFileName, String sBody) {
        try {
 //           String state = Environment.getExternalStorageState();
//            Toast.makeText(getApplicationContext(),"State is " + state, Toast.LENGTH_LONG).show();

//            boolean mExternalStorageAvailable = false;
//            boolean mExternalStorageWriteable = false;
//
//            if (Environment.MEDIA_MOUNTED.equals(state)){
//                //We can read and write the media
//                mExternalStorageAvailable = mExternalStorageWriteable = true;
//                Toast.makeText(getApplicationContext(), "We Can Read And Write ", Toast.LENGTH_LONG).show();
//
//            } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)){
//                mExternalStorageAvailable = true;
//                mExternalStorageWriteable = false;
//                Toast.makeText(getApplicationContext(), "We Can Read but Not Write ", Toast.LENGTH_LONG).show();
//            }else{
//                //something else is wrong
//                mExternalStorageAvailable = mExternalStorageWriteable = false;
//                Toast.makeText(getApplicationContext(), "We Can't Read OR Write ", Toast.LENGTH_LONG).show();
//            }

            File root = new File(Environment.getExternalStorageDirectory(), "Pet-phone-logs");
            if (!root.exists()) {
                root.mkdirs();
            }
            File gpxfile = new File(root, sFileName);
            FileWriter writer = new FileWriter(gpxfile,true);
            writer.append(sBody);
            writer.flush();
            writer.close();
//          Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String[] get_day ()
    {
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
    public String getEmijoByUnicode(int unicode)
    {
        return new String(Character.toChars(unicode));
    }

}

