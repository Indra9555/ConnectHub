package com.example.connecthub.helpers;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.Map;

public class LikeHelper {

    private LikeHelper() {
    }

    public interface LikeStatusCallback {
        void onResult(boolean liked);
    }

    public interface ToggleCallback {
        /** @param liked the NEW state after the toggle */
        void onSuccess(boolean liked, long newCount);

        void onFailure(@NonNull Exception e);
    }

    public static void isLiked(String postId, LikeStatusCallback callback) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null || postId == null) {
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("Posts")
                .document(postId)
                .collection("Likes")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (callback != null) {
                        callback.onResult(snapshot.exists());
                    }
                });
    }

    /**
     * Toggles the like atomically.
     *
     * The like document and the counter are changed in ONE transaction, so rapid taps
     * (or two devices) can no longer leave the counter out of sync with the Likes
     * sub-collection, which the old implementation did.
     */
    public static void toggleLike(
            String postId,
            String postOwnerId,
            ToggleCallback callback
    ) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null || postId == null) {
            return;
        }

        String uid = user.getUid();

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        DocumentReference postRef = firestore.collection("Posts").document(postId);
        DocumentReference likeRef = postRef.collection("Likes").document(uid);

        final long[] resultCount = new long[1];

        firestore.runTransaction(transaction -> {

                    DocumentSnapshot postSnapshot = transaction.get(postRef);
                    DocumentSnapshot likeSnapshot = transaction.get(likeRef);

                    if (!postSnapshot.exists()) {
                        throw new FirebaseFirestoreException(
                                "This post no longer exists",
                                FirebaseFirestoreException.Code.NOT_FOUND
                        );
                    }

                    Long stored = postSnapshot.getLong("likesCount");
                    long current = stored == null ? 0 : stored;

                    boolean nowLiked;

                    if (likeSnapshot.exists()) {

                        transaction.delete(likeRef);
                        current = Math.max(0, current - 1);
                        nowLiked = false;

                    } else {

                        Map<String, Object> like = new HashMap<>();
                        like.put("timestamp", System.currentTimeMillis());

                        transaction.set(likeRef, like);
                        current = current + 1;
                        nowLiked = true;
                    }

                    transaction.update(postRef, "likesCount", current);

                    resultCount[0] = current;

                    return nowLiked;
                })
                .addOnSuccessListener(liked -> {

                    if (liked) {
                        NotificationHelper.sendNotification(
                                uid, postOwnerId, "like", postId, "liked your post");
                    } else {
                        NotificationHelper.removeNotification(
                                uid, postOwnerId, "like", postId);
                    }

                    if (callback != null) {
                        callback.onSuccess(liked, resultCount[0]);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }
}
