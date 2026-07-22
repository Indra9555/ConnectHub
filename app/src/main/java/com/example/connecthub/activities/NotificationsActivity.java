package com.example.connecthub.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connecthub.R;
import com.example.connecthub.adapters.NotificationAdapter;
import com.example.connecthub.models.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView recyclerNotifications;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        recyclerNotifications = findViewById(R.id.recyclerNotifications);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList);

        recyclerNotifications.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerNotifications.setAdapter(adapter);

        loadNotifications();
        markNotificationsAsSeen();
    }

    private void loadNotifications() {

        String uid = auth.getCurrentUser().getUid();

        firestore.collection("Notifications")
                .whereEqualTo("receiverId", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    notificationList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {

                        Notification notification =
                                document.toObject(Notification.class);

                        if (notification != null) {
                            notificationList.add(notification);
                        }
                    }

                    adapter.notifyDataSetChanged();

                });
    }
    private void markNotificationsAsSeen() {

        String uid = auth.getCurrentUser().getUid();

        firestore.collection("Notifications")
                .whereEqualTo("receiverId", uid)
                .whereEqualTo("seen", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    for (DocumentSnapshot document : queryDocumentSnapshots) {

                        document.getReference()
                                .update("seen", true);

                    }

                });

    }
}