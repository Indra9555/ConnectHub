package com.example.connecthub.models;

public class Notification {

    private String senderId;
    private String receiverId;
    private String type;
    private String postId;
    private String message;
    private long timestamp;
    private boolean seen;

    public Notification() {
        // Required empty constructor for Firestore
    }

    public Notification(String senderId,
                        String receiverId,
                        String type,
                        String postId,
                        String message,
                        long timestamp) {

        this.senderId = senderId;
        this.receiverId = receiverId;
        this.type = type;
        this.postId = postId;
        this.message = message;
        this.timestamp = timestamp;
        this.seen = false;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
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
}