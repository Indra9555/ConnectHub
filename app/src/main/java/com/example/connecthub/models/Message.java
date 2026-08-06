package com.example.connecthub.models;


import android.net.Uri;

import java.util.Map;

public class Message {
    private String senderName = "";

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
    private String voiceUrl = "";
    private long voiceDuration = 0;
    private java.util.Map<String, String> reactions;
    private java.util.List<Integer> waveform;
    private String groupId = "";
    private Map<String, Long> readBy;

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

    public java.util.Map<String, String> getReactions() {
        return reactions;
    }

    public void setReactions(java.util.Map<String, String> reactions) {
        this.reactions = reactions;
    }
    public String getVoiceUrl() {
        return voiceUrl;
    }

    public void setVoiceUrl(String voiceUrl) {
        this.voiceUrl = voiceUrl;
    }

    public long getVoiceDuration() {
        return voiceDuration;
    }

    public void setVoiceDuration(long voiceDuration) {
        this.voiceDuration = voiceDuration;
    }
    public java.util.List<Integer> getWaveform() {
        return waveform;
    }
    public void setWaveform(java.util.List<Integer> waveform) {
        this.waveform = waveform;
    }

    public String getReactionSummary() {

        if (reactions == null || reactions.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        for (String reaction : reactions.values()) {
            builder.append(reaction);
        }

        return builder.toString();
    }
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    public Map<String, Long> getReadBy() {
        return readBy;
    }
    public void setReadBy(Map<String, Long> readBy) {
        this.readBy = readBy;
    }
}

