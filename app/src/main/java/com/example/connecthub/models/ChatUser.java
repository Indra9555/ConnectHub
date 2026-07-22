package com.example.connecthub.models;

public class ChatUser {

    private String uid;
    private String name;
    private String image;
    private String lastMessage;
    private long lastTimestamp;
    private int unreadCount;

    public ChatUser() {
    }

    public ChatUser(String uid,
                    String name,
                    String image,
                    String lastMessage,
                    long lastTimestamp) {

        this.uid = uid;
        this.name = name;
        this.image = image;
        this.lastMessage = lastMessage;
        this.lastTimestamp = lastTimestamp;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getLastTimestamp() {
        return lastTimestamp;
    }

    public void setLastTimestamp(long lastTimestamp) {
        this.lastTimestamp = lastTimestamp;
    }
    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}