package com.example.connecthub.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.example.connecthub.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class SplashActivity extends AppCompatActivity {


    private static final int SPLASH_TIME = 3000; // 3 seconds


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);


        new Handler().postDelayed(() -> {

            checkUserSession();

        }, SPLASH_TIME);


    }


    private void checkUserSession() {


        FirebaseUser currentUser =
                FirebaseAuth.getInstance().getCurrentUser();


        if (currentUser != null) {


            // User already logged in

            Intent intent = new Intent(
                    SplashActivity.this,
                    MainActivity.class
            );

            startActivity(intent);


        } else {


            // User not logged in

            Intent intent = new Intent(
                    SplashActivity.this,
                    LoginActivity.class
            );

            startActivity(intent);


        }


        finish();


    }


}