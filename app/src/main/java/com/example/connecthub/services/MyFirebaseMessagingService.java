package com.example.connecthub.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService
        extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {

        super.onNewToken(token);

        Log.d("FCM", "Token : " + token);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }

        String uid =
                FirebaseAuth.getInstance()
                        .getCurrentUser()
                        .getUid();

        Map<String, Object> map = new HashMap<>();

        map.put("fcmToken", token);

        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(uid)
                .update(map);

    }

}