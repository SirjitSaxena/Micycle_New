package com.example.micycle.models;

import com.google.firebase.firestore.DocumentId; // Optional: Import DocumentId for Firestore document ID

import java.util.Date; // Import Date

public class User {

    @DocumentId // Use this annotation if you want Firestore to automatically populate this field with the document ID
    private String userId; // Firebase Auth UID will be the document ID
    private String email;
    private String name;
    private String phoneNumber; // Optional
    private String userType; // "regular" or "admin"
    private Date createdAt;
    private Date updatedAt; // Optional
    private boolean blocked; // Use 'blocked' field instead of 'isBlocked'

    // Required public no-argument constructor for Firestore
    public User() {
    }

    // Constructor for creating a new User object
    public User(String userId, String email, String name, String phoneNumber, String userType, Date createdAt) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.userType = userType;
        this.createdAt = createdAt;
        this.updatedAt = createdAt; // Initially same as createdAt
        this.blocked = false; // Initialize 'blocked' field
    }

    // Getters (required for Firestore)
    public String getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getUserType() { return userType; }
    public Date getCreatedAt() { return createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public boolean getBlocked() { return blocked; } // Getter for 'blocked' field

    // Setters (optional, but useful for updating data)
    public void setUserId(String userId) { this.userId = userId; } // If not using @DocumentId
    public void setEmail(String email) { this.email = email; }
    public void setName(String name) { this.name = name; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setUserType(String userType) { this.userType = userType; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; } // Setter for 'blocked' field
}
