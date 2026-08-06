package com.example.connecthub.models;

import java.util.List;
import java.util.Map;

public class MembershipPeriod {

    private long joinedAt;
    private Long leftAt;
    private Map<String, List<MembershipPeriod>> memberHistory;

    public MembershipPeriod() {
    }
    public Map<String, List<MembershipPeriod>> getMemberHistory() {
        return memberHistory;
    }

    public void setMemberHistory(Map<String, List<MembershipPeriod>> memberHistory) {
        this.memberHistory = memberHistory;
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