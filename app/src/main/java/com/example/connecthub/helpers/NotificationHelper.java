package com.example.connecthub.helpers;

import com.example.connecthub.models.Notification;
import com.google.firebase.firestore.FirebaseFirestore;

public class NotificationHelper {

    private NotificationHelper() {
    }

    /**
     * Creates an in-app notification.
     *
     * "like" and "follow" notifications use a deterministic document id, so liking /
     * unliking / liking again (or following / unfollowing / following again) updates ONE
     * notification instead of piling up duplicates.
     */
    public static void sendNotification(
            String senderId,
            String receiverId,
            String type,
            String postId,
            String message
    ) {

        if (senderId == null || receiverId == null) {
            return;
        }

        // Don't notify yourself
        if (senderId.equals(receiverId)) {
            return;
        }

        Notification notification = new Notification(
                senderId,
                receiverId,
                type,
                postId == null ? "" : postId,
                message,
                System.currentTimeMillis()
        );

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        String id = stableId(senderId, receiverId, type, postId);

        if (id != null) {
            firestore.collection("Notifications").document(id).set(notification);
        } else {
            firestore.collection("Notifications").add(notification);
        }
    }

    /** Removes the like/follow notification again (used when the user un-likes / un-follows). */
    public static void removeNotification(
            String senderId,
            String receiverId,
            String type,
            String postId
    ) {

        if (senderId == null || receiverId == null) {
            return;
        }

        String id = stableId(senderId, receiverId, type, postId);

        if (id != null) {
            FirebaseFirestore.getInstance()
                    .collection("Notifications")
                    .document(id)
                    .delete();
        }
    }

    private static String stableId(
            String senderId,
            String receiverId,
            String type,
            String postId
    ) {
        if ("like".equals(type)) {
            return "like_" + senderId + "_" + postId;
        }
        if ("follow".equals(type)) {
            return "follow_" + senderId + "_" + receiverId;
        }
        return null;
    }
}
