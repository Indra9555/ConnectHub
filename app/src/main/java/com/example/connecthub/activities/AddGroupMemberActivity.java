package com.example.connecthub.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connecthub.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.connecthub.adapters.AddMemberAdapter;
import com.example.connecthub.models.User;

import java.util.ArrayList;
import java.util.List;
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

                    List<String> members = new ArrayList<>(group.getMembers());

                    if (members.contains(user.getUid())) {
                        return;
                    }

                    members.add(user.getUid());

                    // ============================
                    // Save new join timestamp
                    // ============================
                    java.util.Map<String, Long> memberJoinedAt =
                            group.getMemberJoinedAt();

                    if (memberJoinedAt == null) {
                        memberJoinedAt = new java.util.HashMap<>();
                    }

                    memberJoinedAt.put(
                            user.getUid(),
                            System.currentTimeMillis()
                    );

                    firestore.collection("Groups")
                            .document(groupId)
                            .update(
                                    "members", members,
                                    "memberJoinedAt", memberJoinedAt,
                                    "membersCount", members.size()
                            )
                            .addOnSuccessListener(unused -> {

                                android.widget.Toast.makeText(
                                        this,
                                        user.getName() + " added",
                                        android.widget.Toast.LENGTH_SHORT
                                ).show();

                                loadUsers();

                            });

                });

    }
}