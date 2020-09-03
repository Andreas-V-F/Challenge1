package com.example.potholedetection;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.*;
import java.security.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private TextView accelReadings;
    Button buttonStart;
    Button buttonStop;
    boolean isStarted;
    FileWriter writer;
    final String SENSOR_TAG = "Sensor log";

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private float[] currentValues = {0,0,0};
    private float[] previousValues = {0,0,0};
    private float treshhold = 3;
    private boolean tagOnMap = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        isStarted = false;
        buttonStart = (Button)findViewById(R.id.buttonStart);
        buttonStop = (Button)findViewById(R.id.buttonStop);

        accelReadings = findViewById(R.id.accelReadings);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        buttonStart.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                buttonStart.setEnabled(false);
                buttonStop.setEnabled(true);

                Log.d(SENSOR_TAG, "Writing sensor data to " + getStorageDir());

                try {
                    writer = new FileWriter(new File(getStorageDir(), "accelerometer_" + System.currentTimeMillis() + ".txt"));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                sensorManager.registerListener(sensorEventListener2, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 0);

                isStarted = true;
                return true;
            }
        });
        buttonStop.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                buttonStart.setEnabled(true);
                buttonStop.setEnabled(false);

                sensorManager.flush(sensorEventListener2);
                sensorManager.unregisterListener(sensorEventListener2);
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                isStarted = true;
                return false;
            }
        });
    }

    private String getStorageDir() {
        return this.getExternalFilesDir(null).getAbsolutePath();
        //  return "/file/path/";
    }

    public static String getCurrentTimeStamp() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");//dd/MM/yyyy
        Date now = new Date();
        String strDate = sdfDate.format(now);
        return strDate;
    }
    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {

            if (sensorEvent.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
            {
                return;
            }
//            Log.d(SENSOR_TAG, "PREV , current: " + currentValues[1] + "\tprev: " + previousValues[1]);
            currentValues = sensorEvent.values.clone();
//            Log.d(SENSOR_TAG, "NEXT , current: " + currentValues[1] + "\tprev: " + previousValues[1]);
            if(!isStarted)
            {
                isStarted = true;
                previousValues = currentValues;
//                Log.d(SENSOR_TAG, "ISsTARTED: " + isStarted);
            }
            accelReadings.setText(String.format(
                                "x axis: %.3f m/s^2," +
                                "\ny axis: %.3f m/s^2," +
                                "\nz axis: %.3f m/s^2",
                    currentValues[0], currentValues[1], currentValues[2]));

            /*Calculate moving average*/
            System.out.println(currentValues[0] + ", " + currentValues[1] + ", " + currentValues[2]);
            currentValues[0] = (currentValues[0] + previousValues[0])/2;
            currentValues[1] = (currentValues[1] + previousValues[1])/2;
            currentValues[2] = (currentValues[2] + previousValues[2])/2;
            /*********************************************************/
//            Log.d(SENSOR_TAG, "AFTER FILTER , current: " + currentValues[0] + "\tprev: " + previousValues[0]);
            if(Math.abs(currentValues[0] - previousValues[0]) > treshhold || Math.abs(currentValues[1] - previousValues[1]) > treshhold || Math.abs(currentValues[2] - previousValues[2]) > treshhold)
            {
                tagOnMap = true;
                Log.d(SENSOR_TAG, "TAG PLACED , current: " + currentValues[0] + "\tprev: " + previousValues[0]);
            }
            else
            {
                tagOnMap = false;
            }

//            Log.d(SENSOR_TAG, "tagOnMap = " + tagOnMap);'
            if(currentValues != previousValues)
            {
                previousValues = currentValues.clone();
            }

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    @Override
    protected void onResume() {
        super.onResume();
            sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private SensorEventListener2 sensorEventListener2 = new SensorEventListener2() {
        @Override
        public void onFlushCompleted(Sensor sensor) {

        }

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if(isStarted)
            {
                try {
                    writer.write(String.format("%s ; ACC; %f; %f; %f\n", getCurrentTimeStamp(), sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };
}