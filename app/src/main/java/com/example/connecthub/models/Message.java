package com.example.connecthub.models;


import android.net.Uri;

public class Message {

    private String senderId;
    private String receiverId;
    private String message;
    private long timestamp;
    private boolean seen;
    private String imageUrl;
    private String type;
    private boolean uploading;
    private transient Uri localImageUri;


    public Message() {
        // Required empty constructor for Firestore
    }

    public Message(String senderId,
                   String receiverId,
                   String message,
                   String imageUrl,
                   String type,
                   long timestamp) {

        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.imageUrl = imageUrl;
        this.type = type;
        this.timestamp = timestamp;
        this.seen = false;
        this.uploading = false;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    public boolean isSeen() {
        return seen;
    }
    public void setSeen(boolean seen) {
        this.seen = seen;
    }
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    public boolean isUploading() {
        return uploading;
    }
    public void setUploading(boolean uploading) {
        this.uploading = uploading;
    }
    public Uri getLocalImageUri() {
        return localImageUri;
    }
    public void setLocalImageUri(Uri localImageUri) {
        this.localImageUri = localImageUri;
    }
}
