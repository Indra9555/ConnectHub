package com.example.connecthub.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connecthub.R;
import com.example.connecthub.adapters.UserAdapter;
import com.example.connecthub.models.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private EditText etSearch;
    private RecyclerView recyclerUsers;

    private UserAdapter adapter;
    private List<User> userList;

    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        etSearch = findViewById(R.id.etSearch);
        recyclerUsers = findViewById(R.id.recyclerUsers);

        firestore = FirebaseFirestore.getInstance();

        userList = new ArrayList<>();

        adapter = new UserAdapter(this, userList);

        recyclerUsers.setLayoutManager(new LinearLayoutManager(this));
        recyclerUsers.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                searchUsers(s.toString().trim());

            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void searchUsers(String text) {

        if (text.isEmpty()) {

            userList.clear();
            adapter.notifyDataSetChanged();
            return;
        }

        firestore.collection("Users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    userList.clear();

                    for (User user : queryDocumentSnapshots.toObjects(User.class)) {

                        if (user.getUsername() != null &&
                                user.getUsername().toLowerCase().contains(text.toLowerCase())) {

                            userList.add(user);

                        } else if (user.getName() != null &&
                                user.getName().toLowerCase().contains(text.toLowerCase())) {

                            userList.add(user);
                        }
                    }

                    adapter.notifyDataSetChanged();

                });
    }
}