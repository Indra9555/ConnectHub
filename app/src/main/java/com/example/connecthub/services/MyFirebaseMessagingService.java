package com.example.connecthub.services;

import android.app.PendingIntent;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

import com.example.connecthub.ConnectHubApp;
import com.example.connecthub.R;
import com.example.connecthub.activities.ChatActivity;
import com.example.connecthub.activities.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(uid)
                .update("fcmToken", token);
    }

    /**
     * Called while the app is in the foreground (the system shows the notification itself
     * when the app is in the background). We show a heads-up unless the user is already
     * looking at that very chat.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        Map<String, String> data = message.getData();

        String type = data.get("type");
        String groupId = data.get("groupId");
        String senderId = data.get("senderId");

        boolean isGroup = "group".equals(type) && groupId != null && !groupId.isEmpty();
        boolean isChat = "chat".equals(type) && senderId != null;

        // Likes / comments / follows are not tied to a chat
        String chatKey = isGroup ? groupId : (isChat ? senderId : null);

        if (chatKey != null && chatKey.equals(ConnectHubApp.activeChatKey)) {
            return;
        }

        String title = message.getNotification() != null
                ? message.getNotification().getTitle() : data.get("title");
        String body = message.getNotification() != null
                ? message.getNotification().getBody() : data.get("body");

        if (title == null && body == null) {
            return;
        }

        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            return;
        }

        TaskStackBuilder stack = TaskStackBuilder.create(this)
                .addNextIntent(new Intent(this, MainActivity.class));

        if (chatKey != null) {

            Intent chat = new Intent(this, ChatActivity.class);

            if (isGroup) {
                chat.putExtra("isGroup", true);
                chat.putExtra("groupId", groupId);
            } else {
                chat.putExtra("uid", senderId);
            }

            stack.addNextIntent(chat);
        }

        PendingIntent pendingIntent = stack.getPendingIntent(
                (chatKey == null ? "x" : chatKey).hashCode(),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, ConnectHubApp.CHANNEL_MESSAGES)
                        .setSmallIcon(R.drawable.ic_stat_notification)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(this).notify(
                    (chatKey == null ? "x" : chatKey).hashCode(),
                    builder.build()
            );
        } catch (SecurityException ignored) {
            // POST_NOTIFICATIONS not granted
        }
    }
}
