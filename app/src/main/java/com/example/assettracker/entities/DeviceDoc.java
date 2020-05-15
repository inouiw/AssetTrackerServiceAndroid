package com.example.assettracker.entities;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

public class DeviceDoc {
    private String deviceId;
    private String manufacturer;
    private String model;
    private int api;
    @ServerTimestamp
    private Timestamp firstLoginAt;

    public DeviceDoc(String deviceId, String manufacturer, String model, int api) {
        this.deviceId = deviceId;
        this.manufacturer = manufacturer;
        this.model = model;
        this.api = api;
    }

    public String getDeviceId() { return deviceId; }

    public String getManufacturer() { return manufacturer; }

    public String getModel() { return model; }

    public int getApi() { return api; }

    public Timestamp getFirstLoginAt() { return firstLoginAt; }
}
