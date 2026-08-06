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
import com.example.connecthub.models.GroupMemberInfo;
import com.example.connecthub.models.MembershipPeriod;
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

            Toast.makeText(
                    this,
                    "Enter group name",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        List<String> members =
                new ArrayList<>(adapter.getSelectedUsers());

        String myUid =
                auth.getCurrentUser().getUid();

        if (!members.contains(myUid)) {
            members.add(myUid);
        }

        String groupId =
                db.collection("Groups")
                        .document()
                        .getId();

        long now = System.currentTimeMillis();

        Group group = new Group();

        group.setGroupId(groupId);
        group.setGroupName(groupName);
        group.setCreatedBy(myUid);
        group.setCreatedAt(now);

        group.setMembers(members);
        group.setMembersCount(members.size());

        // =====================================
        // Current Member Info
        // =====================================

        Map<String, GroupMemberInfo> memberInfo =
                new HashMap<>();

        for (String uid : members) {

            GroupMemberInfo info =
                    new GroupMemberInfo();

            info.setJoinedAt(now);
            info.setActive(true);

            memberInfo.put(uid, info);
        }

        group.setMemberInfo(memberInfo);

        // =====================================
        // Membership History
        // =====================================

        Map<String, List<MembershipPeriod>> memberHistory =
                new HashMap<>();

        for (String uid : members) {

            List<MembershipPeriod> periods =
                    new ArrayList<>();

            MembershipPeriod period =
                    new MembershipPeriod();

            period.setJoinedAt(now);
            period.setLeftAt(null);

            periods.add(period);

            memberHistory.put(uid, periods);
        }

        group.setMemberHistory(memberHistory);

        // =====================================
        // Admins
        // =====================================

        List<String> admins =
                new ArrayList<>();

        admins.add(myUid);

        group.setAdmins(admins);

        // =====================================
        // Last Message
        // =====================================

        group.setLastMessage("");
        group.setLastMessageTime(now);

        db.collection("Groups")
                .document(groupId)
                .set(group)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            this,
                            "Group Created",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();

                })
                .addOnFailureListener(e ->

                        Toast.makeText(
                                this,
                                e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show()

                );

    }
}