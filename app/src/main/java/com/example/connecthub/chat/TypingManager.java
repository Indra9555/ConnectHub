package com.example.connecthub.chat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class TypingManager {

    private final FirebaseFirestore firestore;

    public TypingManager() {
        firestore = FirebaseFirestore.getInstance();
    }

    public void setTyping(
            String currentUserId,
            String receiverId,
            boolean typing
    ) {

        Map<String, Object> data = new HashMap<>();
        data.put("typing", typing);

        firestore.collection("Chats")
                .document(currentUserId)
                .collection("Typing")
                .document(receiverId)
                .set(data);

    }

}