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
import com.example.connecthub.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

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
                this::removeMember
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

                    admins = group.getAdmins();

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

                    group.getMembers().remove(user.getUid());

                    firestore.collection("Groups")
                            .document(groupId)
                            .update(
                                    "members", group.getMembers(),
                                    "membersCount", group.getMembers().size()
                            )
                            .addOnSuccessListener(unused -> {

                                android.widget.Toast.makeText(
                                        this,
                                        user.getName() + " removed",
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

                    List<String> members = group.getMembers();

                    members.remove(uid);

                    firestore.collection("Groups")
                            .document(groupId)
                            .update(
                                    "members", members,
                                    "membersCount", members.size()
                            )
                            .addOnSuccessListener(unused -> {

                                Toast.makeText(
                                        this,
                                        "You left the group",
                                        Toast.LENGTH_SHORT
                                ).show();

                                finish();

                            });

                });

    }

}