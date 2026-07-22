package com.example.connecthub.helpers;

import com.example.connecthub.models.Notification;
import com.google.firebase.firestore.FirebaseFirestore;

public class NotificationHelper {

    private static final FirebaseFirestore firestore =
            FirebaseFirestore.getInstance();

    public static void sendNotification(
            String senderId,
            String receiverId,
            String type,
            String postId,
            String message
    ) {

        // Don't notify yourself
        if (senderId.equals(receiverId)) {
            return;
        }

        Notification notification = new Notification(
                senderId,
                receiverId,
                type,
                postId,
                message,
                System.currentTimeMillis()
        );

        firestore.collection("Notifications")
                .add(notification);

    }
}