package com.example.connecthub.activities;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import com.example.connecthub.network.CloudinaryUploader;

import com.bumptech.glide.Glide;
import com.example.connecthub.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import com.example.connecthub.models.Post;

import android.widget.TextView;
import android.widget.Toast;

public class CreatePostActivity extends AppCompatActivity {
    private ImageView imgPostPreview;

    private MaterialButton btnSelectImage;

    private Uri postImageUri;

    private MaterialToolbar toolbar;

    private ImageView imgUser;
    private TextView tvUsername;

    private TextInputEditText etPost;

    private MaterialButton btnPost;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    private String name;
    private String username;
    private String imageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        initializeViews();

        initializeFirebase();

        loadCurrentUser();

        toolbar.setNavigationOnClickListener(v -> finish());

        btnPost.setOnClickListener(v -> createPost());
        btnSelectImage.setOnClickListener(v -> {

            Intent intent =
                    new Intent(Intent.ACTION_PICK);

            intent.setType("image/*");

            startActivityForResult(
                    intent,
                    200
            );

        });
    }

    private void initializeViews() {

        imgPostPreview =
                findViewById(R.id.imgPostPreview);


        btnSelectImage =
                findViewById(R.id.btnSelectImage);

        toolbar = findViewById(R.id.toolbar);

        imgUser = findViewById(R.id.imgUser);

        tvUsername = findViewById(R.id.tvUsername);

        etPost = findViewById(R.id.etPost);

        btnPost = findViewById(R.id.btnPost);

    }

    private void initializeFirebase() {

        auth = FirebaseAuth.getInstance();

        firestore = FirebaseFirestore.getInstance();

    }

    private void loadCurrentUser() {

        String uid = auth.getCurrentUser().getUid();

        firestore.collection("Users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    name = document.getString("name");
                    username = document.getString("username");
                    imageUrl = document.getString("image");

                    tvUsername.setText(name);

                    if (imageUrl != null && !imageUrl.isEmpty()) {

                        Glide.with(this)
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_person)
                                .into(imgUser);

                    } else {

                        imgUser.setImageResource(R.drawable.ic_person);

                    }

                });

    }

    private void createPost() {

        String content = etPost.getText().toString().trim();

        if (content.isEmpty()) {

            etPost.setError("Write something...");
            etPost.requestFocus();
            return;

        }

        btnPost.setEnabled(false);
        btnPost.setText("Posting...");

        String uid = auth.getCurrentUser().getUid();

        String postId = firestore.collection("Posts")
                .document()
                .getId();

        savePost(
                postId,
                uid,
                content
        );



    }
    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );


        if(requestCode == 200 &&
                resultCode == RESULT_OK &&
                data != null){


            postImageUri =
                    data.getData();


            imgPostPreview.setVisibility(View.VISIBLE);


            imgPostPreview.setImageURI(
                    postImageUri
            );

        }

    }
    private void savePost(
            String postId,
            String uid,
            String content
    ) {


        if(postImageUri != null){


            CloudinaryUploader.uploadImage(
                    this,
                    postImageUri,
                    new CloudinaryUploader.UploadCallback() {


                        @Override
                        public void onSuccess(String imageUrl) {


                            createPostInFirestore(
                                    postId,
                                    uid,
                                    content,
                                    imageUrl
                            );


                        }


                        @Override
                        public void onFailure(Exception e) {


                            runOnUiThread(() -> {

                                btnPost.setEnabled(true);

                                Toast.makeText(
                                        CreatePostActivity.this,
                                        e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();

                            });


                        }

                    });


        }
        else{


            createPostInFirestore(
                    postId,
                    uid,
                    content,
                    ""
            );


        }

    }
    private void createPostInFirestore(
            String postId,
            String uid,
            String content,
            String postImage
    ){

        Post post =
                new Post(
                        postId,
                        uid,
                        name,
                        username,
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


                    btnPost.setEnabled(true);
                    btnPost.setText("POST");

                    finish();


                });


    }
}