package com.example.micycle.models;

import com.google.firebase.firestore.DocumentId;
import java.util.Date;

public class Message {

    @DocumentId
    private String messageId;
    private String userId; // Recipient User ID
    private String adminId; // Admin User ID (Sender)
    private String actionType; // e.g., "cycle_edited", "cycle_removed", "user_blocked", "user_unblocked"
    private String relatedItemId; // ID of the affected item (cycleId or userId)
    private String relatedItemName; // Name of the related item (e.g., cycle model, user name)
    private String messageContent; // The message from the admin
    private Date timestamp;

    public Message() {
        // Required public no-argument constructor for Firestore
    }

    // Constructor with messageContent and timestamp (for older messages or if relatedItemName is not applicable)
    public Message(String userId, String adminId, String actionType, String relatedItemId, String messageContent, Date timestamp) {
        this.userId = userId;
        this.adminId = adminId;
        this.actionType = actionType;
        this.relatedItemId = relatedItemId;
        this.messageContent = messageContent;
        this.timestamp = timestamp;
        this.relatedItemName = null; // Initialize relatedItemName to null for this constructor
    }

    // Constructor with relatedItemName, messageContent, and timestamp (for newer messages)
    public Message(String userId, String adminId, String actionType, String relatedItemId, String relatedItemName, String messageContent, Date timestamp) {
        this.userId = userId;
        this.adminId = adminId;
        this.actionType = actionType;
        this.relatedItemId = relatedItemId;
        this.relatedItemName = relatedItemName;
        this.messageContent = messageContent;
        this.timestamp = timestamp;
    }

    // Getters (required for Firestore)
    public String getMessageId() { return messageId; }
    public String getUserId() { return userId; }
    public String getAdminId() { return adminId; }
    public String getActionType() { return actionType; }
    public String getRelatedItemId() { return relatedItemId; }
    public String getRelatedItemName() { return relatedItemName; }
    public String getMessageContent() { return messageContent; }
    public Date getTimestamp() { return timestamp; }

    // Setters (optional)
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public void setRelatedItemId(String relatedItemId) { this.relatedItemId = relatedItemId; }
    public void setRelatedItemName(String relatedItemName) { this.relatedItemName = relatedItemName; }
    public void setMessageContent(String messageContent) { this.messageContent = messageContent; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}
