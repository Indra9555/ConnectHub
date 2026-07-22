package com.example.connecthub.activities;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connecthub.adapters.PostAdapter;
import com.example.connecthub.models.Post;

import java.util.ArrayList;
import java.util.List;
import com.example.connecthub.helpers.FollowHelper;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.connecthub.R;
import com.example.connecthub.network.CloudinaryUploader;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;


public class ProfileActivity extends AppCompatActivity {


    private ShapeableImageView imgProfile;

    private String profileUid;

    private TextView tvName;
    private TextView tvUsername;
    private TextView tvBio;
    private TextView tvEmail;


    private Button btnEditProfile;
    private MaterialButton btnFollow;
    private Button btnUploadImage;


    private ProgressBar progressBar;


    private FirebaseAuth auth;
    private FirebaseFirestore firestore;


    private Uri imageUri;
    private RecyclerView recyclerMyPosts;

    private PostAdapter postAdapter;

    private List<Post> myPosts;
    private TextView tvPostCount;
    private TextView tvFollowers;
    private TextView tvFollowing;



    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_profile);
        btnFollow = findViewById(R.id.btnFollow);



        imgProfile = findViewById(R.id.imgProfile);

        tvName = findViewById(R.id.tvName);

        tvUsername = findViewById(R.id.tvUsername);

        tvBio = findViewById(R.id.tvBio);

        tvEmail = findViewById(R.id.tvEmail);


        btnEditProfile = findViewById(R.id.btnEditProfile);

        btnUploadImage = findViewById(R.id.btnUploadImage);


        progressBar = findViewById(R.id.progressBar);
        recyclerMyPosts =
                findViewById(R.id.recyclerMyPosts);


        tvPostCount =
                findViewById(R.id.tvPostCount);


        tvFollowers =
                findViewById(R.id.tvFollowers);


        tvFollowing =
                findViewById(R.id.tvFollowing);





        auth = FirebaseAuth.getInstance();

        firestore = FirebaseFirestore.getInstance();
        profileUid = getIntent().getStringExtra("uid");
        if (profileUid == null) {

            profileUid = auth.getCurrentUser().getUid();

        }
        if (profileUid.equals(auth.getCurrentUser().getUid())) {

            btnFollow.setVisibility(View.GONE);

            btnEditProfile.setVisibility(View.VISIBLE);

            btnUploadImage.setVisibility(View.VISIBLE);

        } else {

            btnFollow.setVisibility(View.VISIBLE);

            btnEditProfile.setVisibility(View.GONE);

            btnUploadImage.setVisibility(View.GONE);

        }
        if (!profileUid.equals(auth.getCurrentUser().getUid())) {

            FollowHelper.isFollowing(profileUid, following -> {

                runOnUiThread(() -> {

                    if (following) {

                        btnFollow.setText("Following");

                    } else {

                        btnFollow.setText("Follow");

                    }

                });

            });

        }





        myPosts = new ArrayList<>();

        postAdapter = new PostAdapter(myPosts);

        recyclerMyPosts.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerMyPosts.setAdapter(postAdapter);

        loadUserProfile();
        loadMyPosts();
        loadFollowCounts();






        imgProfile.setOnClickListener(v -> {
            if (profileUid.equals(auth.getCurrentUser().getUid())) {
                openGallery();
            }
        });
        btnFollow.setOnClickListener(v -> {

            FollowHelper.toggleFollow(profileUid);

            btnFollow.postDelayed(() -> {

                FollowHelper.isFollowing(profileUid, following -> {

                    runOnUiThread(() -> {

                        if (following) {
                            btnFollow.setText("Following");
                        } else {
                            btnFollow.setText("Follow");
                        }

                        loadFollowCounts();

                    });

                });

            }, 500);

        });



        btnUploadImage.setOnClickListener(v -> {


            if(imageUri != null){


                uploadImageToCloudinary();


            }
            else{


                Toast.makeText(
                        this,
                        "Select image first",
                        Toast.LENGTH_SHORT
                ).show();


            }


        });



        btnEditProfile.setOnClickListener(v -> {


            Intent intent = new Intent(
                    ProfileActivity.this,
                    EditProfileActivity.class
            );


            startActivity(intent);


        });


    }





    private void loadUserProfile() {


        if(auth.getCurrentUser() == null){

            return;

        }


        progressBar.setVisibility(View.VISIBLE);



        firestore.collection("Users")
                .document(profileUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {



                    if(documentSnapshot.exists()){


                        tvName.setText(documentSnapshot.getString("name") != null
                                ? documentSnapshot.getString("name")
                                : "");

                        tvUsername.setText("@" + (
                                documentSnapshot.getString("username") != null
                                        ? documentSnapshot.getString("username")
                                        : ""));

                        tvBio.setText(documentSnapshot.getString("bio") != null
                                ? documentSnapshot.getString("bio")
                                : "");

                        tvEmail.setText(documentSnapshot.getString("email") != null
                                ? documentSnapshot.getString("email")
                                : "");



                        // Load Profile Image

                        String imageUrl =
                                documentSnapshot.getString("image");



                        if(imageUrl != null && !imageUrl.isEmpty()){


                            Glide.with(ProfileActivity.this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.ic_person)
                                    .error(R.drawable.ic_person)
                                    .into(imgProfile);


                        }
                        else{


                            imgProfile.setImageResource(R.drawable.ic_person);


                        }



                    }



                    progressBar.setVisibility(View.GONE);



                })
                .addOnFailureListener(e -> {


                    progressBar.setVisibility(View.GONE);


                    Toast.makeText(
                            this,
                            e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();


                });


    }





    private void openGallery(){


        Intent intent =
                new Intent(Intent.ACTION_PICK);


        intent.setType("image/*");


        startActivityForResult(
                intent,
                100
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



        if(requestCode == 100 &&
                resultCode == RESULT_OK &&
                data != null){


            imageUri = data.getData();


            imgProfile.setImageURI(imageUri);


        }


    }





    private void uploadImageToCloudinary(){


        Toast.makeText(
                this,
                "Uploading image...",
                Toast.LENGTH_SHORT
        ).show();



        CloudinaryUploader.uploadImage(
                this,
                imageUri,
                new CloudinaryUploader.UploadCallback() {


                    @Override
                    public void onSuccess(String imageUrl) {


                        runOnUiThread(() -> {

                            saveImageUrl(imageUrl);

                        });


                    }



                    @Override
                    public void onFailure(Exception e) {


                        runOnUiThread(() -> {


                            Toast.makeText(
                                    ProfileActivity.this,
                                    e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();


                        });


                    }


                });


    }





    private void saveImageUrl(String imageUrl){



        String uid =
                auth.getCurrentUser().getUid();



        Map<String,Object> map =
                new HashMap<>();


        map.put("image", imageUrl);



        firestore.collection("Users")
                .document(uid)
                .update(map)
                .addOnSuccessListener(unused -> {


                    Toast.makeText(
                            this,
                            "Profile image updated",
                            Toast.LENGTH_SHORT
                    ).show();



                    loadUserProfile();



                })
                .addOnFailureListener(e -> {


                    Toast.makeText(
                            this,
                            e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();


                });



    }






    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
        loadMyPosts();
        loadFollowCounts();
    }
    private void loadMyPosts(){



        firestore.collection("Posts")
                .whereEqualTo("uid", profileUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {


                    myPosts.clear();


                    for(DocumentSnapshot doc : queryDocumentSnapshots){

                        Post post =
                                doc.toObject(Post.class);


                        if(post != null){

                            myPosts.add(post);

                        }

                    }
                    postAdapter.notifyDataSetChanged();


                    tvPostCount.setText(
                            String.valueOf(myPosts.size())

                    );



                });

    }
    private void loadFollowCounts() {

        firestore.collection("Users")
                .document(profileUid)
                .collection("Followers")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    tvFollowers.setText(
                            String.valueOf(queryDocumentSnapshots.size())
                    );

                });

        firestore.collection("Users")
                .document(profileUid)
                .collection("Following")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    tvFollowing.setText(
                            String.valueOf(queryDocumentSnapshots.size())
                    );

                });

    }




}