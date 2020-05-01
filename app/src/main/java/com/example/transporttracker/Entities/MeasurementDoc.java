package com.example.transporttracker.Entities;

// The measurement document as stored in firebase.
public class MeasurementDoc {
    private long time = 0;
    private double latitude = 0.0;
    private double longitude = 0.0;
    private float speed = 0.0f;
    // horizontal direction of travel (0.0 - 360.0)
    private float bearing = 0.0f;
    // horizontal accuracy, radio, in meters with 68% confidence
    private float horizontalAccuracyMeters = 0.0f;

    public MeasurementDoc(long time, double latitude, double longitude, float speed, float bearing, float horizontalAccuracyMeters) {
        this.time = time;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.bearing = bearing;
        this.horizontalAccuracyMeters = horizontalAccuracyMeters;
    }

    public long getTime() {
        return time;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public float getSpeed() {
        return speed;
    }

    public float getBearing() {
        return bearing;
    }

    public float getHorizontalAccuracyMeters() {
        return horizontalAccuracyMeters;
    }
}
