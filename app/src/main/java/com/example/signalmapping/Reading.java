package com.example.signalmapping;

import java.util.ArrayList;

public class Reading {
    private int x;
    private int y;
    private String direction;
    private double magH;
    private double magX;
    private double magY;
    private double magZ;
    private ArrayList<String> scanResults;

    public Reading(int x, int y, String direction, double magH, double magX, double magY, double magZ, ArrayList<String> scanResults) {
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.magH = magH;
        this.magX = magX;
        this.magY = magY;
        this.magZ = magZ;
        this.scanResults = scanResults;
    }

    public void getReading() {
        System.out.println("--- Start reading");
        System.out.println("x: " + x);
        System.out.println("y: " + y);
        System.out.println("direction: " + direction);
        System.out.println("magH: " + magH);
        System.out.println("magX: " + magX);
        System.out.println("magY: " + magY);
        System.out.println("magZ: " + magZ);
        System.out.println("scanResults:");
        for (String s:
             scanResults) {
            System.out.println(s);
        }
        System.out.println("--- End reading");
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public double getMagH() {
        return this.magH;
    }

    public ArrayList<String> getScanResults() {
        return scanResults;
    }
}
