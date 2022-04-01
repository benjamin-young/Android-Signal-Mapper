package com.example.signalmapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WifiGlobalMap {

    private final HashMap<String, int[][]> wifiMap = new HashMap<>();

    public WifiGlobalMap(ArrayList<Reading> readings) {
        int yBound = 0;
        int xBound = 0;

        // iterate each reading to determine bounds of map
        for (Reading r:
                readings) {

            // check x
            if (r.getX() > xBound) {
                xBound = r.getX();
            }

            // check y
            if (r.getY() > yBound) {
                yBound = r.getY();
            }
        }

        // for each reading, check MAC address of scanResults
        // first filter out WiFi that isn't eduroam or central
        // if new, add to map and update coordinates
        // else, just update coordinates
        for (Reading r:
             readings) {

            for (String s:
                 r.getScanResults()) {

                // split scan entry
                String[] scanParts = s.split(", ");

                // filter out non eduroam and central WAPs
                if (scanParts[0].equals("eduroam") || scanParts[0].equals("central")) {
                    // if already in map, get value
                    // else create 2d int array
                    int[][] t;
                    if (wifiMap.containsKey(scanParts[1])) {
                        t = wifiMap.get(scanParts[1]);
                    } else {
                        t = new int[xBound + 1][yBound + 1];
                    }

                    // then update array
                    t[r.getX()][r.getY()] = Integer.parseInt(scanParts[2]);
                    wifiMap.put(scanParts[1], t);
                }
            }
        }
    }


    public HashMap<String, int[][]> getWifiMap() {
        return wifiMap;
    }
}
