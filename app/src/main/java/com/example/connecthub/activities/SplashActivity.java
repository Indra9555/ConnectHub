package com.example.connecthub.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.connecthub.R;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIME = 1200;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable proceed = this::checkUserSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        handler.postDelayed(proceed, SPLASH_TIME);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(proceed);
        super.onDestroy();
    }

    private void checkUserSession() {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        startActivity(new Intent(this, MainActivity.class));

        // Opened by tapping a push notification -> go straight to that chat.
        // (FCM puts the "data" payload into the launcher intent extras.)
        Bundle extras = getIntent() == null ? null : getIntent().getExtras();

        if (extras != null) {

            String type = extras.getString("type");
            String groupId = extras.getString("groupId");
            String senderId = extras.getString("senderId");

            // like / comment / follow pushes just open the app (type != chat / group)
            if ("group".equals(type) && groupId != null && !groupId.isEmpty()) {

                Intent chat = new Intent(this, ChatActivity.class);
                chat.putExtra("isGroup", true);
                chat.putExtra("groupId", groupId);
                startActivity(chat);

            } else if ("chat".equals(type) && senderId != null && !senderId.isEmpty()) {

                Intent chat = new Intent(this, ChatActivity.class);
                chat.putExtra("uid", senderId);
                startActivity(chat);
            }
        }

        finish();
    }
}
