package com.example.connecthub.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.connecthub.models.GroupMemberInfo;
import com.example.connecthub.models.MembershipPeriod;


public class Group {

    private String groupId;
    private Map<String, GroupMemberInfo> memberInfo;
    private String groupName;
    private String groupImage;
    private String createdBy;
    private long createdAt;
    private int membersCount;

    private List<String> members;


    private List<String> admins;

    private String lastMessage;
    private long lastMessageTime;
    private Map<String, List<MembershipPeriod>> memberHistory;


    public Group() {
        members = new ArrayList<>();
        admins = new ArrayList<>();

    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupImage() {
        return groupImage;
    }

    public void setGroupImage(String groupImage) {
        this.groupImage = groupImage;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public List<String> getAdmins() {
        return admins;
    }

    public void setAdmins(List<String> admins) {
        this.admins = admins;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }
    public int getMembersCount() {
        return membersCount;
    }

    public void setMembersCount(int membersCount) {
        this.membersCount = membersCount;
    }
    public Map<String, GroupMemberInfo> getMemberInfo() {
        return memberInfo;
    }

    public void setMemberInfo(Map<String, GroupMemberInfo> memberInfo) {
        this.memberInfo = memberInfo;
    }
    public Map<String, List<MembershipPeriod>> getMemberHistory() {
        return memberHistory;
    }

    public void setMemberHistory(Map<String, List<MembershipPeriod>> memberHistory) {
        this.memberHistory = memberHistory;
    }



}