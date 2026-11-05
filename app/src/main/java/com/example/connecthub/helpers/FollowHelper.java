package com.example.connecthub.helpers;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class FollowHelper {

    private FollowHelper() {
    }

    public interface FollowCallback {
        void onResult(boolean following);
    }

    public interface CountCallback {
        void onCount(long count);
    }

    public interface ToggleCallback {
        /** @param following the NEW state after the toggle */
        void onSuccess(boolean following);

        void onFailure(@NonNull Exception e);
    }

    private static String getCurrentUid() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        return "";
    }

    public static void isFollowing(String targetUid, FollowCallback callback) {

        String currentUid = getCurrentUid();

        if (currentUid.isEmpty() || targetUid == null) {
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(currentUid)
                .collection("Following")
                .document(targetUid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (callback != null) {
                        callback.onResult(snapshot.exists());
                    }
                });
    }

    /**
     * Follows / unfollows. Both sides of the relationship (my "Following" and their
     * "Followers") are written in a single batch so they can never get out of sync.
     */
    public static void toggleFollow(String targetUid, ToggleCallback callback) {

        String currentUid = getCurrentUid();

        if (currentUid.isEmpty() || targetUid == null || currentUid.equals(targetUid)) {
            return;
        }

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        DocumentReference followingRef = firestore.collection("Users")
                .document(currentUid)
                .collection("Following")
                .document(targetUid);

        DocumentReference followerRef = firestore.collection("Users")
                .document(targetUid)
                .collection("Followers")
                .document(currentUid);

        followingRef.get()
                .addOnSuccessListener(snapshot -> {

                    boolean wasFollowing = snapshot.exists();

                    WriteBatch batch = firestore.batch();

                    if (wasFollowing) {

                        batch.delete(followingRef);
                        batch.delete(followerRef);

                    } else {

                        Map<String, Object> data = new HashMap<>();
                        data.put("timestamp", System.currentTimeMillis());

                        batch.set(followingRef, data);
                        batch.set(followerRef, data);
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> {

                                if (wasFollowing) {
                                    NotificationHelper.removeNotification(
                                            currentUid, targetUid, "follow", "");
                                } else {
                                    NotificationHelper.sendNotification(
                                            currentUid,
                                            targetUid,
                                            "follow",
                                            "",
                                            "started following you"
                                    );
                                }

                                if (callback != null) {
                                    callback.onSuccess(!wasFollowing);
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) {
                                    callback.onFailure(e);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    /**
     * Number of documents in Users/{uid}/{subCollection}. Uses a server-side count
     * (1 read) instead of downloading every follower document like before.
     */
    public static void getCount(String uid, String subCollection, CountCallback callback) {

        CollectionReference collection = FirebaseFirestore.getInstance()
                .collection("Users")
                .document(uid)
                .collection(subCollection);

        collection.count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(result -> callback.onCount(result.getCount()))
                .addOnFailureListener(e ->
                        // Offline: fall back to whatever is cached
                        collection.get()
                                .addOnSuccessListener(snapshot ->
                                        callback.onCount(snapshot.size())));
    }
}
