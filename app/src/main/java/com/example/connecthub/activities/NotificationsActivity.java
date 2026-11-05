package com.example.connecthub.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connecthub.R;
import com.example.connecthub.adapters.NotificationAdapter;
import com.example.connecthub.models.Notification;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationsActivity extends BaseActivity {

    private NotificationAdapter adapter;
    private final List<Notification> notificationList = new ArrayList<>();
    private TextView tvEmpty;

    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvEmpty = findViewById(R.id.tvEmpty);
        RecyclerView recycler = findViewById(R.id.recyclerNotifications);

        firestore = FirebaseFirestore.getInstance();

        adapter = new NotificationAdapter(notificationList);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        loadNotifications();
    }

    private void loadNotifications() {

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        firestore.collection("Notifications")
                .whereEqualTo("receiverId", uid)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (isDestroyed()) return;

                    notificationList.clear();

                    List<DocumentSnapshot> unseen = new ArrayList<>();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {

                        Notification notification = document.toObject(Notification.class);

                        if (notification == null) continue;

                        notificationList.add(notification);

                        if (!notification.isSeen()) {
                            unseen.add(document);
                        }
                    }

                    // Newest first (sorted here so no composite index is needed)
                    Collections.sort(notificationList, (a, b) ->
                            Long.compare(b.getTimestamp(), a.getTimestamp()));

                    adapter.notifyDataSetChanged();

                    tvEmpty.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);

                    markAsSeen(unseen);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Couldn't load notifications", Toast.LENGTH_SHORT).show());
    }

    /** Marks everything as read with batched writes (instead of one write per document). */
    private void markAsSeen(List<DocumentSnapshot> unseen) {

        int index = 0;

        while (index < unseen.size()) {

            WriteBatch batch = firestore.batch();

            int end = Math.min(index + 400, unseen.size());

            for (int i = index; i < end; i++) {
                batch.update(unseen.get(i).getReference(), "seen", true);
            }

            batch.commit();

            index = end;
        }
    }
}
