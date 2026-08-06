package com.example.connecthub.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.connecthub.R;
import com.example.connecthub.adapters.GroupMemberAdapter;
import com.example.connecthub.models.Group;
import com.example.connecthub.models.GroupMemberInfo;
import com.example.connecthub.models.MembershipPeriod;
import com.example.connecthub.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupInfoActivity extends AppCompatActivity {

    private ImageView imgGroup;
    private TextView tvGroupName;
    private TextView tvMembersCount;

    private RecyclerView recyclerMembers;

    private Button btnAddMember;
    private Button btnLeaveGroup;
    private boolean isAdmin = false;

    private FirebaseFirestore firestore;

    private GroupMemberAdapter adapter;

    private final List<User> members = new ArrayList<>();
    private List<String> admins = new ArrayList<>();

    private String groupId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_info);

        imgGroup = findViewById(R.id.imgGroup);
        tvGroupName = findViewById(R.id.tvGroupName);
        tvMembersCount = findViewById(R.id.tvMembersCount);

        recyclerMembers = findViewById(R.id.recyclerMembers);

        btnAddMember = findViewById(R.id.btnAddMember);
        btnAddMember.setOnClickListener(v -> {

            Intent intent = new Intent(
                    GroupInfoActivity.this,
                    AddGroupMemberActivity.class
            );

            intent.putExtra("groupId", groupId);

            startActivity(intent);

        });
        btnLeaveGroup = findViewById(R.id.btnLeaveGroup);
        btnLeaveGroup.setOnClickListener(v -> leaveGroup());

        firestore = FirebaseFirestore.getInstance();

        groupId = getIntent().getStringExtra("groupId");

        adapter = new GroupMemberAdapter(
                members,
                admins,
                isAdmin,
                this::removeMember,
                this::promoteMember
        );

        recyclerMembers.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerMembers.setAdapter(adapter);

        loadGroupInfo();
    }

    private void loadGroupInfo() {

        firestore.collection("Groups")
                .document(groupId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) return;

                    Group group = document.toObject(Group.class);
                    String myUid = FirebaseAuth.getInstance()
                            .getCurrentUser()
                            .getUid();

                    isAdmin = group.getAdmins().contains(myUid);


                    if (group == null) return;

                    tvGroupName.setText(group.getGroupName());

                    tvMembersCount.setText(
                            group.getMembersCount() + " Members"
                    );

                    adapter.updateAdmins(group.getAdmins(), isAdmin);

                    Glide.with(this)
                            .load(group.getGroupImage())
                            .placeholder(R.drawable.ic_group)
                            .error(R.drawable.ic_group)
                            .into(imgGroup);

                    loadMembers(group.getMembers());

                });

    }
    private void removeMember(User user) {

        firestore.collection("Groups")
                .document(groupId)
                .get()
                .addOnSuccessListener(document -> {

                    Group group = document.toObject(Group.class);

                    if (group == null) return;

                    List<String> members =
                            new ArrayList<>(group.getMembers());

                    List<String> admins =
                            new ArrayList<>(group.getAdmins());

                    long now = System.currentTimeMillis();

                    // Remove from current members/admins
                    members.remove(user.getUid());
                    admins.remove(user.getUid());

                    // Keep at least one admin
                    if (admins.isEmpty() && !members.isEmpty()) {
                        admins.add(members.get(0));
                    }

                    // ==================================
                    // Update MemberInfo
                    // ==================================

                    Map<String, GroupMemberInfo> memberInfo =
                            group.getMemberInfo();

                    if (memberInfo == null) {
                        memberInfo = new HashMap<>();
                    }

                    GroupMemberInfo info =
                            memberInfo.get(user.getUid());

                    if (info != null) {
                        info.setActive(false);
                        memberInfo.put(user.getUid(), info);
                    }

                    // ==================================
                    // Update Membership History
                    // ==================================

                    Map<String, List<MembershipPeriod>> memberHistory =
                            group.getMemberHistory();

                    if (memberHistory == null) {
                        memberHistory = new HashMap<>();
                    }

                    List<MembershipPeriod> periods =
                            memberHistory.get(user.getUid());

                    if (periods != null && !periods.isEmpty()) {

                        MembershipPeriod latest =
                                periods.get(periods.size() - 1);

                        if (latest.getLeftAt() == null) {
                            latest.setLeftAt(now);
                        }

                        memberHistory.put(user.getUid(), periods);
                    }

                    firestore.collection("Groups")
                            .document(groupId)
                            .update(
                                    "members", members,
                                    "admins", admins,
                                    "memberInfo", memberInfo,
                                    "memberHistory", memberHistory,
                                    "membersCount", members.size()
                            )
                            .addOnSuccessListener(unused -> {

                                Toast.makeText(
                                        this,
                                        user.getName() + " removed",
                                        Toast.LENGTH_SHORT
                                ).show();

                                loadGroupInfo();

                            });

                });

    }
    private void promoteMember(User user) {

        firestore.collection("Groups")
                .document(groupId)
                .get()
                .addOnSuccessListener(document -> {

                    Group group = document.toObject(Group.class);

                    if (group == null) return;

                    List<String> admins = group.getAdmins();

                    if (admins.contains(user.getUid())) {
                        return;
                    }

                    admins.add(user.getUid());

                    firestore.collection("Groups")
                            .document(groupId)
                            .update("admins", admins)
                            .addOnSuccessListener(unused -> {

                                android.widget.Toast.makeText(
                                        this,
                                        user.getName() + " is now an Admin",
                                        android.widget.Toast.LENGTH_SHORT
                                ).show();

                                loadGroupInfo();

                            });

                });

    }

    private void loadMembers(List<String> memberIds) {

        members.clear();

        for (String uid : memberIds) {

            firestore.collection("Users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener(document -> {

                        User user = document.toObject(User.class);

                        if (user != null) {

                            members.add(user);

                            adapter.notifyDataSetChanged();

                        }

                    });

        }

    }
    private void leaveGroup() {

        String uid = FirebaseAuth.getInstance()
                .getCurrentUser()
                .getUid();

        firestore.collection("Groups")
                .document(groupId)
                .get()
                .addOnSuccessListener(document -> {

                    Group group = document.toObject(Group.class);

                    if (group == null) return;

                    long now = System.currentTimeMillis();

                    List<String> members =
                            new ArrayList<>(group.getMembers());

                    List<String> admins =
                            new ArrayList<>(group.getAdmins());

                    Map<String, GroupMemberInfo> memberInfo =
                            group.getMemberInfo() == null
                                    ? new HashMap<>()
                                    : new HashMap<>(group.getMemberInfo());

                    Map<String, List<MembershipPeriod>> memberHistory =
                            group.getMemberHistory() == null
                                    ? new HashMap<>()
                                    : new HashMap<>(group.getMemberHistory());

                    // Remove yourself
                    members.remove(uid);
                    admins.remove(uid);

                    // ===========================
                    // MemberInfo
                    // ===========================

                    GroupMemberInfo info = memberInfo.get(uid);

                    if (info != null) {

                        info.setActive(false);

                        memberInfo.put(uid, info);

                    }

                    // ===========================
                    // Membership History
                    // ===========================

                    List<MembershipPeriod> periods =
                            memberHistory.get(uid);

                    if (periods != null && !periods.isEmpty()) {

                        MembershipPeriod latest =
                                periods.get(periods.size() - 1);

                        if (latest.getLeftAt() == null) {

                            latest.setLeftAt(now);

                        }

                        memberHistory.put(uid, periods);

                    }

                    // Delete group if empty
                    if (members.isEmpty()) {

                        firestore.collection("Groups")
                                .document(groupId)
                                .delete()
                                .addOnSuccessListener(unused -> {

                                    Toast.makeText(
                                            this,
                                            "Group deleted",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    Intent intent =
                                            new Intent(
                                                    this,
                                                    GroupListActivity.class
                                            );

                                    intent.addFlags(
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                                                    | Intent.FLAG_ACTIVITY_NEW_TASK
                                    );

                                    startActivity(intent);
                                    finish();

                                });

                        return;
                    }

                    // Always keep one admin
                    if (admins.isEmpty()) {

                        admins.add(members.get(0));

                    }

                    // Transfer ownership
                    String createdBy = group.getCreatedBy();

                    if (createdBy != null &&
                            createdBy.equals(uid)) {

                        createdBy = admins.get(0);

                    }

                    firestore.collection("Groups")
                            .document(groupId)
                            .update(
                                    "members", members,
                                    "admins", admins,
                                    "memberInfo", memberInfo,
                                    "memberHistory", memberHistory,
                                    "createdBy", createdBy,
                                    "membersCount", members.size()
                            )
                            .addOnSuccessListener(unused -> {

                                Toast.makeText(
                                        this,
                                        "You left the group",
                                        Toast.LENGTH_SHORT
                                ).show();

                                Intent intent =
                                        new Intent(
                                                this,
                                                GroupListActivity.class
                                        );

                                intent.addFlags(
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                                                | Intent.FLAG_ACTIVITY_NEW_TASK
                                );

                                startActivity(intent);
                                finish();

                            });

                });

    }

}