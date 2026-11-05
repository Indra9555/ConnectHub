package com.example.connecthub;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ConnectHubApp extends Application {

    public static final String CHANNEL_MESSAGES = "messages";

    /** uid (private chat) or groupId of the chat currently on screen; used to suppress duplicate pushes. */
    public static volatile String activeChatKey = null;

    private int activityReferences = 0;
    private boolean isActivityChangingConfigurations = false;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                if (++activityReferences == 1 && !isActivityChangingConfigurations) {
                    setOnline(true);
                }
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                isActivityChangingConfigurations = activity.isChangingConfigurations();
                if (--activityReferences == 0 && !isActivityChangingConfigurations) {
                    setOnline(false);
                }
            }

            @Override public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {}
            @Override public void onActivityResumed(@NonNull Activity activity) {}
            @Override public void onActivityPaused(@NonNull Activity activity) {}
            @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
            @Override public void onActivityDestroyed(@NonNull Activity activity) {}
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_MESSAGES,
                    getString(R.string.notification_channel_messages),
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(getString(R.string.notification_channel_messages_desc));

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Updates the presence fields of the signed-in user. Also called right after login /
     * registration (the lifecycle callback only fires when the app comes to the foreground,
     * so a user who logged in stayed "offline" until they left and re-opened the app).
     */
    public static void setOnline(boolean online) {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> map = new HashMap<>();
        map.put("online", online);
        map.put("lastSeen", System.currentTimeMillis());

        if (!online) {
            map.put("typingTo", "");
        }

        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(uid)
                .update(map);
    }
}
