package com.example.connecthub.helpers;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

/** Post related helpers that touch more than one document. */
public class PostHelper {

    private PostHelper() {
    }

    public interface Callback {
        void onSuccess();

        void onFailure(@NonNull Exception e);
    }

    /**
     * Deletes a post together with its comments and likes (Firestore does not delete
     * sub-collections automatically, so they used to stay behind as orphans).
     */
    public static void deletePost(String postId, Callback callback) {

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        DocumentReference postRef = firestore.collection("Posts").document(postId);

        Task<QuerySnapshot> comments = postRef.collection("Comments").get();
        Task<QuerySnapshot> likes = postRef.collection("Likes").get();

        Tasks.whenAllSuccess(comments, likes)
                .addOnSuccessListener(results -> {

                    WriteBatch batch = firestore.batch();
                    int operations = 0;

                    for (DocumentSnapshot doc : comments.getResult().getDocuments()) {
                        if (operations >= 450) break;
                        batch.delete(doc.getReference());
                        operations++;
                    }

                    for (DocumentSnapshot doc : likes.getResult().getDocuments()) {
                        if (operations >= 450) break;
                        batch.delete(doc.getReference());
                        operations++;
                    }

                    batch.delete(postRef);

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                if (callback != null) callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) callback.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    /**
     * Posts store a copy of the author's name / username / photo. When the profile
     * changes, update those copies so old posts don't keep showing stale data.
     */
    public static void syncAuthorInfo(
            String uid,
            String name,
            String username,
            String image
    ) {

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        firestore.collection("Posts")
                .whereEqualTo("uid", uid)
                .limit(400)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.isEmpty()) return;

                    Map<String, Object> updates = new HashMap<>();

                    if (name != null) updates.put("name", name);
                    if (username != null) updates.put("username", username);
                    if (image != null) updates.put("userImage", image);

                    if (updates.isEmpty()) return;

                    WriteBatch batch = firestore.batch();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        batch.update(doc.getReference(), updates);
                    }

                    batch.commit();
                });
    }
}
