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
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int REQUEST_ID_READ_WRITE_PERMISSION = 99;

    WifiManager wifiManager;

    String wifiString[];
    ListView lv;
    Button button1;
    EditText X;
    EditText Y;

    private SensorManager mSensorManager;
    private Sensor mMagneticField;
    private TextView emfText;
    private double h;

    FirebaseDatabase database;
    public Scan scan;

    private class Scan {
        public float magX;
        public float magY;
        public float magZ;
        public double magH;
        public List<String> scanResults;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button1 = (Button) findViewById(R.id.button);
        emfText = (TextView) findViewById(R.id.emfText);
        X = (EditText) findViewById(R.id.editTextNumberDecimalX);
        Y = (EditText) findViewById(R.id.editTextNumberDecimalY);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        scan = new Scan();

        askWifiPermission();

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        //check wifi state
        if (wifiManager.getWifiState() == wifiManager.WIFI_STATE_DISABLED) {
            wifiManager.setWifiEnabled(true);
        }

        database = FirebaseDatabase.getInstance();

        //registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        //wifiManager.startScan();
    }

    public void scan(View view){
        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
    }

    public void wifiRTT(View view){
        Intent intent = new Intent(this, wifiRTT.class);
        startActivity(intent);
    }

    @Override
    protected void onResume(){
        //registerReceiver(wifiScanReceiver,new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mSensorManager.registerListener(this,mMagneticField,SensorManager.SENSOR_DELAY_NORMAL);

        super.onResume();
    }
    @Override
    protected void onPause(){
        unregisterReceiver(wifiScanReceiver);
        mSensorManager.unregisterListener(this);

        super.onPause();
    }

    public BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, "Scanning Wifi", Toast.LENGTH_SHORT).show();
            List<ScanResult> wifiScanList = wifiManager.getScanResults();
            unregisterReceiver(this);

            List<String> scanResults = new ArrayList<String>();

            wifiString = new String[wifiScanList.size()];
            Log.e("Wifi", String.valueOf(wifiScanList.size()));

            for (int i = 0; i<wifiScanList.size(); i++){
                wifiString[i] = wifiScanList.get(i).SSID + ", " + wifiScanList.get(i).BSSID + ", " + String.valueOf(wifiScanList.get(i).level);
                Log.e("Wifi",String.valueOf(wifiString[i]));
                scanResults.add(wifiString[i]);
            }

            scan.scanResults = scanResults;

            //Time time = new Time();
            //time.setToNow();
            DatabaseReference myRef = database.getReference(X.getText().toString()+","+Y.getText().toString());
            myRef.setValue(scan);


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
                        "mag_Yaxis: " + sensorEvent.values[1] + "\n" +
                        "mag_Zaxis: " + sensorEvent.values[2] + "\n" +
                        "magnitude: " + h + "\n");

        scan.magX = sensorEvent.values[0];
        scan.magY = sensorEvent.values[1];
        scan.magZ = sensorEvent.values[2];
        scan.magH = h;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}