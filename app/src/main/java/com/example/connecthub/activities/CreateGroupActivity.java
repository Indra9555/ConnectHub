package com.example.connecthub.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connecthub.R;
import com.example.connecthub.adapters.SelectMembersAdapter;
import com.example.connecthub.models.Group;
import com.example.connecthub.models.GroupMemberInfo;
import com.example.connecthub.models.MembershipPeriod;
import com.example.connecthub.models.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreateGroupActivity extends BaseActivity {

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

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView rvUsers = findViewById(R.id.rvUsers);
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

        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        String currentUid = auth.getCurrentUser().getUid();

        db.collection("Users")
                .get()
                .addOnSuccessListener(snapshot -> {

                    userList.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {

                        User user = doc.toObject(User.class);

                        if (user == null) continue;

                        user.setUid(doc.getId());

                        if (!currentUid.equals(user.getUid())) {
                            userList.add(user);
                        }
                    }

                    Collections.sort(userList, (a, b) -> {
                        String x = a.getName() == null ? "" : a.getName().toLowerCase(Locale.getDefault());
                        String y = b.getName() == null ? "" : b.getName().toLowerCase(Locale.getDefault());
                        return x.compareTo(y);
                    });

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show());
    }

    private void createGroup() {

        if (auth.getCurrentUser() == null) return;

        String groupName = etGroupName.getText() == null
                ? "" : etGroupName.getText().toString().trim();

        if (groupName.isEmpty()) {
            etGroupName.setError("Enter a group name");
            etGroupName.requestFocus();
            return;
        }

        List<String> members = new ArrayList<>(adapter.getSelectedUsers());

        if (members.isEmpty()) {
            Toast.makeText(this, "Select at least one member", Toast.LENGTH_SHORT).show();
            return;
        }

        String myUid = auth.getCurrentUser().getUid();

        if (!members.contains(myUid)) {
            members.add(myUid);
        }

        btnCreateGroup.setEnabled(false);

        String groupId = db.collection("Groups").document().getId();

        long now = System.currentTimeMillis();

        Group group = new Group();

        group.setGroupId(groupId);
        group.setGroupName(groupName);
        group.setCreatedBy(myUid);
        group.setCreatedAt(now);
        group.setMembers(members);
        group.setMembersCount(members.size());

        Map<String, GroupMemberInfo> memberInfo = new HashMap<>();
        Map<String, List<MembershipPeriod>> memberHistory = new HashMap<>();

        for (String uid : members) {

            GroupMemberInfo info = new GroupMemberInfo();
            info.setJoinedAt(now);
            info.setActive(true);
            memberInfo.put(uid, info);

            MembershipPeriod period = new MembershipPeriod();
            period.setJoinedAt(now);
            period.setLeftAt(null);

            List<MembershipPeriod> periods = new ArrayList<>();
            periods.add(period);
            memberHistory.put(uid, periods);
        }

        group.setMemberInfo(memberInfo);
        group.setMemberHistory(memberHistory);

        List<String> admins = new ArrayList<>();
        admins.add(myUid);
        group.setAdmins(admins);

        group.setLastMessage("");
        group.setLastMessageTime(now);

        db.collection("Groups")
                .document(groupId)
                .set(group)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Group created", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnCreateGroup.setEnabled(true);
                    Toast.makeText(this,
                            e.getMessage() == null ? "Couldn't create group" : e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
