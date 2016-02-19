package com.air.petphone;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private int eventCounter = -1;
    private Long now = new Long(0);

    protected float[] val;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        if(mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null){
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            Log.d("STAT","SUCCESS");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        val = lowPass(event.values.clone(), val);
        final TextView t = (TextView) findViewById(R.id.msg1);
        final TextView t2 = (TextView) findViewById(R.id.face);
        if(val[2] > 1.0f || val[2] < -1.0f) {
            Log.d("TAG", Float.toString(val[2]));

            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("TAG", "MANY COUNT " + eventCounter);
                            if(eventCounter < 10 ){
                                t.setText(R.string.light_drop_response);
                                t2.setText(R.string.shocked_face);
                                sendNotification();
                                eventCounter = -1;
                            }
                        }
                    });
                }
            };

            if(val[2] < -15.0f && eventCounter == -1){
                eventCounter = 1;
                Timer timer = new Timer("timer1");
                timer.schedule(task, 3000);
            }
            else if (val[2] < -15.0f && eventCounter != -1){
                eventCounter++;
            }

//            if(val[2] < -15.0f && (System.currentTimeMillis()/1000) - now > 3 ){
//                t.setText(R.string.light_drop_response);
//                sendNotification();
//            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //mSensorManager.unregisterListener(this);
    }

    float ALPHA = 0.5f;

    protected float[] lowPass( float[] input, float[] output ) {
        if ( output == null ) return input;
        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    private void sendNotification() {
        Intent intent = new Intent(this, MainActivity.class);

        PendingIntent p = PendingIntent.getActivity(this, 0 , intent, 0);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification noti = new Notification.Builder(this)
                .setContentTitle("YOU DROPPED ME!")
                .setContentText("WOW" + Float.toString(val[2]))
                .setSmallIcon(R.mipmap.ic_launcher)

                .addAction(R.mipmap.ic_launcher, ":(", p)
                .setPriority(Notification.PRIORITY_MAX)
                .setVibrate(new long[] {0, 1000, 100, 1000, 100, 1000, 100})
                .setDefaults(Notification.DEFAULT_SOUND)
                .build();
        notificationManager.notify(10, noti);
        now = System.currentTimeMillis()/1000;
    }

    public void saySorry(View view){
        TextView t = (TextView) findViewById(R.id.msg1);
        TextView t2 = (TextView) findViewById(R.id.face);

        t.setText(R.string.hello);
        t2.setText(R.string.happy_face);

        Toast.makeText(getApplicationContext(), "You better be sorry >__<", Toast.LENGTH_SHORT).show();
    }

}
