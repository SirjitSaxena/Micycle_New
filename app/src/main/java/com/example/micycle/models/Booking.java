package com.example.micycle.models;

import java.util.Date; // Import Date

public class Booking {
    private String bookingId;
    private String cycleId;
    private String userId;
    private String ownerId;
    private Date startTime;
    private Date endTime;
    private double totalCost;
    private String bookingStatus; // e.g., "pending", "active", "completed", "cancelled"
    private String paymentStatus; // e.g., "pending", "paid", "refunded"
    private Date createdAt;
    private Date updatedAt;
    private String cycleName;
    private String ownerName;

    // Transient fields (not stored in Firestore, used for display in history)
    private String transactionType; // "rented" (user is renter) or "lent" (user is owner)
    private String otherUserName; // Name of the other user (owner if rented, renter if lent)
    private boolean isReviewed; // Added to track if the booking is reviewed
    private String reviewId; // Added to store the review document ID

    // Default constructor required for calls to DataSnapshot.getValue(Booking.class)
    public Booking() {
    }

    public Booking(String cycleId, String userId, String ownerId, Date startTime, Date endTime, double totalCost, String bookingStatus, String paymentStatus, Date createdAt, Date updatedAt, String cycleName) {
        this.cycleId = cycleId;
        this.userId = userId;
        this.ownerId = ownerId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalCost = totalCost;
        this.bookingStatus = bookingStatus;
        this.paymentStatus = paymentStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.cycleName = cycleName;
        this.ownerName = null;
        this.transactionType = null;
        this.otherUserName = null;
        this.isReviewed = false; // Initialize
        this.reviewId = null; // Initialize
    }

    // Getters
    public String getBookingId() { return bookingId; }
    public String getCycleId() { return cycleId; }
    public String getUserId() { return userId; }
    public String getOwnerId() { return ownerId; }
    public Date getStartTime() { return startTime; }
    public Date getEndTime() { return endTime; }
    public double getTotalCost() { return totalCost; }
    public String getBookingStatus() { return bookingStatus; }
    public String getPaymentStatus() { return paymentStatus; }
    public Date getCreatedAt() { return createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public String getCycleName() { return cycleName; }
    public String getOwnerName() { return ownerName; }
    public String getTransactionType() { return transactionType; }
    public String getOtherUserName() { return otherUserName; }
    public boolean isReviewed() { return isReviewed; }
    public String getReviewId() { return reviewId; } // Getter for reviewId

    // Setters (optional, but good practice for Firestore object mapping)
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public void setCycleId(String cycleId) { this.cycleId = cycleId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public void setCycleName(String cycleName) { this.cycleName = cycleName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public void setOtherUserName(String otherUserName) { this.otherUserName = otherUserName; }
    public void setReviewed(boolean reviewed) { this.isReviewed = reviewed; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; } // Setter for reviewId
}
