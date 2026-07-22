package com.example.connecthub.repository;

import com.example.connecthub.models.Message;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

public class ChatRepository {

    private final FirebaseFirestore firestore;

    public ChatRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public void sendMessage(
            Message message,
            OnSuccessListener<Void> success,
            OnFailureListener failure
    ) {

        firestore.collection("Messages")
                .add(message)
                .addOnSuccessListener(documentReference -> success.onSuccess(null))
                .addOnFailureListener(failure);

    }

    public FirebaseFirestore getFirestore() {
        return firestore;
    }
}