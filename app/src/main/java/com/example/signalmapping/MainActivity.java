package com.example.signalmapping;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int REQUEST_ID_READ_WRITE_PERMISSION = 99;

    WifiManager wifiManager;
    String wifiString[];
    ListView lv;
    Button button1;

    private SensorManager mSensorManager;
    private Sensor mMagneticField;

    private TextView emfText;


    private double h;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //button1=(Button) findViewById(R.id.button);
        emfText = (TextView) findViewById(R.id.emfText);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        askWifiPermission();

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        //check wifi state
        if(wifiManager.getWifiState() == wifiManager.WIFI_STATE_DISABLED){
            wifiManager.setWifiEnabled(true);
        }

        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();



    }

    protected void onResume(){
        registerReceiver(wifiScanReceiver,new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mSensorManager.registerListener(this,mMagneticField,SensorManager.SENSOR_DELAY_NORMAL);

        super.onResume();
    }

    protected void onPause(){
        unregisterReceiver(wifiScanReceiver);
        mSensorManager.unregisterListener(this);

        super.onPause();
    }

    public BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            List<ScanResult> wifiScanList = wifiManager.getScanResults();
            unregisterReceiver(this);

            wifiString = new String[wifiScanList.size()];
            Log.e("Wifi", String.valueOf(wifiScanList.size()));

            for (int i = 0; i<wifiScanList.size(); i++){
                wifiString[i] = wifiScanList.get(i).SSID + ", " + wifiScanList.get(i).BSSID + ", " + String.valueOf(wifiScanList.get(i).level);
                Log.e("Wifi",String.valueOf(wifiString[i]));
            }
            lv=(ListView) findViewById(R.id.listView);

            lv.setAdapter(new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1,wifiString));
        }
    };

    private void askWifiPermission(){
        if(Build.VERSION.SDK_INT >=23){
            //PERMISSION STATES
            int wifiAccessPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE);
            int wifiChangePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE);
            int courseLocationPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
            int fineLocationPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

            //CHECK IF PERMISSIONS ARE GRANTED
            if(wifiAccessPermission != PackageManager.PERMISSION_GRANTED ||
                    wifiChangePermission != PackageManager.PERMISSION_GRANTED ||
                    courseLocationPermission != PackageManager.PERMISSION_GRANTED ||
                    fineLocationPermission != PackageManager.PERMISSION_GRANTED ){

                //IF YOU DON'T HAVE PERMISSIONS - PROMPT THE USER
                this.requestPermissions(
                        new String[]{
                                Manifest.permission.ACCESS_WIFI_STATE,
                                Manifest.permission.CHANGE_WIFI_STATE,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION
                        },
                        REQUEST_ID_READ_WRITE_PERMISSION
                );
            }
        }
    }

    //WHEN THE USER PERMISSION RESULTS
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case REQUEST_ID_READ_WRITE_PERMISSION: {
                if(grantResults.length>1 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[2] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[3] == PackageManager.PERMISSION_GRANTED){

                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show();
                }
                else{
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        h = Math.sqrt(sensorEvent.values[0] * sensorEvent.values[0] +
                sensorEvent.values[1] * sensorEvent.values[1] +
                sensorEvent.values[2] * sensorEvent.values[2]);

        emfText.setText("mag_Xaxis: " + sensorEvent.values[0] + "\n" +
                        "mag_Xaxis: " + sensorEvent.values[1] + "\n" +
                        "mag_Xaxis: " + sensorEvent.values[2] + "\n" +
                        "magnitude: " + h + "\n");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}