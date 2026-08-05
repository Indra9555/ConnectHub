package com.example.connecthub.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connecthub.R;
import com.example.connecthub.adapters.SelectMembersAdapter;
import com.example.connecthub.models.Group;
import com.example.connecthub.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateGroupActivity extends AppCompatActivity {

    private RecyclerView rvUsers;
    private TextInputEditText etGroupName;
    private MaterialButton btnCreateGroup;

    private final List<User> userList = new ArrayList<>();
    private SelectMembersAdapter adapter;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        rvUsers = findViewById(R.id.rvUsers);
        etGroupName = findViewById(R.id.etGroupName);
        btnCreateGroup = findViewById(R.id.btnCreateGroup);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        adapter = new SelectMembersAdapter(userList);

        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(adapter);

        loadUsers();

        btnCreateGroup.setOnClickListener(v -> createGroup());
    }

    private void loadUsers() {

        String currentUid = auth.getCurrentUser().getUid();

        db.collection("Users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    userList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                        User user = doc.toObject(User.class);

                        if (!user.getUid().equals(currentUid)) {
                            userList.add(user);
                        }
                    }

                    adapter.notifyDataSetChanged();

                });
    }

    private void createGroup() {

        String groupName = etGroupName.getText().toString().trim();

        if (groupName.isEmpty()) {

            Toast.makeText(this,
                    "Enter group name",
                    Toast.LENGTH_SHORT).show();

            return;
        }

        List<String> members =
                new ArrayList<>(adapter.getSelectedUsers());

        members.add(auth.getCurrentUser().getUid());

        String groupId =
                db.collection("Groups").document().getId();

        Group group = new Group();

        group.setGroupId(groupId);
        group.setGroupName(groupName);
        group.setCreatedBy(auth.getCurrentUser().getUid());
        group.setCreatedAt(System.currentTimeMillis());

        group.setMembers(members);
        group.setMembersCount(members.size());
        Map<String, Long> memberJoinedAt = new HashMap<>();

        long now = System.currentTimeMillis();

        for (String uid : members) {
            memberJoinedAt.put(uid, now);
        }

        group.setMemberJoinedAt(memberJoinedAt);

        List<String> admins = new ArrayList<>();
        admins.add(auth.getCurrentUser().getUid());

        group.setAdmins(admins);

        group.setLastMessage("");
        group.setLastMessageTime(System.currentTimeMillis());

        db.collection("Groups")
                .document(groupId)
                .set(group)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(this,
                            "Group Created",
                            Toast.LENGTH_SHORT).show();

                    finish();

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                e.getMessage(),
                                Toast.LENGTH_SHORT).show());

    }
}