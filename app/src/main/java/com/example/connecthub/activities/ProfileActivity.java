package com.example.connecthub.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.connecthub.R;
import com.example.connecthub.adapters.PostAdapter;
import com.example.connecthub.helpers.FollowHelper;
import com.example.connecthub.helpers.PostHelper;
import com.example.connecthub.models.Post;
import com.example.connecthub.network.CloudinaryUploader;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProfileActivity extends BaseActivity {

    private ShapeableImageView imgProfile;

    private TextView tvName;
    private TextView tvUsername;
    private TextView tvBio;
    private TextView tvEmail;
    private TextView tvPostCount;
    private TextView tvFollowers;
    private TextView tvFollowing;
    private TextView tvNoPosts;
    private TextView tvPostsTitle;

    private MaterialButton btnEditProfile;
    private MaterialButton btnFollow;
    private MaterialButton btnMessage;
    private MaterialButton btnUploadImage;

    private ProgressBar progressBar;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    private String profileUid;
    private String myUid;
    private boolean isMe;
    private boolean following;
    private boolean followBusy;

    private String currentName;
    private String currentUsername;

    private PostAdapter postAdapter;
    private final List<Post> myPosts = new ArrayList<>();

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    // Show the chosen photo immediately and upload it right away
                    imgProfile.setImageURI(uri);
                    uploadProfileImage(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        FirebaseUser me = auth.getCurrentUser();

        if (me == null) {
            finish();
            return;
        }

        myUid = me.getUid();

        profileUid = getIntent().getStringExtra("uid");

        if (profileUid == null || profileUid.isEmpty()) {
            profileUid = myUid;
        }

        isMe = profileUid.equals(myUid);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setTitle(isMe ? "My profile" : "Profile");

        imgProfile = findViewById(R.id.imgProfile);
        tvName = findViewById(R.id.tvName);
        tvUsername = findViewById(R.id.tvUsername);
        tvBio = findViewById(R.id.tvBio);
        tvEmail = findViewById(R.id.tvEmail);
        tvPostCount = findViewById(R.id.tvPostCount);
        tvFollowers = findViewById(R.id.tvFollowers);
        tvFollowing = findViewById(R.id.tvFollowing);
        tvNoPosts = findViewById(R.id.tvNoPosts);
        tvPostsTitle = findViewById(R.id.tvPostsTitle);

        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnFollow = findViewById(R.id.btnFollow);
        btnMessage = findViewById(R.id.btnMessage);
        btnUploadImage = findViewById(R.id.btnUploadImage);
        progressBar = findViewById(R.id.progressBar);

        RecyclerView recyclerMyPosts = findViewById(R.id.recyclerMyPosts);

        postAdapter = new PostAdapter(myPosts);
        recyclerMyPosts.setLayoutManager(new LinearLayoutManager(this));
        recyclerMyPosts.setAdapter(postAdapter);

        tvPostsTitle.setText(isMe ? "My posts" : "Posts");

        btnEditProfile.setVisibility(isMe ? View.VISIBLE : View.GONE);
        btnUploadImage.setVisibility(isMe ? View.VISIBLE : View.GONE);
        btnFollow.setVisibility(isMe ? View.GONE : View.VISIBLE);
        btnMessage.setVisibility(isMe ? View.GONE : View.VISIBLE);

        if (isMe) {
            imgProfile.setOnClickListener(v -> imagePicker.launch("image/*"));
        }

        btnUploadImage.setOnClickListener(v -> imagePicker.launch("image/*"));

        btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class)));

        btnFollow.setOnClickListener(v -> toggleFollow());

        btnMessage.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("uid", profileUid);
            startActivity(intent);
        });

        if (!isMe) {
            FollowHelper.isFollowing(profileUid, isFollowing -> {
                following = isFollowing;
                renderFollowButton();
            });
        }

        renderFollowButton();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reloaded here only (not additionally in onCreate) so each load runs once per visit
        if (profileUid != null) {
            loadUserProfile();
            loadPosts();
            loadFollowCounts();
        }
    }

    // ---------------------------------------------------------------------

    private void renderFollowButton() {
        btnFollow.setText(following ? "Following" : "Follow");
    }

    private void toggleFollow() {

        if (followBusy) return;

        followBusy = true;
        btnFollow.setEnabled(false);

        FollowHelper.toggleFollow(profileUid, new FollowHelper.ToggleCallback() {

            @Override
            public void onSuccess(boolean nowFollowing) {
                followBusy = false;
                following = nowFollowing;
                btnFollow.setEnabled(true);
                renderFollowButton();
                loadFollowCounts();
            }

            @Override
            public void onFailure(Exception e) {
                followBusy = false;
                btnFollow.setEnabled(true);
                Toast.makeText(ProfileActivity.this,
                        "Couldn't update follow. Check your connection.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserProfile() {

        progressBar.setVisibility(View.VISIBLE);

        firestore.collection("Users")
                .document(profileUid)
                .get()
                .addOnSuccessListener(doc -> {

                    progressBar.setVisibility(View.GONE);

                    if (isDestroyed() || !doc.exists()) return;

                    currentName = doc.getString("name");
                    currentUsername = doc.getString("username");

                    tvName.setText(currentName == null ? "" : currentName);
                    tvUsername.setText(currentUsername == null ? "" : "@" + currentUsername);

                    String bio = doc.getString("bio");
                    tvBio.setText(bio == null ? "" : bio);
                    tvBio.setVisibility(bio == null || bio.isEmpty() ? View.GONE : View.VISIBLE);

                    // The e-mail address is private: only show it on your own profile
                    String email = doc.getString("email");

                    if (isMe && email != null && !email.isEmpty()) {
                        tvEmail.setText(email);
                        tvEmail.setVisibility(View.VISIBLE);
                    } else {
                        tvEmail.setVisibility(View.GONE);
                    }

                    String imageUrl = doc.getString("image");

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(this)
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person)
                                .into(imgProfile);
                    } else {
                        imgProfile.setImageResource(R.drawable.ic_person);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Couldn't load profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadPosts() {

        firestore.collection("Posts")
                .whereEqualTo("uid", profileUid)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (isDestroyed()) return;

                    myPosts.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {

                        Post post = doc.toObject(Post.class);

                        if (post != null) {
                            post.setPostId(doc.getId());
                            myPosts.add(post);
                        }
                    }

                    // Newest first (sorted here so no composite index is required)
                    Collections.sort(myPosts, (a, b) ->
                            Long.compare(b.getTimestamp(), a.getTimestamp()));

                    postAdapter.notifyDataSetChanged();

                    tvPostCount.setText(String.valueOf(myPosts.size()));
                    tvNoPosts.setVisibility(myPosts.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void loadFollowCounts() {

        FollowHelper.getCount(profileUid, "Followers", count ->
                tvFollowers.setText(String.valueOf(count)));

        FollowHelper.getCount(profileUid, "Following", count ->
                tvFollowing.setText(String.valueOf(count)));
    }

    private void uploadProfileImage(Uri uri) {

        Toast.makeText(this, "Uploading photo...", Toast.LENGTH_SHORT).show();

        btnUploadImage.setEnabled(false);

        CloudinaryUploader.uploadImage(this, uri, new CloudinaryUploader.UploadCallback() {

            @Override
            public void onSuccess(String url) {

                firestore.collection("Users")
                        .document(myUid)
                        .update("image", url)
                        .addOnSuccessListener(unused -> {

                            btnUploadImage.setEnabled(true);

                            // keep the photo shown on old posts in sync
                            PostHelper.syncAuthorInfo(myUid, null, null, url);

                            if (!isDestroyed()) {
                                Toast.makeText(ProfileActivity.this,
                                        "Profile photo updated", Toast.LENGTH_SHORT).show();
                                loadPosts();
                            }
                        })
                        .addOnFailureListener(e -> {
                            btnUploadImage.setEnabled(true);
                            Toast.makeText(ProfileActivity.this,
                                    "Couldn't save photo", Toast.LENGTH_LONG).show();
                            loadUserProfile();
                        });
            }

            @Override
            public void onFailure(Exception e) {
                btnUploadImage.setEnabled(true);
                Toast.makeText(ProfileActivity.this,
                        e.getMessage() == null ? "Upload failed" : e.getMessage(),
                        Toast.LENGTH_LONG).show();
                loadUserProfile();
            }
        });
    }
}
