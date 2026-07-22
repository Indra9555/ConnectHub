package com.example.connecthub.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connecthub.R;
import com.example.connecthub.adapters.PostAdapter;
import com.example.connecthub.models.Post;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;

public class MainActivity extends AppCompatActivity {

    private TextView tvBadge;
    private MenuItem notificationMenuItem;

    private RecyclerView recyclerPosts;
    private FloatingActionButton btnCreatePost;
    private MaterialToolbar toolbar;

    private List<Post> postList;
    private PostAdapter adapter;

    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {

            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            v.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
            );

            return insets;
        });


        toolbar = findViewById(R.id.homeToolbar);
        recyclerPosts = findViewById(R.id.recyclerPosts);
        btnCreatePost = findViewById(R.id.btnCreatePost);

        firestore = FirebaseFirestore.getInstance();
        com.google.firebase.messaging.FirebaseMessaging
                .getInstance()
                .getToken()
                .addOnSuccessListener(token -> {

                    if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                        return;
                    }

                    firestore.collection("Users")
                            .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .update("fcmToken", token);

                });

        setSupportActionBar(toolbar);

        postList = new ArrayList<>();
        adapter = new PostAdapter(postList);

        recyclerPosts.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerPosts.setAdapter(adapter);

        loadPosts();

        btnCreatePost.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    CreatePostActivity.class
            );

            startActivity(intent);

        });
    }

    private void loadPosts() {

        firestore.collection("Posts")
                .orderBy(
                        "timestamp",
                        Query.Direction.DESCENDING
                )
                .addSnapshotListener((value, error) -> {

                    if (value == null) {
                        return;
                    }

                    postList.clear();

                    for (var document : value.getDocuments()) {

                        Post post = document.toObject(Post.class);

                        if (post != null) {
                            postList.add(post);
                        }
                    }

                    adapter.notifyDataSetChanged();

                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.home_menu, menu);

        notificationMenuItem = menu.findItem(R.id.action_notifications);

        View actionView = getLayoutInflater()
                .inflate(R.layout.menu_notification_badge, null);

        tvBadge = actionView.findViewById(R.id.tvBadge);

        actionView.setOnClickListener(v ->
                onOptionsItemSelected(notificationMenuItem));

        notificationMenuItem.setActionView(actionView);

        loadNotificationBadge();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(
            @NonNull MenuItem item) {

        if (item.getItemId() == R.id.action_notifications) {

            startActivity(
                    new Intent(
                            this,
                            NotificationsActivity.class
                    )
            );

            return true;
        }

        if (item.getItemId() == R.id.action_profile) {

            startActivity(
                    new Intent(
                            this,
                            ProfileActivity.class
                    )
            );

            return true;
        }

        if (item.getItemId() == R.id.action_search) {

            startActivity(
                    new Intent(
                            this,
                            SearchActivity.class
                    )
            );

            return true;
        }

        if (item.getItemId() == R.id.action_notifications) {

            startActivity(
                    new Intent(
                            this,
                            NotificationsActivity.class
                    )
            );

            return true;
        }
        if (item.getItemId() == R.id.action_chat) {

            startActivity(
                    new Intent(
                            this,
                            ChatListActivity.class
                    )
            );

            return true;
        }

        if (item.getItemId() == R.id.action_logout) {

            FirebaseAuth.getInstance().signOut();

            Intent intent = new Intent(
                    this,
                    LoginActivity.class
            );

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private void loadNotificationBadge() {

        String uid = FirebaseAuth.getInstance()
                .getCurrentUser()
                .getUid();

        firestore.collection("Notifications")
                .whereEqualTo("receiverId", uid)
                .whereEqualTo("seen", false)
                .addSnapshotListener((value, error) -> {

                    if (value == null || tvBadge == null) {
                        return;
                    }

                    int count = value.size();

                    if (count == 0) {

                        tvBadge.setVisibility(View.GONE);

                    } else {

                        tvBadge.setVisibility(View.VISIBLE);
                        tvBadge.setText(String.valueOf(count));

                    }

                });

    }
}