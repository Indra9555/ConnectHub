package com.example.connecthub.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connecthub.R;
import com.example.connecthub.adapters.CommentAdapter;
import com.example.connecthub.helpers.NotificationHelper;
import com.example.connecthub.models.Comment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CommentActivity extends AppCompatActivity {


    private MaterialToolbar toolbar;

    private RecyclerView recyclerComments;

    private TextInputEditText etComment;

    private Button btnSend;


    private List<Comment> commentList;

    private CommentAdapter adapter;


    private FirebaseFirestore firestore;

    private FirebaseAuth auth;


    private String postId;



    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_comment);



        toolbar = findViewById(R.id.commentToolbar);

        recyclerComments = findViewById(R.id.recyclerComments);

        etComment = findViewById(R.id.etComment);

        btnSend = findViewById(R.id.btnSendComment);



        firestore = FirebaseFirestore.getInstance();

        auth = FirebaseAuth.getInstance();



        postId = getIntent().getStringExtra("postId");



        commentList = new ArrayList<>();


        adapter = new CommentAdapter(commentList);



        recyclerComments.setLayoutManager(
                new LinearLayoutManager(this)
        );


        recyclerComments.setAdapter(adapter);



        loadComments();



        btnSend.setOnClickListener(v -> {

            sendComment();

        });



        toolbar.setNavigationOnClickListener(v -> finish());


    }






    private void loadComments() {


        firestore.collection("Posts")
                .document(postId)
                .collection("Comments")
                .orderBy("timestamp")
                .addSnapshotListener((value, error) -> {



                    if(value == null){

                        return;

                    }



                    commentList.clear();



                    for(DocumentSnapshot document : value.getDocuments()){



                        Comment comment =
                                document.toObject(Comment.class);



                        if(comment != null){

                            commentList.add(comment);

                        }


                    }



                    adapter.notifyDataSetChanged();



                });


    }








    private void sendComment() {

        String text =
                etComment.getText()
                        .toString()
                        .trim();

        if (text.isEmpty()) {

            etComment.setError("Enter a comment");
            return;
        }

        if (auth.getCurrentUser() == null) {

            Toast.makeText(
                    this,
                    "Login required",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String uid =
                auth.getCurrentUser().getUid();

        firestore.collection("Posts")
                .document(postId)
                .get()
                .addOnSuccessListener(postDoc -> {

                    String postOwnerUid =
                            postDoc.getString("uid");

                    firestore.collection("Users")
                            .document(uid)
                            .get()
                            .addOnSuccessListener(userDoc -> {

                                String commentId =
                                        firestore.collection("Posts")
                                                .document(postId)
                                                .collection("Comments")
                                                .document()
                                                .getId();

                                Map<String, Object> commentMap =
                                        new HashMap<>();

                                commentMap.put(
                                        "commentId",
                                        commentId
                                );

                                commentMap.put(
                                        "uid",
                                        uid
                                );

                                commentMap.put(
                                        "name",
                                        userDoc.getString("name")
                                );

                                commentMap.put(
                                        "username",
                                        userDoc.getString("username")
                                );

                                commentMap.put(
                                        "userImage",
                                        userDoc.getString("image")
                                );

                                commentMap.put(
                                        "comment",
                                        text
                                );

                                commentMap.put(
                                        "timestamp",
                                        System.currentTimeMillis()
                                );

                                firestore.collection("Posts")
                                        .document(postId)
                                        .collection("Comments")
                                        .document(commentId)
                                        .set(commentMap)
                                        .addOnSuccessListener(unused -> {

                                            firestore.collection("Posts")
                                                    .document(postId)
                                                    .update(
                                                            "commentsCount",
                                                            FieldValue.increment(1)
                                                    );

                                            NotificationHelper.sendNotification(
                                                    uid,
                                                    postOwnerUid,
                                                    "comment",
                                                    postId,
                                                    "commented: " + text
                                            );

                                            etComment.setText("");

                                            Toast.makeText(
                                                    CommentActivity.this,
                                                    "Comment Added",
                                                    Toast.LENGTH_SHORT
                                            ).show();

                                        });

                            });

                });

    }



}