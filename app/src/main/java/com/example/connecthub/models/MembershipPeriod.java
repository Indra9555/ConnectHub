package com.example.connecthub.models;

public class MembershipPeriod {

    private long joinedAt;
    private Long leftAt;

    public MembershipPeriod() {
    }

    public MembershipPeriod(long joinedAt, Long leftAt) {
        this.joinedAt = joinedAt;
        this.leftAt = leftAt;
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
}
