package com.example.connecthub.models;


import android.net.Uri;

public class Message {

    private String senderId;
    private String receiverId;
    private String messageId="";
    private String message;
    private long timestamp;
    private boolean seen;
    private String imageUrl;
    private String type;
    private boolean uploading;
    private transient Uri localImageUri;
    private String replyMessage = "";
    private String replySender = "";
    private String replyType = "";
    private String replyMessageId = "";
    private String replyImageUrl = "";
    private boolean deleted = false;


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
    public String getReplyMessage() {
        return replyMessage;
    }

    public void setReplyMessage(String replyMessage) {
        this.replyMessage = replyMessage;
    }

    public String getReplySender() {
        return replySender;
    }

    public void setReplySender(String replySender) {
        this.replySender = replySender;
    }

    public String getReplyType() {
        return replyType;
    }

    public void setReplyType(String replyType) {
        this.replyType = replyType;
    }
    public String getReplyMessageId() {
        return replyMessageId;
    }

    public void setReplyMessageId(String replyMessageId) {
        this.replyMessageId = replyMessageId;
    }
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    public String getReplyImageUrl() {
        return replyImageUrl;
    }

    public void setReplyImageUrl(String replyImageUrl) {
        this.replyImageUrl = replyImageUrl;
    }
    public boolean isDeleted() {
        return deleted;
    }
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
