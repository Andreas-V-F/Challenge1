package com.example.potholedetection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
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

public class MainActivity extends AppCompatActivity implements LocationListener, OnMapReadyCallback {
    protected LocationManager locationManager;
    TextView txtLat;
    private TextView accelReadings;
    Button buttonStart;
    Button buttonStop;
    boolean isStarted;
    FileWriter writer;
    final String SENSOR_TAG = "Sensor log";
    private GoogleMap mMap;
    private Marker mMarker;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    double lati;
    double longi;
    int i;
    int j;

    private float[] currentValues = {0,0,0};
    private float[] previousValues = {0,0,0};
    private float treshhold = 3;
    private boolean tagOnMap = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        i = 1;
        j = 1;
        isStarted = false;
        buttonStart = (Button) findViewById(R.id.buttonStart);
        buttonStop = (Button) findViewById(R.id.buttonStop);

        accelReadings = findViewById(R.id.accelReadings);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        txtLat = findViewById(R.id.textview1);
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);

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

    public void checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {


            } else {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,}, 1);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
            sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
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
                String name = "";
                tagOnMap = true;
                Log.d(SENSOR_TAG, "TAG PLACED , current: " + currentValues[0] + "\tprev: " + previousValues[0]);
                if(Math.abs(currentValues[0] - previousValues[0]) > 5 || Math.abs(currentValues[1] - previousValues[1]) > 5 || Math.abs(currentValues[2] - previousValues[2]) > 5){
                    name = "Pothole #" + j;
                    j++;
                }
                else{
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

    @Override
    public void onLocationChanged(@NonNull Location location) {

        txtLat = findViewById(R.id.textview1);
        lati = location.getLatitude();
        longi = location.getLongitude();
        mMarker.setPosition(new LatLng(lati, longi));
        txtLat.setText("Latitude:" + lati + ", Longitude:" + longi);
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
}