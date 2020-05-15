package com.example.assettracker.Entities;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.GeoPoint;

// The measurement document as stored in firebase.
public class MeasurementDoc {
    private Timestamp time;
    private GeoPoint geoPoint;
    private float speed;
    // horizontal direction of travel (0.0 - 360.0)
    private float bearing;
    // horizontal accuracy, radio, in meters with 68% confidence
    private float horizontalAccuracyMeters;

    public MeasurementDoc(Timestamp time, GeoPoint geoPoint, float speed, float bearing, float horizontalAccuracyMeters) {
        this.time = time;
        this.geoPoint = geoPoint;
        this.speed = speed;
        this.bearing = bearing;
        this.horizontalAccuracyMeters = horizontalAccuracyMeters;
    }

    public Timestamp getTime() {
        return time;
    }

    public GeoPoint getGeoPoint() {
        return geoPoint;
    }

    public float getSpeed() {
        return speed;
    }

    public float getBearing() { return bearing; }

    public float getHorizontalAccuracyMeters() {
        return horizontalAccuracyMeters;
    }
}
