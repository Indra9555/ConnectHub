package com.example.connecthub.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connecthub.R;
import com.example.connecthub.adapters.GroupAdapter;
import com.example.connecthub.models.Group;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class GroupListActivity extends AppCompatActivity {

    private RecyclerView rvGroups;
    private FloatingActionButton fabCreateGroup;

    private final List<Group> groupList = new ArrayList<>();
    private GroupAdapter adapter;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_list);

        rvGroups = findViewById(R.id.rvGroups);
        fabCreateGroup = findViewById(R.id.fabCreateGroup);

        db = FirebaseFirestore.getInstance();

        adapter = new GroupAdapter(groupList, group -> {

            Intent intent =
                    new Intent(GroupListActivity.this,
                            ChatActivity.class);

            intent.putExtra("isGroup", true);

            intent.putExtra("groupId", group.getGroupId());

            intent.putExtra("groupName", group.getGroupName());

            startActivity(intent);

        });

        rvGroups.setLayoutManager(new LinearLayoutManager(this));
        rvGroups.setAdapter(adapter);

        loadGroups();

        fabCreateGroup.setOnClickListener(v ->
                startActivity(
                        new Intent(this,
                                CreateGroupActivity.class)
                ));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGroups();
    }

    private void loadGroups() {

        String uid = FirebaseAuth.getInstance()
                .getCurrentUser()
                .getUid();

        db.collection("Groups")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    groupList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                        Group group = doc.toObject(Group.class);

                        if (group == null) continue;

                        group.setGroupId(doc.getId());

                        // Show group if user has EVER been a member
                        if (group.getMemberInfo() != null &&
                                group.getMemberInfo().containsKey(uid)) {

                            groupList.add(group);

                        }

                    }

                    adapter.notifyDataSetChanged();

                })
                .addOnFailureListener(Throwable::printStackTrace);

    }
}