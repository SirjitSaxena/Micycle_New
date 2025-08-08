package com.example.micycle.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Review {

    @DocumentId
    private String reviewId; // Firestore document ID for the review
    private String cycleId;    // ID of the cycle being reviewed
    private String bookingId;  // ID of the booking this review is for
    private String reviewerId; // ID of the user who left the review
    private double rating;     // The rating (e.g., 1.0 to 5.0)
    private String reviewText; // The text of the review (optional)
    @ServerTimestamp
    private Date createdAt;    // Timestamp when the review was created

    // Required public no-argument constructor for Firestore
    public Review() {
    }

    // Constructor for creating a new Review object
    public Review(String cycleId, String bookingId, String reviewerId, double rating, String reviewText) {
        this.cycleId = cycleId;
        this.bookingId = bookingId;
        this.reviewerId = reviewerId;
        this.rating = rating;
        this.reviewText = reviewText;
        // createdAt will be set by @ServerTimestamp
    }

    // Getters (required for Firestore)
    public String getReviewId() { return reviewId; }
    public String getCycleId() { return cycleId; }
    public String getBookingId() { return bookingId; }
    public String getReviewerId() { return reviewerId; }
    public double getRating() { return rating; }
    public String getReviewText() { return reviewText; }
    public Date getCreatedAt() { return createdAt; }

    // Setters (optional, but useful)
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }
    public void setCycleId(String cycleId) { this.cycleId = cycleId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }
    public void setRating(double rating) { this.rating = rating; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
