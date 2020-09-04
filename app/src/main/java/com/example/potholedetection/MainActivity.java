package com.example.potholedetection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Stream;

public class MainActivity extends AppCompatActivity implements SensorEventListener, SensorEventListener2, LocationListener, OnMapReadyCallback {
    protected LocationManager locationManager;
    private TextView txtLat;
    private TextView accelReadings;

    Button buttonStart;
    Button buttonStop;
    Button buttonCalibrate;


    FileWriter writerRawData;
    FileWriter writerFilteredData;
    final String SENSOR_TAG = "Sensor log";
    private GoogleMap mMap;
    private Marker mMarker;
    private SensorManager sensorManager;
    private Sensor accelerometer;
//    private Sensor gravitySensor;

    private double lati;
    private double longi;
    private int i;
    private int j;

    private float[] currentValues = {0,0,0};
    private float[] previousValues = {0,0,0};
    private float[] calibrationValues = {0,0,0};
    private float treshhold = 3;

    private boolean tagOnMap = false;
    private boolean isCalibrated = false;
    boolean isStarted = false;
    boolean isFirstRead = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        i = 1;
        buttonStart = findViewById(R.id.buttonStart);
        buttonStop = findViewById(R.id.buttonStop);
        buttonCalibrate = findViewById(R.id.buttonCalibrate);

        accelReadings = findViewById(R.id.accelReadings);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        txtLat = findViewById(R.id.textview1);
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
        checkPermission();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10, 1 , this);



        buttonStart.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                buttonStart.setEnabled(false);
                buttonStop.setEnabled(true);

                Log.d(SENSOR_TAG, "Writing sensor data to " + getStorageDir());

                try {
                    writerRawData = new FileWriter(new File(getStorageDir(), "accelerometer_RAW_" + System.currentTimeMillis() + ".txt"));
                    writerFilteredData = new FileWriter(new File(getStorageDir(), "accelerometer_FILTERED_" + System.currentTimeMillis() + ".txt"));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_GAME);

                isStarted = true;
                return true;
            }
        });

        buttonStop.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                buttonStart.setEnabled(true);
                buttonStop.setEnabled(false);

                sensorManager.flush(MainActivity.this);
                sensorManager.unregisterListener(MainActivity.this);
                try {
                    writerRawData.close();
                    writerFilteredData.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                isStarted = true;
                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        currentValues = sensorEvent.values.clone(); // get sensor's current readings

        currentValues[0] = currentValues[0] - calibrationValues[0];
        currentValues[1] = currentValues[1] - calibrationValues[1];
        currentValues[2] = currentValues[2] - calibrationValues[2];

        /*If reading first time assign current values to previous values to avoid false positive*/
        if(isFirstRead)
        {
            isFirstRead = false;
            previousValues = currentValues.clone();
        }
        /*
        *   NOTE: Calibrate when phone is lying on a flat surface.
        *   Calibration values are subtracted from current value readings
        */
        buttonCalibrate.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(!isCalibrated) {
                    calibrationValues = currentValues.clone();
                    isCalibrated = true;
                }
                Log.d(SENSOR_TAG, "CALIBRATION_VALUES , x: " + calibrationValues[0] + "y: " + calibrationValues[1] + "z: " + calibrationValues[2]);
                return true;
            }
        });

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

        if(Math.abs(currentValues[0] - previousValues[0]) > treshhold || Math.abs(currentValues[1] - previousValues[1]) > treshhold || Math.abs(currentValues[2] - previousValues[2]) > treshhold)
        {
            String name = "";
            tagOnMap = true;
            Log.d(SENSOR_TAG, "TAG PLACED , current: " + currentValues[0] + "\tprev: " + previousValues[0]);
            name = "Pothole #" + j;
            j++;

            if(Math.abs(currentValues[0] - previousValues[0]) > treshhold/2 || Math.abs(currentValues[1] - previousValues[1]) > treshhold/2 || Math.abs(currentValues[2] - previousValues[2]) > treshhold/2){
                name = "Speedbump #" + i;
                i++;
            }

            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(lati, longi))
                    .title(name));
        }
        else
        {
            tagOnMap = false;
        }

        if(isStarted)
        {
            try {
                writerRawData.write(String.format("%s ; ACC; %f; %f; %f; %f; %f\n", getCurrentTimeStamp(), sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2], lati, longi));  // write raw sensor readings to file
                writerFilteredData.write(String.format("%s ; ACC; %f; %f; %f; %f; %f\n", getCurrentTimeStamp(), currentValues[0], currentValues[1], currentValues[2], lati, longi));  // write filtered readings to a file
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

            previousValues = currentValues.clone();


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onFlushCompleted(Sensor sensor) {

    }

    @Override
    public void onLocationChanged(@NonNull Location location) {

        txtLat = findViewById(R.id.textview1);
        lati = location.getLatitude();
        longi = location.getLongitude();
        mMarker.setPosition(new LatLng(lati, longi));
        txtLat.setText(String.format("Latitude: %.3f, Longitude: %.3f", lati, longi));
        float zoomLevel = 16.0f; //This goes up to 21
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lati, longi), zoomLevel));

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        System.out.println("test");
        mMap = googleMap;
        mMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(lati, longi))
                .title("Marker"));
        mMarker.setIcon((BitmapDescriptorFactory.defaultMarker(270)));
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

        } else {
            checkPermission();
        }
    }

    /**
     * Returns file path on the phone where data is stored
     * @return "/file/path/";
     */
    private String getStorageDir() {
        return this.getExternalFilesDir(null).getAbsolutePath();
    }

    /**
     * Gets current time stamp
     * @return string with timestamp
     */
    private static String getCurrentTimeStamp() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("HH:mm:ss:SSS");
        Date now = new Date();
        String strDate = sdfDate.format(now);
        return strDate;
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {


            } else {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,}, 1);
            }
        }
    }
}

