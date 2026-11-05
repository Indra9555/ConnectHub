package com.example.connecthub.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.connecthub.ConnectHubApp;
import com.example.connecthub.R;
import com.example.connecthub.utils.ValidationUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends BaseActivity {

    private TextInputLayout layoutEmail;
    private TextInputLayout layoutPassword;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;

    private Button btnLogin;
    private TextView tvRegister;
    private TextView tvForgot;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        layoutEmail = findViewById(R.id.layoutEmail);
        layoutPassword = findViewById(R.id.layoutPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgot = findViewById(R.id.tvForgot);
        progressBar = findViewById(R.id.progressBar);

        mAuth = FirebaseAuth.getInstance();

        btnLogin.setOnClickListener(v -> loginUser());

        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                loginUser();
                return true;
            }
            return false;
        });

        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        tvForgot.setOnClickListener(v -> resetPassword());
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        tvForgot.setEnabled(!loading);
    }

    private void loginUser() {

        String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
        String password = etPassword.getText() == null ? "" : etPassword.getText().toString();

        layoutEmail.setError(null);
        layoutPassword.setError(null);

        if (!ValidationUtils.isValidEmail(email)) {
            layoutEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            layoutPassword.setError("Enter your password");
            etPassword.requestFocus();
            return;
        }

        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (isFinishing() || isDestroyed()) return;

                    setLoading(false);

                    if (task.isSuccessful()) {

                        ConnectHubApp.setOnline(true);

                        Intent intent = new Intent(this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();

                    } else {

                        Exception e = task.getException();

                        String message = e != null && e.getMessage() != null
                                ? e.getMessage()
                                : "Login failed. Please try again.";

                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** The "Forgot password?" link used to do nothing at all. */
    private void resetPassword() {

        String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();

        if (!ValidationUtils.isValidEmail(email)) {
            layoutEmail.setError("Enter your email first, then tap \"Forgot password?\"");
            etEmail.requestFocus();
            return;
        }

        layoutEmail.setError(null);
        setLoading(true);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {

                    if (isFinishing() || isDestroyed()) return;

                    setLoading(false);

                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Password reset email sent to " + email,
                                Toast.LENGTH_LONG).show();
                    } else {
                        Exception e = task.getException();
                        Toast.makeText(this,
                                e != null && e.getMessage() != null
                                        ? e.getMessage()
                                        : "Could not send reset email",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
