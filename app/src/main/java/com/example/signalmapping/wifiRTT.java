package com.example.signalmapping;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.rtt.RangingRequest;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class wifiRTT extends AppCompatActivity{

    private WifiManager wifiManager;
    private WifiScanReceiver wifiScanReceiver;

    private TextView outputTextView;

    List<ScanResult> mAccessPoints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_rtt);

        outputTextView = findViewById(R.id.textView);

        mAccessPoints = new ArrayList<>();

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiScanReceiver = new WifiScanReceiver();
        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(wifiScanReceiver);
    }

    private class WifiScanReceiver extends BroadcastReceiver {
        private List<ScanResult> find80211mcSupportedAccessPoints(@NonNull List<ScanResult> originalList) {
            List<ScanResult> newList = new ArrayList<>();
            for (ScanResult scanResult : originalList) {
                if (scanResult.is80211mcResponder()) {
                    newList.add(scanResult);
                }
                if (newList.size() >= RangingRequest.getMaxPeers()) {
                    break;
                }
            }
            Toast.makeText(wifiRTT.this, newList.toString(), Toast.LENGTH_SHORT).show();
            return newList;
        }

        public void onReceive(Context context, Intent intent) {
            List<ScanResult> scanResults = wifiManager.getScanResults();

            if (scanResults != null) {
                mAccessPoints = find80211mcSupportedAccessPoints(scanResults);
                outputTextView.setText(scanResults.size() + " APs discovered, " + mAccessPoints.size() + " RTT capable.");
            }
        }
    }


}