package com.example.connecthub.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.connecthub.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;


public class LoginActivity extends AppCompatActivity {


    private TextInputEditText etEmail;
    private TextInputEditText etPassword;

    private Button btnLogin;
    private TextView tvRegister;

    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);


        mAuth = FirebaseAuth.getInstance();


        btnLogin.setOnClickListener(v -> loginUser());


        tvRegister.setOnClickListener(v -> {

            startActivity(new Intent(
                    LoginActivity.this,
                    RegisterActivity.class));

        });


    }
    private void loginUser() {

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();


        // Empty field validation
        if (email.isEmpty()) {

            etEmail.setError("Enter email");
            etEmail.requestFocus();
            return;

        }


        if (password.isEmpty()) {

            etPassword.setError("Enter password");
            etPassword.requestFocus();
            return;

        }


        // Firebase Login

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {


                    if (task.isSuccessful()) {


                        Toast.makeText(LoginActivity.this,
                                "Login Successful",
                                Toast.LENGTH_SHORT).show();


                        Intent intent = new Intent(
                                LoginActivity.this,
                                MainActivity.class);


                        // Remove login screen from back stack
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TASK);


                        startActivity(intent);

                        finish();



                    } else {


                        String error = task.getException()
                                .getMessage();


                        Toast.makeText(LoginActivity.this,
                                error,
                                Toast.LENGTH_LONG).show();


                    }


                });


    }


}