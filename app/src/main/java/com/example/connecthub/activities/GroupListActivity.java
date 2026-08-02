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

        android.util.Log.d("GROUPS", "Current UID = " + uid);

        db.collection("Groups")
                .whereArrayContains("members", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    android.util.Log.d(
                            "GROUPS",
                            "Found groups = " + queryDocumentSnapshots.size()
                    );

                    groupList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                        android.util.Log.d(
                                "GROUPS",
                                "Document ID = " + doc.getId()
                        );

                        android.util.Log.d(
                                "GROUPS",
                                "Data = " + doc.getData().toString()
                        );

                        Group group = doc.toObject(Group.class);

                        group.setGroupId(doc.getId());

                        groupList.add(group);
                    }

                    adapter.notifyDataSetChanged();

                })
                .addOnFailureListener(e -> {

                    android.util.Log.e(
                            "GROUPS",
                            "Firestore Error",
                            e
                    );

                });

    }
}