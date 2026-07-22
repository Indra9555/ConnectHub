package com.example.connecthub.models;

public class Post {

    private String postId;
    private String uid;
    private String name;
    private String username;
    private String userImage;
    private String content;
    private String postImage;

    private long timestamp;

    private int likesCount;
    private int commentsCount;


    // Required for Firestore
    public Post() {
    }

    public Post(String postId,
                String uid,
                String name,
                String username,
                String userImage,
                String content,
                String postImage,
                long timestamp,
                int likesCount,
                int commentsCount) {

        this.postId = postId;
        this.uid = uid;
        this.name = name;
        this.username = username;
        this.userImage = userImage;
        this.content = content;
        this.postImage = postImage;
        this.timestamp = timestamp;
        this.likesCount = likesCount;
        this.commentsCount = commentsCount;
    }

    public String getPostId() {
        return postId;
    }

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public String getUserImage() {
        return userImage;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getLikesCount() {
        return likesCount;
    }

    public int getCommentsCount() {
        return commentsCount;

    }
    public String getPostImage() {
        return postImage;
    }
    public void setPostImage(String postImage) {
        this.postImage = postImage;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setUserImage(String userImage) {
        this.userImage = userImage;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setLikesCount(int likesCount) {
        this.likesCount = likesCount;
    }

    public void setCommentsCount(int commentsCount) {
        this.commentsCount = commentsCount;
    }
}