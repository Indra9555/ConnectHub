package com.example.connecthub.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Group {

    private String groupId;

    private Map<String, GroupMemberInfo> memberInfo;

    private String groupName;
    private String groupImage;

    // Original creator / owner of the group
    private String createdBy;

    private long createdAt;

    private int membersCount;

    // Current group members
    private List<String> members;

    // Current group admins
    private List<String> admins;

    private String lastMessage;
    private long lastMessageTime;

    // User membership history
    private Map<String, List<MembershipPeriod>> memberHistory;


    /*
     * ==========================================
     * CONSTRUCTOR
     * ==========================================
     */

    public Group() {

        members = new ArrayList<>();
        admins = new ArrayList<>();
    }


    /*
     * ==========================================
     * GROUP ID
     * ==========================================
     */

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }


    /*
     * ==========================================
     * GROUP NAME
     * ==========================================
     */

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }


    /*
     * ==========================================
     * GROUP IMAGE
     * ==========================================
     */

    public String getGroupImage() {
        return groupImage;
    }

    public void setGroupImage(String groupImage) {
        this.groupImage = groupImage;
    }


    /*
     * ==========================================
     * GROUP OWNER / CREATOR
     * ==========================================
     */

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }


    /*
     * ==========================================
     * CREATED AT
     * ==========================================
     */

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }


    /*
     * ==========================================
     * MEMBERS
     * ==========================================
     */

    public List<String> getMembers() {

        if (members == null) {
            members = new ArrayList<>();
        }

        return members;
    }

    public void setMembers(List<String> members) {

        this.members =
                members != null
                        ? members
                        : new ArrayList<>();
    }


    /*
     * ==========================================
     * ADMINS
     * ==========================================
     */

    public List<String> getAdmins() {

        if (admins == null) {
            admins = new ArrayList<>();
        }

        return admins;
    }

    public void setAdmins(List<String> admins) {

        this.admins =
                admins != null
                        ? admins
                        : new ArrayList<>();
    }


    /*
     * ==========================================
     * MEMBER COUNT
     * ==========================================
     */

    public int getMembersCount() {
        return membersCount;
    }

    public void setMembersCount(int membersCount) {
        this.membersCount = membersCount;
    }


    /*
     * ==========================================
     * MEMBER INFO
     * ==========================================
     */

    public Map<String, GroupMemberInfo> getMemberInfo() {
        return memberInfo;
    }

    public void setMemberInfo(
            Map<String, GroupMemberInfo> memberInfo
    ) {
        this.memberInfo = memberInfo;
    }


    /*
     * ==========================================
     * MEMBERSHIP HISTORY
     * ==========================================
     */

    public Map<String, List<MembershipPeriod>>
    getMemberHistory() {
        return memberHistory;
    }

    public void setMemberHistory(
            Map<String, List<MembershipPeriod>> memberHistory
    ) {
        this.memberHistory = memberHistory;
    }


    /*
     * ==========================================
     * LAST MESSAGE
     * ==========================================
     */

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }


    /*
     * ==========================================
     * LAST MESSAGE TIME
     * ==========================================
     */

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }


    /*
     * ==========================================
     * PERMISSION HELPERS
     * ==========================================
     */

    /**
     * Checks whether the given user is the
     * original creator / owner of the group.
     */
    public boolean isOwner(String uid) {

        if (uid == null || createdBy == null) {
            return false;
        }

        return uid.equals(createdBy);
    }


    /**
     * Checks whether the given user is
     * currently an administrator.
     */
    public boolean isAdmin(String uid) {

        if (uid == null) {
            return false;
        }

        return getAdmins().contains(uid);
    }


    /**
     * Checks whether the user is either
     * the owner or an administrator.
     */
    public boolean canManageMembers(String uid) {

        return isOwner(uid) || isAdmin(uid);
    }


    /**
     * Checks whether the user can edit
     * group information.
     */
    public boolean canEditGroup(String uid) {

        return isOwner(uid) || isAdmin(uid);
    }


    /**
     * Checks whether the user can manage
     * administrators.
     *
     * Only the original group owner
     * can promote or demote admins.
     */
    public boolean canManageAdmins(String uid) {

        return isOwner(uid);
    }


    /**
     * Checks whether the user is currently
     * a member of the group.
     */
    public boolean isMember(String uid) {

        if (uid == null) {
            return false;
        }

        return getMembers().contains(uid);
    }


    /**
     * Checks whether the user can send
     * messages in the group.
     */
    public boolean canSendMessages(String uid) {

        return isMember(uid);
    }


    /**
     * Checks whether the user can leave
     * the group.
     *
     * The owner should normally transfer
     * ownership before leaving.
     */
    public boolean canLeaveGroup(String uid) {

        if (!isMember(uid)) {
            return false;
        }

        return !isOwner(uid);
    }
}