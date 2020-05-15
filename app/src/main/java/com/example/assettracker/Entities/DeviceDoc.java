package com.example.assettracker.Entities;

import com.google.firebase.Timestamp;

public class DeviceDoc {
    private String manufacturer;
    private String model;
    private int api;
    private Timestamp firstLoginAt;

    public DeviceDoc(String manufacturer, String model, int api) {
        this.manufacturer = manufacturer;
        this.model = model;
        this.api = api;
        this.firstLoginAt = Timestamp.now();
    }

    public String getManufacturer() { return manufacturer; }

    public String getModel() { return model; }

    public int getApi() { return api; }

    public Timestamp getFirstLoginAt() { return firstLoginAt; }
}
