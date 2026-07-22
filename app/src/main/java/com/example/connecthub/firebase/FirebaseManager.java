package com.example.connecthub.firebase;

import com.example.connecthub.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseManager {

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public FirebaseManager() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    public FirebaseAuth getAuth() {
        return auth;
    }

    public FirebaseFirestore getFirestore() {
        return firestore;
    }

    public void saveUser(User user,
                         OnUserSavedListener listener) {

        firestore.collection("Users")
                .document(user.getUid())
                .set(user)
                .addOnSuccessListener(unused ->
                        listener.onSuccess())
                .addOnFailureListener(listener::onFailure);

    }

    public interface OnUserSavedListener {

        void onSuccess();

        void onFailure(Exception e);

    }

}