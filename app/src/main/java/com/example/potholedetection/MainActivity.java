package com.example.potholedetection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
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

public class MainActivity extends AppCompatActivity {

    private TextView accelReadings;
    Button buttonStart;
    Button buttonStop;
    boolean isStarted;
    FileWriter writer;
    final String SENSOR_TAG = "Sensor log";

    private SensorManager sensorManager;
    private Sensor accelerometer;

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
                    writer = new FileWriter(new File(getStorageDir(), "accelerometer_" + System.currentTimeMillis() + ".csv"));
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
    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            float[] values = sensorEvent.values;
                accelReadings.setText(String.format(
                        "x axis: %.3f m/s^2," +
                                "\ny axis: %.3f m/s^2," +
                                "\nz axis: %.3f m/s^2",
                        values[0], values[1], values[2]));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    @Override
    protected void onResume() {
        super.onResume();
            sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
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
                    writer.write(String.format("%d; ACC; %f; %f; %f; %f; %f; %f\n", sensorEvent.timestamp, sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2], 0.f, 0.f, 0.f));
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