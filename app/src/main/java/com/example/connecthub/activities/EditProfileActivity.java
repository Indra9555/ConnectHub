package com.example.connecthub.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.connecthub.R;
import com.example.connecthub.helpers.PostHelper;
import com.example.connecthub.utils.ValidationUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditProfileActivity extends BaseActivity {

    private TextInputLayout layoutName;
    private TextInputLayout layoutUsername;
    private TextInputEditText etName;
    private TextInputEditText etUsername;
    private TextInputEditText etBio;
    private MaterialButton btnSave;

    private FirebaseFirestore firestore;
    private String uid;
    private String originalUsername = "";
    private boolean loaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            finish();
            return;
        }

        uid = user.getUid();
        firestore = FirebaseFirestore.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        layoutName = findViewById(R.id.layoutName);
        layoutUsername = findViewById(R.id.layoutUsername);
        etName = findViewById(R.id.etName);
        etUsername = findViewById(R.id.etUsername);
        etBio = findViewById(R.id.etBio);
        btnSave = findViewById(R.id.btnSave);

        btnSave.setEnabled(false);
        btnSave.setOnClickListener(v -> updateProfile());

        loadCurrentData();
    }

    private void loadCurrentData() {
        firestore.collection("Users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {

                    if (isDestroyed() || !doc.exists()) return;

                    etName.setText(doc.getString("name"));
                    etUsername.setText(doc.getString("username"));
                    etBio.setText(doc.getString("bio"));

                    originalUsername = doc.getString("username") == null
                            ? "" : doc.getString("username");

                    loaded = true;
                    btnSave.setEnabled(true);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Couldn't load your profile", Toast.LENGTH_SHORT).show());
    }

    private String value(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private void updateProfile() {

        if (!loaded) return;

        String name = value(etName);
        String username = value(etUsername).toLowerCase(Locale.ROOT);
        String bio = value(etBio);

        layoutName.setError(null);
        layoutUsername.setError(null);

        if (name.isEmpty()) {
            layoutName.setError("Name is required");
            return;
        }

        if (!ValidationUtils.isValidUsername(username)) {
            layoutUsername.setError("3-20 characters: letters, numbers, _ or .");
            return;
        }

        btnSave.setEnabled(false);

        if (username.equals(originalUsername)) {
            save(name, username, bio);
            return;
        }

        // Username changed -> must still be unique
        firestore.collection("Users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (!snapshot.isEmpty()) {
                        btnSave.setEnabled(true);
                        layoutUsername.setError("That username is already taken");
                        return;
                    }

                    save(name, username, bio);
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(this, "Couldn't check username", Toast.LENGTH_SHORT).show();
                });
    }

    private void save(String name, String username, String bio) {

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("username", username);
        updates.put("bio", bio);

        firestore.collection("Users")
                .document(uid)
                .update(updates)
                .addOnSuccessListener(unused -> {

                    // keep old posts in sync with the new name / username
                    PostHelper.syncAuthorInfo(uid, name, username, null);

                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(this,
                            e.getMessage() == null ? "Update failed" : e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
