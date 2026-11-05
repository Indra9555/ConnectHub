package com.example.connecthub.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.connecthub.ConnectHubApp;
import com.example.connecthub.R;
import com.example.connecthub.firebase.FirebaseManager;
import com.example.connecthub.models.User;
import com.example.connecthub.utils.ValidationUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class RegisterActivity extends BaseActivity {

    private TextInputLayout layoutName;
    private TextInputLayout layoutUsername;
    private TextInputLayout layoutEmail;
    private TextInputLayout layoutPassword;
    private TextInputLayout layoutConfirm;

    private TextInputEditText etName;
    private TextInputEditText etUsername;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;

    private Button btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        layoutName = findViewById(R.id.layoutName);
        layoutUsername = findViewById(R.id.layoutUsername);
        layoutEmail = findViewById(R.id.layoutEmail);
        layoutPassword = findViewById(R.id.layoutPassword);
        layoutConfirm = findViewById(R.id.layoutConfirmPassword);

        etName = findViewById(R.id.etName);
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        firebaseManager = new FirebaseManager();

        btnRegister.setOnClickListener(v -> registerUser());

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
    }

    private String text(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString();
    }

    private void registerUser() {

        String name = text(etName).trim();
        String username = text(etUsername).trim().toLowerCase(Locale.ROOT);
        String email = text(etEmail).trim();
        String password = text(etPassword);
        String confirmPassword = text(etConfirmPassword);

        layoutName.setError(null);
        layoutUsername.setError(null);
        layoutEmail.setError(null);
        layoutPassword.setError(null);
        layoutConfirm.setError(null);

        if (name.isEmpty()) {
            layoutName.setError("Enter your name");
            etName.requestFocus();
            return;
        }

        if (!ValidationUtils.isValidUsername(username)) {
            layoutUsername.setError("3-20 characters: letters, numbers, _ or .");
            etUsername.requestFocus();
            return;
        }

        if (!ValidationUtils.isValidEmail(email)) {
            layoutEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        if (!ValidationUtils.isPasswordValid(password)) {
            layoutPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            layoutConfirm.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        setLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (isFinishing() || isDestroyed()) return;

                    if (!task.isSuccessful()) {
                        setLoading(false);
                        Exception e = task.getException();
                        Toast.makeText(this,
                                e != null && e.getMessage() != null
                                        ? e.getMessage()
                                        : "Registration failed",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser firebaseUser = mAuth.getCurrentUser();

                    if (firebaseUser == null) {
                        setLoading(false);
                        return;
                    }

                    ensureUsernameIsFree(firebaseUser, name, username, email);
                });
    }

    /**
     * Usernames must be unique. The check runs after sign-up (Firestore needs an
     * authenticated user to read); if the name is taken the new auth account is removed again.
     */
    private void ensureUsernameIsFree(
            FirebaseUser firebaseUser,
            String name,
            String username,
            String email
    ) {

        firestore.collection("Users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (!snapshot.isEmpty()) {
                        rollback(firebaseUser, "That username is already taken");
                        return;
                    }

                    User user = new User(
                            firebaseUser.getUid(),
                            name,
                            username,
                            email,
                            "Hey! I am using ConnectHub.",
                            ""
                    );

                    firebaseManager.saveUser(user, new FirebaseManager.OnUserSavedListener() {

                        @Override
                        public void onSuccess() {

                            if (isFinishing() || isDestroyed()) return;

                            setLoading(false);

                            ConnectHubApp.setOnline(true);

                            Toast.makeText(RegisterActivity.this,
                                    "Welcome to ConnectHub!",
                                    Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            rollback(firebaseUser,
                                    e.getMessage() != null ? e.getMessage() : "Could not save profile");
                        }
                    });
                })
                .addOnFailureListener(e ->
                        rollback(firebaseUser,
                                e.getMessage() != null ? e.getMessage() : "Could not verify username"));
    }

    /** Removes the half-created account so the email isn't "used up" without a profile. */
    private void rollback(FirebaseUser firebaseUser, String message) {

        firebaseUser.delete().addOnCompleteListener(done -> {
            mAuth.signOut();

            if (isFinishing() || isDestroyed()) return;

            setLoading(false);

            if (message.contains("username")) {
                layoutUsername.setError(message);
                etUsername.requestFocus();
            } else {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
