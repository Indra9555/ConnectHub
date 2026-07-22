package com.example.connecthub;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ConnectHubApp extends Application {

    private int activityReferences = 0;
    private boolean isActivityChangingConfigurations = false;

    @Override
    public void onCreate() {
        super.onCreate();

        registerActivityLifecycleCallbacks(
                new ActivityLifecycleCallbacks() {

                    @Override
                    public void onActivityStarted(@NonNull Activity activity) {

                        if (++activityReferences == 1
                                && !isActivityChangingConfigurations) {

                            updateOnlineStatus(true);

                        }

                    }

                    @Override
                    public void onActivityStopped(@NonNull Activity activity) {

                        isActivityChangingConfigurations =
                                activity.isChangingConfigurations();

                        if (--activityReferences == 0
                                && !isActivityChangingConfigurations) {

                            updateOnlineStatus(false);

                        }

                    }

                    @Override public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {}
                    @Override public void onActivityResumed(@NonNull Activity activity) {}
                    @Override public void onActivityPaused(@NonNull Activity activity) {}
                    @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
                    @Override public void onActivityDestroyed(@NonNull Activity activity) {}
                }
        );

    }

    private void updateOnlineStatus(boolean online) {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }

        String uid =
                FirebaseAuth.getInstance()
                        .getCurrentUser()
                        .getUid();

        Map<String, Object> map = new HashMap<>();

        map.put("online", online);

        map.put("lastSeen", System.currentTimeMillis());

        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(uid)
                .update(map);

    }

}