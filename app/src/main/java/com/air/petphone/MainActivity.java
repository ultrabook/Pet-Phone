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
import android.widget.TextView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mSensor;
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
        TextView t = (TextView) findViewById(R.id.msg1);
        if(val[2] > 1.0f || val[2] < -1.0f) {
            Log.d("TAG", Float.toString(val[2]));

            if(val[2] < -15.0f && (System.currentTimeMillis()/1000) - now > 3 ){
                t.setText(R.string.light_drop_response);
                Intent intent = new Intent(this, MainActivity.class);

                PendingIntent p = PendingIntent.getActivity(this, 0 , intent, 0);
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                Notification noti = new Notification.Builder(this)
                        .setContentTitle("YOU DROPPED ME!")
                        .setContentText("WOW" + Float.toString(val[2]))
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .addAction(R.mipmap.ic_launcher, ":(", p)
                        .setPriority(Notification.PRIORITY_MAX)
                        .build();
                notificationManager.notify(10, noti);
                now = System.currentTimeMillis()/1000;
            }
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


}
