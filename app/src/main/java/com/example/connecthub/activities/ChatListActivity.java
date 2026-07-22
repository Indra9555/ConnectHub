package com.example.connecthub.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connecthub.R;
import com.example.connecthub.adapters.ChatUserAdapter;
import com.example.connecthub.models.ChatUser;
import com.example.connecthub.models.Message;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

import java.util.List;

public class ChatListActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private RecyclerView recyclerChats;

    private ChatUserAdapter adapter;
    private List<ChatUser> chatUserList;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        toolbar = findViewById(R.id.chatListToolbar);
        recyclerChats = findViewById(R.id.recyclerChats);

        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        chatUserList = new ArrayList<>();
        adapter = new ChatUserAdapter(chatUserList);

        recyclerChats.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerChats.setAdapter(adapter);

        loadChats();
    }

    private void loadChats() {

        String currentUid = auth.getCurrentUser().getUid();

        firestore.collection("Chats")
                .orderBy("lastTimestamp",
                        com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {

                    if (value == null) return;

                    chatUserList.clear();

                    for (DocumentSnapshot document : value.getDocuments()) {

                        String senderId =
                                document.getString("senderId");

                        String receiverId =
                                document.getString("receiverId");

                        if (!currentUid.equals(senderId)
                                && !currentUid.equals(receiverId)) {

                            continue;

                        }

                        ChatUser chatUser = new ChatUser();

                        if (currentUid.equals(senderId)) {

                            chatUser.setUid(receiverId);

                            chatUser.setName(
                                    document.getString("receiverName")
                            );

                            chatUser.setImage(
                                    document.getString("receiverImage")
                            );

                        } else {

                            chatUser.setUid(senderId);

                            chatUser.setName(
                                    document.getString("senderName")
                            );

                            chatUser.setImage(
                                    document.getString("senderImage")
                            );

                        }

                        chatUser.setLastMessage(
                                document.getString("lastMessage")
                        );

                        Long time =
                                document.getLong("lastTimestamp");

                        chatUser.setLastTimestamp(
                                time == null ? 0 : time
                        );

                        if (currentUid.equals(senderId)) {

                            Long unread = document.getLong("senderUnread");

                            chatUser.setUnreadCount(
                                    unread == null ? 0 : unread.intValue()
                            );

                        } else {

                            Long unread = document.getLong("receiverUnread");

                            chatUser.setUnreadCount(
                                    unread == null ? 0 : unread.intValue()
                            );

                        }

                        chatUserList.add(chatUser);

                    }
                    adapter.notifyDataSetChanged();



                });

    }
    @Override
    protected void onResume() {
        super.onResume();

        loadChats();
    }
}