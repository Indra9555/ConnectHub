package com.example.connecthub.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connecthub.R;
import com.example.connecthub.adapters.PostAdapter;
import com.example.connecthub.models.Post;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends BaseActivity {

    private static final String PREFS = "connecthub_prefs";
    private static final String KEY_ASKED_NOTIFICATIONS = "asked_notifications";

    private TextView tvBadge;
    private TextView tvEmpty;
    private MenuItem notificationMenuItem;
    private boolean badgeListenerAttached = false;

    private RecyclerView recyclerPosts;
    private BottomNavigationView bottomNav;

    private final List<Post> postList = new ArrayList<>();
    private PostAdapter adapter;

    private FirebaseFirestore firestore;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                // Nothing to do: FCM works automatically once the permission is granted.
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_main);

        // The BottomNavigationView takes care of the bottom inset itself
        applyWindowInsets(false);

        MaterialToolbar toolbar = findViewById(R.id.homeToolbar);
        recyclerPosts = findViewById(R.id.recyclerPosts);
        tvEmpty = findViewById(R.id.tvEmpty);
        bottomNav = findViewById(R.id.bottomNav);
        FloatingActionButton btnCreatePost = findViewById(R.id.btnCreatePost);

        firestore = FirebaseFirestore.getInstance();

        setSupportActionBar(toolbar);

        adapter = new PostAdapter(postList);
        recyclerPosts.setLayoutManager(new LinearLayoutManager(this));
        recyclerPosts.setAdapter(adapter);

        btnCreatePost.setOnClickListener(v ->
                startActivity(new Intent(this, CreatePostActivity.class)));

        setupBottomNavigation();
        registerFcmToken();
        askNotificationPermission();
        loadPosts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNav != null) {
            bottomNav.getMenu().findItem(R.id.nav_home).setChecked(true);
        }
    }

    // ---------------------------------------------------------------------
    // Navigation
    // ---------------------------------------------------------------------

    private void setupBottomNavigation() {

        bottomNav.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_home) {
                return true;
            }

            Class<?> target = null;

            if (id == R.id.nav_search) target = SearchActivity.class;
            else if (id == R.id.nav_chats) target = ChatListActivity.class;
            else if (id == R.id.nav_groups) target = GroupListActivity.class;
            else if (id == R.id.nav_profile) target = ProfileActivity.class;

            if (target != null) {
                startActivity(new Intent(this, target));
            }

            // Returning false keeps "Home" highlighted
            return false;
        });

        bottomNav.setOnItemReselectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                recyclerPosts.smoothScrollToPosition(0);
            }
        });
    }

    // ---------------------------------------------------------------------
    // Push notifications
    // ---------------------------------------------------------------------

    private void registerFcmToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (user == null) return;

            firestore.collection("Users")
                    .document(user.getUid())
                    .update("fcmToken", token);
        });
    }

    /** Android 13+ requires a runtime permission, otherwise no push notification is ever shown. */
    private void askNotificationPermission() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        if (prefs.getBoolean(KEY_ASKED_NOTIFICATIONS, false)) return;

        prefs.edit().putBoolean(KEY_ASKED_NOTIFICATIONS, true).apply();

        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    // ---------------------------------------------------------------------
    // Feed
    // ---------------------------------------------------------------------

    private void loadPosts() {

        // Activity-scoped listener: removed automatically when the screen stops
        firestore.collection("Posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(this, (value, error) -> {

                    if (error != null || value == null) {
                        if (postList.isEmpty()) {
                            tvEmpty.setText("Couldn't load posts.\nCheck your connection.");
                            tvEmpty.setVisibility(View.VISIBLE);
                        }
                        return;
                    }

                    postList.clear();

                    for (DocumentSnapshot document : value.getDocuments()) {

                        Post post = document.toObject(Post.class);

                        if (post != null) {
                            post.setPostId(document.getId());
                            postList.add(post);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    tvEmpty.setText("No posts yet.\nTap + to share the first one!");
                    tvEmpty.setVisibility(postList.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    // ---------------------------------------------------------------------
    // Toolbar menu
    // ---------------------------------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.home_menu, menu);

        notificationMenuItem = menu.findItem(R.id.action_notifications);

        View actionView = getLayoutInflater()
                .inflate(R.layout.menu_notification_badge, null);

        tvBadge = actionView.findViewById(R.id.tvBadge);

        actionView.setOnClickListener(v -> onOptionsItemSelected(notificationMenuItem));

        notificationMenuItem.setActionView(actionView);

        loadNotificationBadge();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.action_notifications) {
            startActivity(new Intent(this, NotificationsActivity.class));
            return true;
        }

        if (item.getItemId() == R.id.action_logout) {
            confirmLogout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadNotificationBadge() {

        if (badgeListenerAttached) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) return;

        badgeListenerAttached = true;

        firestore.collection("Notifications")
                .whereEqualTo("receiverId", user.getUid())
                .whereEqualTo("seen", false)
                .addSnapshotListener(this, (value, error) -> {

                    if (value == null || tvBadge == null) return;

                    int count = value.size();

                    if (count == 0) {
                        tvBadge.setVisibility(View.GONE);
                    } else {
                        tvBadge.setVisibility(View.VISIBLE);
                        tvBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                    }
                });
    }

    // ---------------------------------------------------------------------
    // Logout
    // ---------------------------------------------------------------------

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Log out?")
                .setMessage("You will stop receiving notifications on this device.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Log out", (d, w) -> logout())
                .show();
    }

    /**
     * Marks the user offline and removes this device's push token BEFORE signing out.
     * Previously the token stayed on the account, so after logging out (or switching accounts)
     * this phone kept receiving the previous user's message notifications.
     */
    private void logout() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            goToLogin();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("online", false);
        updates.put("lastSeen", System.currentTimeMillis());
        updates.put("typingTo", "");
        updates.put("fcmToken", FieldValue.delete());

        AtomicBoolean done = new AtomicBoolean(false);

        Runnable finishLogout = () -> {
            if (done.compareAndSet(false, true)) {
                FirebaseAuth.getInstance().signOut();
                goToLogin();
            }
        };

        firestore.collection("Users")
                .document(user.getUid())
                .update(updates)
                .addOnCompleteListener(task -> finishLogout.run());

        // Don't block the user when offline
        new Handler(Looper.getMainLooper()).postDelayed(finishLogout, 2000);
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
