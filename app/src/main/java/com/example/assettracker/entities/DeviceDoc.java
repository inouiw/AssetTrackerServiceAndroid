package com.example.assettracker.entities;

import com.google.firebase.Timestamp;

public class DeviceDoc {
    private String manufacturer;
    private String model;
    private int api;
    private Timestamp firstLoginAt = Timestamp.now();

    public DeviceDoc(String manufacturer, String model, int api) {
        this.manufacturer = manufacturer;
        this.model = model;
        this.api = api;
    }

    public String getManufacturer() { return manufacturer; }

    public String getModel() { return model; }

    public int getApi() { return api; }

    public Timestamp getFirstLoginAt() { return firstLoginAt; }
}
