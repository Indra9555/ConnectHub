package com.example.connecthub.chat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PresenceManager {

    private final FirebaseFirestore firestore;

    public PresenceManager() {
        firestore = FirebaseFirestore.getInstance();
    }

    public void setOnline(String uid) {

        Map<String, Object> data = new HashMap<>();

        data.put("online", true);
        data.put("lastSeen", System.currentTimeMillis());

        firestore.collection("Users")
                .document(uid)
                .update(data);

    }

    public void setOffline(String uid) {

        Map<String, Object> data = new HashMap<>();

        data.put("online", false);
        data.put("lastSeen", System.currentTimeMillis());

        firestore.collection("Users")
                .document(uid)
                .update(data);

    }

}
