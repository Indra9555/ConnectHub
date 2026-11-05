package com.example.connecthub.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connecthub.R;
import com.example.connecthub.adapters.AddMemberAdapter;
import com.example.connecthub.helpers.GroupHelper;
import com.example.connecthub.models.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AddGroupMemberActivity extends BaseActivity {

    /** Everyone who is not currently in the group */
    private final List<User> candidates = new ArrayList<>();
    /** What is shown (candidates filtered by the search text) */
    private final List<User> userList = new ArrayList<>();

    private AddMemberAdapter adapter;
    private TextInputEditText etSearch;
    private TextView tvEmpty;

    private FirebaseFirestore firestore;
    private String groupId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_group_member);

        groupId = getIntent().getStringExtra("groupId");

        if (groupId == null) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etSearch = findViewById(R.id.etSearch);
        tvEmpty = findViewById(R.id.tvEmpty);
        RecyclerView recyclerUsers = findViewById(R.id.recyclerUsers);

        firestore = FirebaseFirestore.getInstance();

        adapter = new AddMemberAdapter(userList, this::addMemberToGroup);

        recyclerUsers.setLayoutManager(new LinearLayoutManager(this));
        recyclerUsers.setAdapter(adapter);

        // The search box used to be decoration only; it now filters the list
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        loadUsers();
    }

    @SuppressWarnings("unchecked")
    private void loadUsers() {

        firestore.collection("Groups")
                .document(groupId)
                .get()
                .addOnSuccessListener(groupDoc -> {

                    if (!groupDoc.exists()) {
                        finish();
                        return;
                    }

                    Object membersObject = groupDoc.get("members");

                    List<String> currentMembers = membersObject instanceof List
                            ? (List<String>) membersObject : new ArrayList<>();

                    firestore.collection("Users")
                            .get()
                            .addOnSuccessListener(snapshot -> {

                                candidates.clear();

                                for (DocumentSnapshot doc : snapshot.getDocuments()) {

                                    User user = doc.toObject(User.class);

                                    if (user == null) continue;

                                    user.setUid(doc.getId());

                                    if (!currentMembers.contains(user.getUid())) {
                                        candidates.add(user);
                                    }
                                }

                                applyFilter();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Couldn't load users",
                                            Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Couldn't load group", Toast.LENGTH_SHORT).show());
    }

    private void applyFilter() {

        String query = etSearch.getText() == null
                ? "" : etSearch.getText().toString().trim().toLowerCase(Locale.getDefault());

        userList.clear();

        for (User user : candidates) {

            String name = user.getName() == null ? "" : user.getName().toLowerCase(Locale.getDefault());
            String username = user.getUsername() == null ? "" : user.getUsername().toLowerCase(Locale.getDefault());

            if (query.isEmpty() || name.contains(query) || username.contains(query)) {
                userList.add(user);
            }
        }

        adapter.notifyDataSetChanged();

        tvEmpty.setVisibility(userList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void addMemberToGroup(User user) {

        GroupHelper.addMember(groupId, user.getUid(), new GroupHelper.Callback() {

            @Override
            public void onSuccess() {
                candidates.remove(user);
                applyFilter();

                Toast.makeText(AddGroupMemberActivity.this,
                        (user.getName() == null ? "User" : user.getName()) + " added",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(AddGroupMemberActivity.this,
                        "Couldn't add member", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
