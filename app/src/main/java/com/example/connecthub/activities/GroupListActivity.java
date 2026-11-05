package com.example.connecthub.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connecthub.R;
import com.example.connecthub.adapters.GroupAdapter;
import com.example.connecthub.models.Group;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GroupListActivity extends BaseActivity {

    private TextView tvEmpty;
    private final List<Group> groupList = new ArrayList<>();
    private GroupAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_list);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView rvGroups = findViewById(R.id.rvGroups);
        FloatingActionButton fabCreateGroup = findViewById(R.id.fabCreateGroup);
        tvEmpty = findViewById(R.id.tvEmpty);

        adapter = new GroupAdapter(groupList, group -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("isGroup", true);
            intent.putExtra("groupId", group.getGroupId());
            intent.putExtra("groupName", group.getGroupName());
            startActivity(intent);
        });

        rvGroups.setLayoutManager(new LinearLayoutManager(this));
        rvGroups.setAdapter(adapter);

        fabCreateGroup.setOnClickListener(v ->
                startActivity(new Intent(this, CreateGroupActivity.class)));

        loadGroups();
    }

    /**
     * Only the groups I have EVER been a member of are fetched (a server-side query on
     * memberInfo.{uid}.joinedAt). The old code downloaded every group in the app and
     * filtered locally, and re-did that on every onResume. The live listener also makes
     * new groups / last-message previews appear without reopening the screen.
     */
    private void loadGroups() {

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("Groups")
                .whereGreaterThan(FieldPath.of("memberInfo", uid, "joinedAt"), 0)
                .addSnapshotListener(this, (snapshot, error) -> {

                    if (error != null) {
                        Toast.makeText(this, "Couldn't load groups", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshot == null) return;

                    groupList.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {

                        Group group = doc.toObject(Group.class);

                        if (group == null) continue;

                        group.setGroupId(doc.getId());
                        groupList.add(group);
                    }

                    // most recently active first
                    Collections.sort(groupList, (a, b) ->
                            Long.compare(b.getLastMessageTime(), a.getLastMessageTime()));

                    adapter.notifyDataSetChanged();

                    tvEmpty.setVisibility(groupList.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }
}
