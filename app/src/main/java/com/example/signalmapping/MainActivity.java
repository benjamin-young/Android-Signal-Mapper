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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener, AdapterView.OnItemSelectedListener {

    private static final int REQUEST_ID_READ_WRITE_PERMISSION = 99;

    WifiManager wifiManager;

    String wifiString[];
    ListView lv;
    Button button1;
    EditText X;
    EditText Y;
    Spinner spinner;

    private SensorManager mSensorManager;
    private Sensor mMagneticField;
    private Sensor rotationSensor;
    private TextView emfText;
    private TextView tv_compass;
    private double h;

    private final float[] rotationMatrix = new float[16];
    private final float[] radianValues = new float[3];
    private final double[] degreeValues = new double[3];

    String[] areas = { "enginn", "corridor_sanderson", "corridor_fj", "sanderson", "fj", "tlg", "test"};

    FirebaseDatabase database;
    public Scan scan;
    public String area;

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
        tv_compass = (TextView) findViewById(R.id.tv_compass);
        spinner = (Spinner) findViewById(R.id.spinner);

        //Creating the ArrayAdapter instance having the country list
        ArrayAdapter aa = new ArrayAdapter(this,android.R.layout.simple_spinner_item, areas);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //Setting the ArrayAdapter data on the Spinner
        spinner.setAdapter(aa);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        rotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        spinner.setOnItemSelectedListener(this);

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


    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        area = (String) adapterView.getItemAtPosition(i);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

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
        mSensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_NORMAL);
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
            DatabaseReference myRef = database.getReference(X.getText().toString()+","+Y.getText().toString()+"," + area);
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

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, sensorEvent.values);
            SensorManager.getOrientation(rotationMatrix, radianValues);
            degreeValues[0] = Math.toDegrees(radianValues[0]);
            degreeValues[1] = Math.toDegrees(radianValues[1]);
            degreeValues[2] = Math.toDegrees(radianValues[2]);

            Log.e("X angle", Double.toString(degreeValues[0]));
            Log.e("Y angle", Double.toString(degreeValues[1]));
            Log.e("Z angle", Double.toString(degreeValues[2]));

            // Determine compass direction
            // TODO: Change string to resources
            /*
            if (degreeValues[0] >= -22.5 && degreeValues[0] < 22.5) {
                tv_compass.setText("North");
            } else if (degreeValues[0] >= -67.5 && degreeValues[0] < -22.5) {
                tv_compass.setText("North-West");
            } else if (degreeValues[0] >= -112.5 && degreeValues[0] < -67.5) {
                tv_compass.setText("West");
            } else if (degreeValues[0] >= -157.5 && degreeValues[0] < -112.5) {
                tv_compass.setText("South-West");
            } else if (degreeValues[0] >= 157.5 || degreeValues[0] < -157.5) {
                tv_compass.setText("South");
            } else if (degreeValues[0] >= 22.5 && degreeValues[0] < 67.5) {
                tv_compass.setText("North-East");
            } else if (degreeValues[0] >= 67.5 && degreeValues[0] < 112.5) {
                tv_compass.setText("East");
            } else {
                tv_compass.setText("South-East");
            }
             */
            tv_compass.setText(Double.toString(degreeValues[0]));
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
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
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}