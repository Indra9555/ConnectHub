package com.example.connecthub.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.bumptech.glide.Glide;
import com.example.connecthub.R;
import com.example.connecthub.models.Post;
import com.example.connecthub.network.CloudinaryUploader;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class CreatePostActivity extends BaseActivity {

    private ShapeableImageView imgPostPreview;
    private ShapeableImageView imgUser;
    private View layoutPreview;
    private MaterialButton btnSelectImage;
    private MaterialButton btnPost;
    private TextView tvUsername;
    private TextInputEditText etPost;

    private Uri postImageUri;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    private String name;
    private String username;
    private String imageUrl;
    private boolean profileLoaded = false;

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    postImageUri = uri;
                    layoutPreview.setVisibility(View.VISIBLE);
                    imgPostPreview.setImageURI(uri);
                    btnSelectImage.setText("Change photo");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        imgPostPreview = findViewById(R.id.imgPostPreview);
        imgUser = findViewById(R.id.imgUser);
        layoutPreview = findViewById(R.id.layoutPreview);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnPost = findViewById(R.id.btnPost);
        tvUsername = findViewById(R.id.tvUsername);
        etPost = findViewById(R.id.etPost);
        ImageButton btnRemoveImage = findViewById(R.id.btnRemoveImage);

        btnSelectImage.setOnClickListener(v -> imagePicker.launch("image/*"));

        btnRemoveImage.setOnClickListener(v -> {
            postImageUri = null;
            layoutPreview.setVisibility(View.GONE);
            btnSelectImage.setText("Add photo");
        });

        btnPost.setOnClickListener(v -> createPost());

        loadCurrentUser();
    }

    private void loadCurrentUser() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            finish();
            return;
        }

        firestore.collection("Users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {

                    if (isDestroyed()) return;

                    name = document.getString("name");
                    username = document.getString("username");
                    imageUrl = document.getString("image");

                    profileLoaded = true;

                    tvUsername.setText(name == null ? "" : name);

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(this)
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_person)
                                .into(imgUser);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Couldn't load your profile", Toast.LENGTH_SHORT).show());
    }

    private void setPosting(boolean posting) {
        btnPost.setEnabled(!posting);
        btnSelectImage.setEnabled(!posting);
        btnPost.setText(posting ? "Posting..." : "Post");
    }

    private void createPost() {

        String content = etPost.getText() == null ? "" : etPost.getText().toString().trim();

        // A post needs text OR a photo (photo-only posts were impossible before)
        if (content.isEmpty() && postImageUri == null) {
            etPost.setError("Write something or add a photo");
            etPost.requestFocus();
            return;
        }

        if (!profileLoaded) {
            Toast.makeText(this, "Loading your profile, try again in a moment",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            finish();
            return;
        }

        setPosting(true);

        String uid = user.getUid();
        String postId = firestore.collection("Posts").document().getId();

        if (postImageUri == null) {
            createPostInFirestore(postId, uid, content, "");
            return;
        }

        CloudinaryUploader.uploadImage(this, postImageUri, new CloudinaryUploader.UploadCallback() {

            @Override
            public void onSuccess(String url) {
                createPostInFirestore(postId, uid, content, url);
            }

            @Override
            public void onFailure(Exception e) {
                setPosting(false);
                Toast.makeText(CreatePostActivity.this,
                        e.getMessage() == null ? "Image upload failed" : e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void createPostInFirestore(
            String postId,
            String uid,
            String content,
            String postImage
    ) {

        Post post = new Post(
                postId,
                uid,
                name == null ? "" : name,
                username == null ? "" : username,
                imageUrl == null ? "" : imageUrl,
                content,
                postImage,
                System.currentTimeMillis(),
                0,
                0
        );

        firestore.collection("Posts")
                .document(postId)
                .set(post)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Posted!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setPosting(false);
                    Toast.makeText(this,
                            e.getMessage() == null ? "Couldn't publish post" : e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
