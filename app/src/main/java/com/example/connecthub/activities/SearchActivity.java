package com.example.connecthub.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connecthub.R;
import com.example.connecthub.adapters.UserAdapter;
import com.example.connecthub.models.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchActivity extends BaseActivity {

    private static final long DEBOUNCE_MS = 250;

    private EditText etSearch;
    private TextView tvEmpty;

    private UserAdapter adapter;
    private final List<User> userList = new ArrayList<>();

    /** All users, downloaded ONCE per visit (previously re-downloaded on every keystroke). */
    private final List<User> allUsers = new ArrayList<>();
    private boolean usersLoaded = false;
    private boolean loadFailed = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable searchRunnable = this::applyFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etSearch = findViewById(R.id.etSearch);
        tvEmpty = findViewById(R.id.tvEmpty);
        RecyclerView recyclerUsers = findViewById(R.id.recyclerUsers);

        adapter = new UserAdapter(this, userList);

        recyclerUsers.setLayoutManager(new LinearLayoutManager(this));
        recyclerUsers.setAdapter(adapter);

        tvEmpty.setVisibility(View.VISIBLE);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                handler.removeCallbacks(searchRunnable);
                handler.postDelayed(searchRunnable, DEBOUNCE_MS);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        loadUsers();
        etSearch.requestFocus();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(searchRunnable);
        super.onDestroy();
    }

    private void loadUsers() {

        String myUid = FirebaseAuth.getInstance().getCurrentUser() == null
                ? "" : FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("Users")
                .get()
                .addOnSuccessListener(snapshot -> {

                    allUsers.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {

                        User user = doc.toObject(User.class);

                        if (user == null) continue;

                        user.setUid(doc.getId());

                        // don't list yourself
                        if (!doc.getId().equals(myUid)) {
                            allUsers.add(user);
                        }
                    }

                    usersLoaded = true;
                    applyFilter();
                })
                .addOnFailureListener(e -> {
                    loadFailed = true;
                    applyFilter();
                });
    }

    private void applyFilter() {

        String query = etSearch.getText() == null
                ? "" : etSearch.getText().toString().trim().toLowerCase(Locale.getDefault());

        userList.clear();

        if (query.isEmpty()) {
            adapter.notifyDataSetChanged();
            showEmpty("Type a name or username");
            return;
        }

        if (!usersLoaded) {
            adapter.notifyDataSetChanged();
            showEmpty(loadFailed ? "Couldn't load users. Check your connection." : "Searching...");
            return;
        }

        for (User user : allUsers) {

            String username = user.getUsername() == null
                    ? "" : user.getUsername().toLowerCase(Locale.getDefault());
            String name = user.getName() == null
                    ? "" : user.getName().toLowerCase(Locale.getDefault());

            if (username.contains(query) || name.contains(query)) {
                userList.add(user);
            }
        }

        adapter.notifyDataSetChanged();

        if (userList.isEmpty()) {
            showEmpty("No users found for \"" + etSearch.getText().toString().trim() + "\"");
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void showEmpty(String message) {
        tvEmpty.setText(message);
        tvEmpty.setVisibility(View.VISIBLE);
    }
}
