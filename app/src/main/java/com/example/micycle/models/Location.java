package com.example.micycle.models;

public class Location {
    private String locationId;
    private String name;

    // Default constructor required for calls to DataSnapshot.getValue(Location.class)
    public Location() {
    }

    public Location(String locationId, String name) {
        this.locationId = locationId;
        this.name = name;
    }

    // Getters
    public String getLocationId() {
        return locationId;
    }

    public String getName() {
        return name;
    }

    // Setters (optional, but good practice)
    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public void setName(String name) {
        this.name = name;
    }
} 