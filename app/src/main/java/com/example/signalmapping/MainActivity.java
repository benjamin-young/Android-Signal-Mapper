package com.example.signalmapping;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
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
import com.google.gson.Gson;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
    private TextView predicted_coordinates;
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

    EmfGlobalMap emfGlobalMap;
    WifiGlobalMap wifiGlobalMap;
    LocationPrediction locationPrediction;
    private boolean predictFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button1 = (Button) findViewById(R.id.button);
        emfText = (TextView) findViewById(R.id.emfText);
        X = (EditText) findViewById(R.id.editTextNumberDecimalX);
        Y = (EditText) findViewById(R.id.editTextNumberDecimalY);
        tv_compass = (TextView) findViewById(R.id.tv_compass);
        predicted_coordinates = (TextView) findViewById(R.id.predicted_coordinates);
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

        // -----
        // begin predictor code
        // -----
        AssetManager assetManager = this.getAssets();
        InputStream fileIn = null;
        try {
            fileIn = assetManager.open("new-signalmapping-default-rtdb-export.json");
            System.out.println(fileIn);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        try (Reader reader = new InputStreamReader(fileIn)) {
            // extract json info into Map, before putting contents in Reading objects
            Map<String, Map> inputJson = new Gson().fromJson(reader, Map.class);
            ArrayList<Reading> readings = new ArrayList<>();

            for (var key:
                    inputJson.keySet()) {
                // split key into components and assign to object variables
                String[] keyParts = key.toString().split(",");
                int x = Integer.parseInt(keyParts[0]);
                int y = Integer.parseInt(keyParts[1]);
                String building = keyParts[2];

                /* depending on building location, add adjustment for global coordinates
                   buildings are:
                   "enginn", "sanderson", "fj", "corridor_sanderson", "corridor_fj", "tlg"
                 */
                if (building.equals("enginn")) {
                    // add adjustment of 0 on x-axis and +10 on y-axis
                    y += 10;
                } else if (building.equals("corridor_sanderson")) {
                    // add adjustment of +3 on x-axis and +10 on y-axis
                    x += 3;
                    y += 10;
                } else if (building.equals("corridor_fj")) {
                    // add adjustment of 0 on x-axis and +21 on y-axis
                    y += 21;
                } else if (building.equals("sanderson")) {
                    // add adjustment of +12 on x-axis and -1 adjustment to y-axis
                    x += 12;
                    y -= 1;
                } else if (building.equals("fj")) {
                    // no adjustment on x-axis and (12 - y) + 34 adjustment to y-axis (to flip inverted y-axis)
                    y = (12 - y) + 34;
                    // adjust
                } else if (building.equals("tlg")) {
                    // add adjustment of +1 on x-axis and +43 on y-axis
                    x += 17;
                    y += 41;
                }

                // get value and assign to object variables
                Map<String, Object> inside = inputJson.get(key);

                // get scan readings
                ArrayList<String> scanResults = new ArrayList<>();
                scanResults.addAll((ArrayList<String>) inside.get("scanResults"));

                // get mag readings
                double magH = (Double) inside.get("magH");
                double magX = (Double) inside.get("magX");
                double magY = (Double) inside.get("magY");
                double magZ = (Double) inside.get("magZ");

                // construct object for reading
                Reading instance = new Reading(x, y, building, magH, magX, magY, magZ, scanResults);
                readings.add(instance);
            }

            // generate wifi and emf maps
            emfGlobalMap = new EmfGlobalMap(readings);
            wifiGlobalMap = new WifiGlobalMap(readings);

            // create location prediction object
            locationPrediction = new LocationPrediction(wifiGlobalMap, emfGlobalMap);

        } catch (IOException e) {
            e.printStackTrace();
        }
        // -----
        // end predictor code
        // -----
    }

    public void predictOnClick(View view) {
        predictFlag = true;
        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
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

            lv=(ListView) findViewById(R.id.listView);
            lv.setAdapter(new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1,wifiString));

            // do prediction related activities
            if (predictFlag) {
                TreeMap<Integer, String> sortedMap = new TreeMap<>();
                ArrayList<String> sortedScan = new ArrayList<>();

                // get the strongest access points by sorting wifi scan
                for (String scan:
                     scanResults) {
                    String[] scanParts = scan.split(", ");
                    sortedMap.put(Integer.parseInt(scanParts[2]), scan);
                }

                // get the 3 strongest signals and put in an array list
                // first check keyset is large enough
                int keySetSize = sortedMap.keySet().size();
                if (keySetSize > 5) {
                    keySetSize = 5;
                }

                Set<Integer> keySetSorted = sortedMap.descendingKeySet();

                for (int i = 0; i < keySetSize; i++) {
                    int key = (int) keySetSorted.toArray()[i];
                    sortedScan.add(sortedMap.get(key));
                }

                // send to wifi predictor
                HashMap<Integer, ArrayList<LocationPrediction.Coordinates>> lp = locationPrediction.getClosestAccessPoints(sortedScan);

                // use emf to predict coordinates
                LocationPrediction.Coordinates outputPrediction = locationPrediction.getLocationPrediction(h, lp, emfGlobalMap.getMaxReading());
                predicted_coordinates.setText("predicted coordinates: (" + outputPrediction.getX() + ", " + outputPrediction.getY() + ")");

                // return without sending data to database
                predictFlag = false;
                return;
            }

            DatabaseReference myRef = database.getReference(X.getText().toString()+","+Y.getText().toString()+"," + area);
            myRef.setValue(scan);
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