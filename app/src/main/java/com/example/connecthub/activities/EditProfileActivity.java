package com.example.connecthub.activities;


import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.connecthub.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;


import java.util.HashMap;
import java.util.Map;


public class EditProfileActivity extends AppCompatActivity {


    private EditText etName;
    private EditText etUsername;
    private EditText etBio;

    private Button btnSave;


    private FirebaseAuth auth;
    private FirebaseFirestore firestore;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit_profile);



        etName = findViewById(R.id.etName);

        etUsername = findViewById(R.id.etUsername);

        etBio = findViewById(R.id.etBio);

        btnSave = findViewById(R.id.btnSave);



        auth = FirebaseAuth.getInstance();

        firestore = FirebaseFirestore.getInstance();



        loadCurrentData();



        btnSave.setOnClickListener(v -> updateProfile());



    }




    private void loadCurrentData(){


        String uid = auth.getCurrentUser().getUid();



        firestore.collection("Users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {



                    if(documentSnapshot.exists()){


                        etName.setText(
                                documentSnapshot.getString("name")
                        );


                        etUsername.setText(
                                documentSnapshot.getString("username")
                        );


                        etBio.setText(
                                documentSnapshot.getString("bio")
                        );


                    }



                });



    }





    private void updateProfile(){


        String name =
                etName.getText().toString().trim();


        String username =
                etUsername.getText().toString().trim();


        String bio =
                etBio.getText().toString().trim();



        if(name.isEmpty() ||
                username.isEmpty()){


            Toast.makeText(
                    this,
                    "Name and Username required",
                    Toast.LENGTH_SHORT
            ).show();


            return;

        }



        String uid =
                auth.getCurrentUser().getUid();



        Map<String,Object> updates =
                new HashMap<>();


        updates.put("name",name);

        updates.put("username",username);

        updates.put("bio",bio);



        firestore.collection("Users")
                .document(uid)
                .update(updates)
                .addOnSuccessListener(unused -> {



                    Toast.makeText(
                            this,
                            "Profile Updated",
                            Toast.LENGTH_SHORT
                    ).show();


                    finish();



                })
                .addOnFailureListener(e -> {


                    Toast.makeText(
                            this,
                            e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();


                });



    }



}