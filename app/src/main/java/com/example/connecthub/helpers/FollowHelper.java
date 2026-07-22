package com.example.connecthub.helpers;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class FollowHelper {

    private static final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public interface FollowCallback {
        void onResult(boolean following);
    }

    // Helper method to get the fresh current UID safely
    private static String getCurrentUid() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        return "";
    }

    public static void isFollowing(String targetUid, FollowCallback callback) {
        String currentUid = getCurrentUid();
        if (currentUid.isEmpty()) return;

        firestore.collection("Users")
                .document(currentUid)
                .collection("Following")
                .document(targetUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (callback != null) {
                        callback.onResult(documentSnapshot.exists());
                    }
                });
    }

    public static void toggleFollow(String targetUid) {
        String currentUid = getCurrentUid();
        if (currentUid.isEmpty()) return;

        firestore.collection("Users")
                .document(currentUid)
                .collection("Following")
                .document(targetUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Unfollow logic
                        firestore.collection("Users")
                                .document(currentUid)
                                .collection("Following")
                                .document(targetUid)
                                .delete();

                        firestore.collection("Users")
                                .document(targetUid)
                                .collection("Followers")
                                .document(currentUid)
                                .delete();
                    } else {
                        // Follow logic
                        firestore.collection("Users")
                                .document(currentUid)
                                .collection("Following")
                                .document(targetUid)
                                .set(new HashMap<>());

                        firestore.collection("Users")
                                .document(targetUid)
                                .collection("Followers")
                                .document(currentUid)
                                .set(new HashMap<>())
                                .addOnSuccessListener(unused -> {
                                    NotificationHelper.sendNotification(
                                            currentUid,
                                            targetUid,
                                            "follow",
                                            "",
                                            "started following you"
                                    );
                                });
                    }
                });
    }
}