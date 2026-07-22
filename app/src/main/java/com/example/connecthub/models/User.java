package com.example.connecthub.models;

public class User {

    private String uid;
    private String name;
    private String username;
    private String email;
    private String bio;
    private String image;
    private boolean online;
    private long lastSeen;

    public User() {
        // Required empty constructor for Firestore
    }

    public User(String uid, String name, String username,
                String email, String bio, String image) {

        this.uid = uid;
        this.name = name;
        this.username = username;
        this.email = email;
        this.bio = bio;
        this.image = image;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }
}