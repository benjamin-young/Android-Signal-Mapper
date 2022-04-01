package com.example.signalmapping;

import java.util.ArrayList;

public class EmfGlobalMap {

    private final double[][] emfMap;
    private double maxReading = 0;

    public EmfGlobalMap(ArrayList<Reading> readings) {
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

        // create map with required bounds
        emfMap = new double[xBound + 1][yBound + 1];

        // iterate again to create map contents
        for (Reading r:
             readings) {
            emfMap[r.getX()][r.getY()] = r.getMagH();
            if (r.getMagH() > maxReading) {
                maxReading = r.getMagH();
            }
        }

        // normalise map contents by dividing through by max
        // !!! assumes no duplicate readings (i.e. same coordinates) !!!
        for (Reading r:
                readings) {
            emfMap[r.getX()][r.getY()] = emfMap[r.getX()][r.getY()]/maxReading;
        }
    }

    public double[][] getEmfMap() {
        return emfMap;
    }

    public double getEmfMapEntry(int x, int y) {
        return emfMap[x][y];
    }

    public double getMaxReading() {
        return maxReading;
    }
}
