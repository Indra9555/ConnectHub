package com.example.connecthub.models;

public class GroupMemberInfo {

    private long joinedAt;
    private Long leftAt;
    private boolean active;

    public GroupMemberInfo() {
    }

    public GroupMemberInfo(long joinedAt,
                           Long leftAt,
                           boolean active) {

        this.joinedAt = joinedAt;
        this.leftAt = leftAt;
        this.active = active;
    }

    public long getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(long joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Long getLeftAt() {
        return leftAt;
    }

    public void setLeftAt(Long leftAt) {
        this.leftAt = leftAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}