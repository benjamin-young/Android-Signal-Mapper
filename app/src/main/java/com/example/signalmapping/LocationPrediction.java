package com.example.signalmapping;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LocationPrediction {

    private WifiGlobalMap wifiMap;
    private EmfGlobalMap emfMap;
    private int threshold = 5;

    public LocationPrediction(WifiGlobalMap wifiMap, EmfGlobalMap emfMap){
        this.wifiMap = wifiMap;
        this.emfMap = emfMap;
    }

    public Coordinates maxLocation(String accessPoint){
        int[][] map = wifiMap.getWifiMap().get(accessPoint);

        int max = -100;
        int x = 0;
        int y = 0;

        for(int i = 0; i<map.length; i++){
            for (int j = 0; j<map[0].length; j++){
                if(map[i][j] > max && map[i][j] < 0){
                    max = map[i][j];
                    x = i;
                    y = j;
                }
            }
        }
        Coordinates returnValues = new Coordinates(x,y);
        return returnValues;
    }

    /**
     *
     * @param strongestAccessPoints send this function an array list of the strongest access points from reading
     * @return a map of WAP strength to an array of closest coordinates for closest matches
     */
    public HashMap<Integer, ArrayList<Coordinates>> getClosestAccessPoints(ArrayList<String> strongestAccessPoints) {
        HashMap<Integer, ArrayList<Coordinates>> map = new HashMap<>();

        // iterate access points
        for (String s:
             strongestAccessPoints) {
            ArrayList<Coordinates> coordinates = new ArrayList<>();

            // check if access point is in map
            String[] scanParts = s.split(", ");
            int rcvStrength = Integer.parseInt(scanParts[2]);

            if (wifiMap.getWifiMap().get(scanParts[1]) != null) {
                // find coordinates where strength is most similar
                for (int i = 0; i < wifiMap.getWifiMap().get(scanParts[1]).length; i++) {
                    for (int j = 0; j < wifiMap.getWifiMap().get(scanParts[1])[0].length; j++) {
                        // if within threshold
                        if (wifiMap.getWifiMap().get(scanParts[1])[i][j] > (rcvStrength - threshold) &&
                        wifiMap.getWifiMap().get(scanParts[1])[i][j] < (rcvStrength + threshold)) {
                            coordinates.add(new Coordinates(i, j));
                        }
                    }
                }

                // add coordinates to map
                map.put(rcvStrength, coordinates);
            }
        }

        return map;
    }

    public Coordinates getLocationPrediction(double emfReading, HashMap<Integer, ArrayList<Coordinates>> map, double normaliseVal) {
        // first normalise input reading
        emfReading = emfReading / normaliseVal;

        Coordinates closestCoordinate = new Coordinates(0, 0);
        double dist = 0;
        double newDist;
        // iterate coordinates in emf map, compare with emfReading input, select closest coordinate as return value
        for (Integer i:
             map.keySet()) {
            for (Coordinates c:
                 map.get(i)) {
                double iEmfReading = emfMap.getEmfMap()[c.getX()][c.getY()];
                if (dist == 0) {
                    closestCoordinate = new Coordinates(c.getX(), c.getY());
                    dist = Math.abs(emfReading - iEmfReading);
                } else {
                    newDist = Math.abs(emfReading - iEmfReading);
                    if (newDist < dist) {
                        closestCoordinate = new Coordinates(c.getX(), c.getY());
                        dist = newDist;
                    }
                }
            }
        }

        return closestCoordinate;
    }

    public class Coordinates {
        private int x;
        private int y;

        public Coordinates(int x, int y){
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }
}
