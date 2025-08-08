package com.example.micycle.models;

import java.util.Date; // Import Date

public class Transaction {
    private String bookingId;
    private String cycleId;
    private String userId;
    // private String ownerId; // Might not be needed for user's transaction history view
    private Date startTime; // Use Date for timestamps
    private Date endTime;
    private Date actualReturnTime; // Can be null for active bookings
    private double totalCost;
    private String bookingStatus; // e.g., "completed", "active", "cancelled"
    // private String paymentStatus; // Can add if needed in the list view
    private Date createdAt;
    // private Date updatedAt; // Can add if needed

    // Optional: Field to store cycle model name for easier display
    private String cycleModelName;

    // Constructor (can be empty for Firebase deserialization)
    public Transaction() {
    }

    // Constructor with key fields for history display
    public Transaction(String bookingId, String cycleId, String userId, Date startTime, Date endTime, double totalCost, String bookingStatus, Date createdAt) {
        this.bookingId = bookingId;
        this.cycleId = cycleId;
        this.userId = userId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalCost = totalCost;
        this.bookingStatus = bookingStatus;
        this.createdAt = createdAt;
        // actualReturnTime is null initially, cycleModelName set separately
    }

    // Getters
    public String getBookingId() { return bookingId; }
    public String getCycleId() { return cycleId; }
    public String getUserId() { return userId; }
    public Date getStartTime() { return startTime; }
    public Date getEndTime() { return endTime; }
    public Date getActualReturnTime() { return actualReturnTime; }
    public double getTotalCost() { return totalCost; }
    public String getBookingStatus() { return bookingStatus; }
    public Date getCreatedAt() { return createdAt; }
    public String getCycleModelName() { return cycleModelName; }

    // Setters (can add if needed)
    public void setActualReturnTime(Date actualReturnTime) { this.actualReturnTime = actualReturnTime; }
    public void setCycleModelName(String cycleModelName) { this.cycleModelName = cycleModelName; }
    // Add setters for other fields if necessary
}
