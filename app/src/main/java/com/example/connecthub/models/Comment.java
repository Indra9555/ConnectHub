package com.example.connecthub.models;

public class Comment {

    private String commentId;
    private String uid;
    private String name;
    private String username;
    private String userImage;
    private String comment;
    private long timestamp;

    // Required for Firestore
    public Comment() {
    }

    public Comment(String commentId,
                   String uid,
                   String name,
                   String username,
                   String userImage,
                   String comment,
                   long timestamp) {

        this.commentId = commentId;
        this.uid = uid;
        this.name = name;
        this.username = username;
        this.userImage = userImage;
        this.comment = comment;
        this.timestamp = timestamp;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
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

    public String getUserImage() {
        return userImage;
    }

    public void setUserImage(String userImage) {
        this.userImage = userImage;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
