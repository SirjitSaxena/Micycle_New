package com.example.micycle.models;

import java.util.List;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date; // Using java.util.Date for timestamp
import com.google.firebase.firestore.Exclude; // Import Exclude

public class Cycle {
    private String cycleId;
    private String ownerId;
    private String model;
    private String color;
    private String description;
    private double pricePerHour; // Using double for price
    private String locationId;
    private List<String> imageUrls; // Assuming a list of image URLs
    private String availabilityStatus;
    private Date bookedUntilTimestamp; // Field to store the booking end timestamp
    private double averageRating;
    private int numberOfReviews;

    // Transient field for location name (not stored in Firestore)
    @Exclude
    private String locationName;

    // Timestamps and ratings can be added later if needed for the list view,
    // but are not strictly required for the basic list item display.

    // Constructor (can be empty for now, or add necessary fields)
    public Cycle() {
        // Default constructor required for calls to DataSnapshot.getValue(Cycle.class)
    }

    public Cycle(String cycleId, String ownerId, String model, String color, String description, double pricePerHour, String locationId, List<String> imageUrls, String availabilityStatus, Date bookedUntilTimestamp) {
        this.cycleId = cycleId;
        this.ownerId = ownerId;
        this.model = model;
        this.color = color;
        this.description = description;
        this.pricePerHour = pricePerHour;
        this.locationId = locationId;
        this.imageUrls = imageUrls;
        this.availabilityStatus = availabilityStatus;
        this.bookedUntilTimestamp = bookedUntilTimestamp;
        this.averageRating = 0.0;
        this.numberOfReviews = 0;
    }

    // Getters (needed to access the data)
    public String getCycleId() { return cycleId; }
    public String getOwnerId() { return ownerId; }
    public String getModel() { return model; }
    public String getColor() { return color; }
    public String getDescription() { return description; }
    public double getPricePerHour() { return pricePerHour; }
    public String getLocationId() { return locationId; }
    public List<String> getImageUrls() { return imageUrls; }
    public String getAvailabilityStatus() { return availabilityStatus; }
    public Date getBookedUntilTimestamp() { return bookedUntilTimestamp; }
    public double getAverageRating() { return averageRating; }
    public int getNumberOfReviews() { return numberOfReviews; }
    // Getter for transient location name
    @Exclude
    public String getLocationName() { return locationName; }

    // Setters (optional, depending on how data is loaded/updated)
    public void setCycleId(String cycleId) { this.cycleId = cycleId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public void setModel(String model) { this.model = model; }
    public void setColor(String color) { this.color = color; }
    public void setDescription(String description) { this.description = description; }
    public void setPricePerHour(double pricePerHour) { this.pricePerHour = pricePerHour; }
    public void setLocationId(String locationId) { this.locationId = locationId; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public void setAvailabilityStatus(String availabilityStatus) { this.availabilityStatus = availabilityStatus; }
    public void setBookedUntilTimestamp(Date bookedUntilTimestamp) { this.bookedUntilTimestamp = bookedUntilTimestamp; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    public void setNumberOfReviews(int numberOfReviews) { this.numberOfReviews = numberOfReviews; }
    // Setter for transient location name
    @Exclude
    public void setLocationName(String locationName) { this.locationName = locationName; }
}
