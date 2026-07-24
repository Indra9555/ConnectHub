package com.example.connecthub.chat;

import com.example.connecthub.models.Message;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class ChatRepository {

    private final FirebaseFirestore firestore;

    public ChatRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public void sendMessage(
            Message message,
            OnSuccessListener<DocumentReference> success,
            OnFailureListener failure
    ) {

        firestore.collection("Messages")
                .add(message)
                .addOnSuccessListener(success)
                .addOnFailureListener(failure);

    }

    public FirebaseFirestore getFirestore() {
        return firestore;
    }
}