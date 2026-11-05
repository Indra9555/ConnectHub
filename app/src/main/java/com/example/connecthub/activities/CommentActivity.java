package com.example.connecthub.activities;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connecthub.R;
import com.example.connecthub.adapters.CommentAdapter;
import com.example.connecthub.helpers.NotificationHelper;
import com.example.connecthub.models.Comment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentActivity extends BaseActivity {

    private RecyclerView recyclerComments;
    private EditText etComment;
    private ImageButton btnSend;
    private TextView tvEmpty;

    private final List<Comment> commentList = new ArrayList<>();
    private CommentAdapter adapter;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    private String postId;
    private boolean sending = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        postId = getIntent().getStringExtra("postId");

        if (postId == null || postId.isEmpty()) {
            Toast.makeText(this, "Post not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.commentToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerComments = findViewById(R.id.recyclerComments);
        etComment = findViewById(R.id.etComment);
        btnSend = findViewById(R.id.btnSendComment);
        tvEmpty = findViewById(R.id.tvEmpty);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        adapter = new CommentAdapter(commentList);

        recyclerComments.setLayoutManager(new LinearLayoutManager(this));
        recyclerComments.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendComment());

        etComment.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendComment();
                return true;
            }
            return false;
        });

        loadComments();
    }

    private void loadComments() {

        firestore.collection("Posts")
                .document(postId)
                .collection("Comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(this, (value, error) -> {

                    if (value == null) return;

                    int previousSize = commentList.size();

                    commentList.clear();

                    for (DocumentSnapshot document : value.getDocuments()) {

                        Comment comment = document.toObject(Comment.class);

                        if (comment != null) {
                            commentList.add(comment);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    tvEmpty.setVisibility(commentList.isEmpty() ? View.VISIBLE : View.GONE);

                    // Jump to the newest comment when one was added
                    if (commentList.size() > previousSize && !commentList.isEmpty()) {
                        recyclerComments.scrollToPosition(commentList.size() - 1);
                    }
                });
    }

    private void sendComment() {

        if (sending) return;

        String text = etComment.getText().toString().trim();

        if (text.isEmpty()) {
            return;
        }

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
            return;
        }

        sending = true;
        btnSend.setEnabled(false);

        String uid = user.getUid();

        DocumentReference postRef = firestore.collection("Posts").document(postId);

        postRef.get().addOnSuccessListener(postDoc -> {

            if (!postDoc.exists()) {
                finishSending();
                Toast.makeText(this, "This post no longer exists", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            String postOwnerUid = postDoc.getString("uid");

            firestore.collection("Users").document(uid).get().addOnSuccessListener(userDoc -> {

                DocumentReference commentRef = postRef.collection("Comments").document();

                Map<String, Object> commentMap = new HashMap<>();
                commentMap.put("commentId", commentRef.getId());
                commentMap.put("uid", uid);
                commentMap.put("name", userDoc.getString("name"));
                commentMap.put("username", userDoc.getString("username"));
                commentMap.put("userImage", userDoc.getString("image"));
                commentMap.put("comment", text);
                commentMap.put("timestamp", System.currentTimeMillis());

                // comment + counter in ONE atomic write
                WriteBatch batch = firestore.batch();
                batch.set(commentRef, commentMap);
                batch.update(postRef, "commentsCount", FieldValue.increment(1));

                batch.commit()
                        .addOnSuccessListener(unused -> {

                            NotificationHelper.sendNotification(
                                    uid,
                                    postOwnerUid,
                                    "comment",
                                    postId,
                                    "commented: " + text
                            );

                            etComment.setText("");
                            finishSending();
                        })
                        .addOnFailureListener(e -> {
                            finishSending();
                            Toast.makeText(this, "Couldn't post comment", Toast.LENGTH_SHORT).show();
                        });

            }).addOnFailureListener(e -> {
                finishSending();
                Toast.makeText(this, "Couldn't post comment", Toast.LENGTH_SHORT).show();
            });

        }).addOnFailureListener(e -> {
            finishSending();
            Toast.makeText(this, "Couldn't post comment", Toast.LENGTH_SHORT).show();
        });
    }

    private void finishSending() {
        sending = false;
        btnSend.setEnabled(true);
    }
}
