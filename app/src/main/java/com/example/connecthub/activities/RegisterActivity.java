package com.example.connecthub.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.connecthub.R;
import com.example.connecthub.firebase.FirebaseManager;
import com.example.connecthub.models.User;
import com.example.connecthub.utils.ValidationUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etName;
    private TextInputEditText etUsername;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;

    private Button btnRegister;
    private TextView tvLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private FirebaseManager firebaseManager;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeViews();

        initializeFirebase();

        setupProgressDialog();

        setupListeners();
    }

    private void initializeViews() {

        etName = findViewById(R.id.etName);
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

    }

    private void initializeFirebase() {

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        firebaseManager = new FirebaseManager();

    }

    private void setupProgressDialog() {

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Creating Account");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

    }

    private void setupListeners() {

        btnRegister.setOnClickListener(v -> registerUser());

        tvLogin.setOnClickListener(v -> {

            startActivity(new Intent(
                    RegisterActivity.this,
                    LoginActivity.class));

            finish();

        });

    }

    private void registerUser() {

        String name = etName.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validate Empty Fields
        if (ValidationUtils.isEmpty(name) ||
                ValidationUtils.isEmpty(username) ||
                ValidationUtils.isEmpty(email) ||
                ValidationUtils.isEmpty(password) ||
                ValidationUtils.isEmpty(confirmPassword)) {

            Toast.makeText(this,
                    "Please fill all fields",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate Email
        if (!ValidationUtils.isValidEmail(email)) {

            etEmail.setError("Enter a valid email");
            etEmail.requestFocus();
            return;
        }

        // Validate Password Length
        if (!ValidationUtils.isPasswordValid(password)) {

            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        // Validate Password Match
        if (!password.equals(confirmPassword)) {

            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        progressDialog.show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        String uid = mAuth.getCurrentUser().getUid();

                        User user = new User(
                                uid,
                                name,
                                username,
                                email,
                                "Hey! I am using ConnectHub.",
                                ""
                        );

                        firebaseManager.saveUser(user,
                                new FirebaseManager.OnUserSavedListener() {

                                    @Override
                                    public void onSuccess() {

                                        progressDialog.dismiss();

                                        Toast.makeText(RegisterActivity.this,
                                                "Registration Successful",
                                                Toast.LENGTH_SHORT).show();

                                        Intent intent = new Intent(
                                                RegisterActivity.this,
                                                MainActivity.class);

                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                                Intent.FLAG_ACTIVITY_CLEAR_TASK);

                                        startActivity(intent);

                                        finish();
                                    }

                                    @Override
                                    public void onFailure(Exception e) {

                                        progressDialog.dismiss();

                                        Toast.makeText(RegisterActivity.this,
                                                e.getMessage(),
                                                Toast.LENGTH_LONG).show();

                                    }

                                });

                    } else {

                        progressDialog.dismiss();

                        Toast.makeText(RegisterActivity.this,
                                task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();

                    }

                });

    }

}