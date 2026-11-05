package com.example.connecthub.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.connecthub.R;
import com.example.connecthub.adapters.GroupMemberAdapter;
import com.example.connecthub.helpers.GroupHelper;
import com.example.connecthub.models.Group;
import com.example.connecthub.models.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class GroupInfoActivity extends BaseActivity {

    private ImageView imgGroup;
    private TextView tvGroupName;
    private TextView tvMembersCount;

    private MaterialButton btnAddMember;
    private MaterialButton btnLeaveGroup;

    private GroupMemberAdapter adapter;

    private final List<User> members = new ArrayList<>();
    private final List<String> admins = new ArrayList<>();

    private FirebaseFirestore firestore;

    private String groupId;
    private String myUid;
    private Group group;
    private boolean isAdmin = false;
    private boolean isMember = false;

    /** incremented for every reload so late answers of an older load are ignored */
    private int loadGeneration = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_info);

        groupId = getIntent().getStringExtra("groupId");

        if (groupId == null || FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestore = FirebaseFirestore.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        imgGroup = findViewById(R.id.imgGroup);
        tvGroupName = findViewById(R.id.tvGroupName);
        tvMembersCount = findViewById(R.id.tvMembersCount);
        btnAddMember = findViewById(R.id.btnAddMember);
        btnLeaveGroup = findViewById(R.id.btnLeaveGroup);
        RecyclerView recyclerMembers = findViewById(R.id.recyclerMembers);

        adapter = new GroupMemberAdapter(
                members,
                new ArrayList<>(),
                false,
                this::confirmRemove,
                this::promote,
                this::demote
        );

        recyclerMembers.setLayoutManager(new LinearLayoutManager(this));
        recyclerMembers.setAdapter(adapter);

        btnAddMember.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddGroupMemberActivity.class);
            intent.putExtra("groupId", groupId);
            startActivity(intent);
        });

        btnLeaveGroup.setOnClickListener(v -> confirmLeave());

        listenToGroup();
    }

    /** Live listener: member changes (by me or another admin) show up immediately. */
    private void listenToGroup() {

        firestore.collection("Groups")
                .document(groupId)
                .addSnapshotListener(this, (doc, error) -> {

                    if (doc == null) return;

                    if (!doc.exists()) {
                        // deleted (last member left)
                        finish();
                        return;
                    }

                    Group loaded = doc.toObject(Group.class);

                    if (loaded == null) return;

                    loaded.setGroupId(doc.getId());
                    group = loaded;

                    bindGroup();
                });
    }

    private void bindGroup() {

        tvGroupName.setText(group.getGroupName() == null ? "Group" : group.getGroupName());

        int count = group.getMembersCount();
        tvMembersCount.setText(count + (count == 1 ? " member" : " members"));

        if (group.getGroupImage() != null && !group.getGroupImage().isEmpty()) {
            Glide.with(this)
                    .load(group.getGroupImage())
                    .placeholder(R.drawable.ic_group_avatar)
                    .into(imgGroup);
        } else {
            imgGroup.setImageResource(R.drawable.ic_group_avatar);
        }

        admins.clear();

        if (group.getAdmins() != null) {
            admins.addAll(group.getAdmins());
        }

        isMember = group.getMembers() != null && group.getMembers().contains(myUid);
        isAdmin = isMember && admins.contains(myUid);

        // Only admins can add people; only members can leave
        btnAddMember.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        btnLeaveGroup.setVisibility(isMember ? View.VISIBLE : View.GONE);

        adapter.setOwner(group.getCreatedBy());
        adapter.updateAdmins(admins, isAdmin);

        loadMembers();
    }

    /** Loads the member profiles in ONE batched query per 10 users, preserving a stable order. */
    private void loadMembers() {

        List<String> uids = group.getMembers() == null
                ? new ArrayList<>() : new ArrayList<>(group.getMembers());

        final int generation = ++loadGeneration;

        if (uids.isEmpty()) {
            members.clear();
            adapter.notifyDataSetChanged();
            return;
        }

        List<Task<QuerySnapshot>> tasks = new ArrayList<>();

        for (int i = 0; i < uids.size(); i += 10) {

            List<String> chunk = uids.subList(i, Math.min(i + 10, uids.size()));

            tasks.add(firestore.collection("Users")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get());
        }

        Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {

            if (isDestroyed() || generation != loadGeneration) return;

            List<User> loaded = new ArrayList<>();

            for (Object result : results) {
                for (DocumentSnapshot doc : ((QuerySnapshot) result).getDocuments()) {

                    User user = doc.toObject(User.class);

                    if (user != null) {
                        user.setUid(doc.getId());
                        loaded.add(user);
                    }
                }
            }

            String owner = group.getCreatedBy();

            // owner first, then admins, then everyone else by name
            Collections.sort(loaded, (a, b) -> {

                int rankA = a.getUid().equals(owner) ? 0 : admins.contains(a.getUid()) ? 1 : 2;
                int rankB = b.getUid().equals(owner) ? 0 : admins.contains(b.getUid()) ? 1 : 2;

                if (rankA != rankB) return Integer.compare(rankA, rankB);

                String x = a.getName() == null ? "" : a.getName().toLowerCase(Locale.getDefault());
                String y = b.getName() == null ? "" : b.getName().toLowerCase(Locale.getDefault());

                return x.compareTo(y);
            });

            members.clear();
            members.addAll(loaded);

            adapter.notifyDataSetChanged();
        });
    }

    // ---------------------------------------------------------------------
    // Actions
    // ---------------------------------------------------------------------

    private void confirmRemove(User user) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Remove member?")
                .setMessage("Remove " + (user.getName() == null ? "this user" : user.getName())
                        + " from the group?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Remove", (d, w) ->
                        GroupHelper.removeMember(groupId, user.getUid(), toastCallback(
                                "Member removed", "Couldn't remove member")))
                .show();
    }

    private void promote(User user) {
        GroupHelper.setAdmin(groupId, user.getUid(), true,
                toastCallback("Promoted to admin", "Couldn't promote"));
    }

    private void demote(User user) {
        GroupHelper.setAdmin(groupId, user.getUid(), false,
                toastCallback("Admin rights removed", "Couldn't update admin"));
    }

    private void confirmLeave() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Leave group?")
                .setMessage("You will no longer receive new messages from this group.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Leave", (d, w) ->
                        GroupHelper.removeMember(groupId, myUid, new GroupHelper.Callback() {

                            @Override
                            public void onSuccess() {
                                Toast.makeText(GroupInfoActivity.this,
                                        "You left the group", Toast.LENGTH_SHORT).show();

                                // back to the list (skip the chat that is still open)
                                Intent intent = new Intent(GroupInfoActivity.this,
                                        GroupListActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                finish();
                            }

                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(GroupInfoActivity.this,
                                        "Couldn't leave group", Toast.LENGTH_SHORT).show();
                            }
                        }))
                .show();
    }

    private GroupHelper.Callback toastCallback(String ok, String fail) {
        return new GroupHelper.Callback() {
            @Override
            public void onSuccess() {
                Toast.makeText(GroupInfoActivity.this, ok, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(GroupInfoActivity.this,
                        e.getMessage() != null && e.getMessage().contains("at least one admin")
                                ? e.getMessage() : fail,
                        Toast.LENGTH_SHORT).show();
            }
        };
    }
}
