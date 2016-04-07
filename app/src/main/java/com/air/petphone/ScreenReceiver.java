package com.air.petphone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Ivan on 06/04/2016.
 */
public class ScreenReceiver extends BroadcastReceiver {

    public static boolean wasScreenOn = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            // Log that the screen was turned off
            //retrieve the date and create the payload of the data
            String[] cur_day = get_day();
            String payload = cur_day[0] + " - The screen was turned - OFF ";
            //write the data to the txt of that day
            generateNoteOnSD("Screen-Activity-" + (cur_day[1] + ".txt"), payload + "\r\n");

            wasScreenOn = false;
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            //Log that the screen was turned on
            String[] cur_day = get_day();
            String payload = cur_day[0] + " - The screen was turned - ON ";
            //write the data to the txt of that day
            generateNoteOnSD("Screen-Activity-" + (cur_day[1] + ".txt"), payload + "\r\n");

            wasScreenOn = true;
        }
    }


    public void generateNoteOnSD(String sFileName, String sBody) {
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