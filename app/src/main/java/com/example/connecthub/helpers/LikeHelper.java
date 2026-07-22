package com.example.connecthub.helpers;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class LikeHelper {

    public interface LikeStatusCallback {
        void onResult(boolean liked);
    }

    public static void isLiked(
            String postId,
            LikeStatusCallback callback) {

        String uid = FirebaseAuth.getInstance()
                .getCurrentUser()
                .getUid();

        FirebaseFirestore.getInstance()
                .collection("Posts")
                .document(postId)
                .collection("Likes")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    callback.onResult(documentSnapshot.exists());

                });

    }

    public static void toggleLike(String postId) {

        FirebaseFirestore firestore =
                FirebaseFirestore.getInstance();

        String uid = FirebaseAuth.getInstance()
                .getCurrentUser()
                .getUid();

        firestore.collection("Posts")
                .document(postId)
                .collection("Likes")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {

                        // Unlike

                        firestore.collection("Posts")
                                .document(postId)
                                .collection("Likes")
                                .document(uid)
                                .delete();

                        firestore.collection("Posts")
                                .document(postId)
                                .update(
                                        "likesCount",
                                        FieldValue.increment(-1)
                                );

                    } else {

                        // Like

                        firestore.collection("Posts")
                                .document(postId)
                                .collection("Likes")
                                .document(uid)
                                .set(new HashMap<>());

                        firestore.collection("Posts")
                                .document(postId)
                                .update(
                                        "likesCount",
                                        FieldValue.increment(1)
                                );

                    }

                });

    }

}