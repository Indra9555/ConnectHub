package com.example.connecthub.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connecthub.R;
import com.example.connecthub.models.GroupMemberInfo;
import com.example.connecthub.models.MembershipPeriod;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.connecthub.adapters.AddMemberAdapter;
import com.example.connecthub.models.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.firebase.firestore.DocumentSnapshot;
import com.example.connecthub.models.Group;

public class AddGroupMemberActivity extends AppCompatActivity {
    private final List<User> userList = new ArrayList<>();

    private AddMemberAdapter adapter;

    private MaterialToolbar toolbar;
    private TextInputEditText etSearch;
    private RecyclerView recyclerUsers;

    private FirebaseFirestore firestore;

    private String groupId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_group_member);

        toolbar = findViewById(R.id.toolbar);
        etSearch = findViewById(R.id.etSearch);
        recyclerUsers = findViewById(R.id.recyclerUsers);

        firestore = FirebaseFirestore.getInstance();

        groupId = getIntent().getStringExtra("groupId");

        recyclerUsers.setLayoutManager(
                new LinearLayoutManager(this)
        );
        adapter = new AddMemberAdapter(userList, this::addMemberToGroup);

        recyclerUsers.setAdapter(adapter);

        loadUsers();
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    private void loadUsers() {

        firestore.collection("Groups")
                .document(groupId)
                .get()
                .addOnSuccessListener(groupDoc -> {

                    if (!groupDoc.exists()) return;

                    Group group = groupDoc.toObject(Group.class);

                    if (group == null) return;

                    List<String> currentMembers = group.getMembers();

                    firestore.collection("Users")
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {

                                userList.clear();

                                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {

                                    User user = doc.toObject(User.class);

                                    if (user == null) continue;

                                    user.setUid(doc.getId());

                                    if (!currentMembers.contains(user.getUid())) {

                                        userList.add(user);

                                    }

                                }

                                adapter.notifyDataSetChanged();

                            });

                });

    }
    private void addMemberToGroup(User user) {

        firestore.collection("Groups")
                .document(groupId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) return;

                    Group group = document.toObject(Group.class);

                    if (group == null) return;

                    List<String> members =
                            new ArrayList<>(group.getMembers());

                    if (!members.contains(user.getUid())) {
                        members.add(user.getUid());
                    }

                    long now = System.currentTimeMillis();

                    // ===============================
                    // Current Member Info
                    // ===============================

                    Map<String, GroupMemberInfo> memberInfo =
                            group.getMemberInfo();

                    if (memberInfo == null) {
                        memberInfo = new HashMap<>();
                    }

                    GroupMemberInfo info =
                            memberInfo.get(user.getUid());

                    if (info == null) {

                        info = new GroupMemberInfo();

                    }

                    info.setJoinedAt(now);
                    info.setActive(true);

                    memberInfo.put(user.getUid(), info);

                    // ===============================
                    // Membership History
                    // ===============================

                    Map<String, List<MembershipPeriod>> memberHistory =
                            group.getMemberHistory();

                    if (memberHistory == null) {
                        memberHistory = new HashMap<>();
                    }

                    List<MembershipPeriod> periods =
                            memberHistory.get(user.getUid());

                    if (periods == null) {
                        periods = new ArrayList<>();
                    }

                    MembershipPeriod newPeriod =
                            new MembershipPeriod();

                    newPeriod.setJoinedAt(now);
                    newPeriod.setLeftAt(null);

                    periods.add(newPeriod);

                    memberHistory.put(user.getUid(), periods);

                    // ===============================
                    // Update Firestore
                    // ===============================

                    firestore.collection("Groups")
                            .document(groupId)
                            .update(
                                    "members", members,
                                    "memberInfo", memberInfo,
                                    "memberHistory", memberHistory,
                                    "membersCount", members.size()
                            )
                            .addOnSuccessListener(unused -> {

                                Toast.makeText(
                                        this,
                                        user.getName() + " added",
                                        Toast.LENGTH_SHORT
                                ).show();

                                loadUsers();

                            });

                });

    }
}