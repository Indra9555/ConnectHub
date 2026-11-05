package com.example.connecthub.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connecthub.R;
import com.example.connecthub.adapters.ChatUserAdapter;
import com.example.connecthub.models.ChatUser;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatListActivity extends BaseActivity {

    private RecyclerView recyclerChats;
    private ChatUserAdapter adapter;
    private TextView tvEmpty;

    private final List<ChatUser> chatUserList = new ArrayList<>();

    /** Chats where I started the conversation / where the other person did. */
    private final Map<String, ChatUser> asSender = new HashMap<>();
    private final Map<String, ChatUser> asReceiver = new HashMap<>();

    /** uid -> {name, image}: fresh profile data (chat documents only hold a stale copy). */
    private final Map<String, String[]> profileCache = new HashMap<>();

    private FirebaseFirestore firestore;
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestore = FirebaseFirestore.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.chatListToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerChats = findViewById(R.id.recyclerChats);
        tvEmpty = findViewById(R.id.tvEmpty);

        adapter = new ChatUserAdapter(chatUserList);
        recyclerChats.setLayoutManager(new LinearLayoutManager(this));
        recyclerChats.setAdapter(adapter);

        loadChats();
    }

    /**
     * The old version downloaded EVERY chat of EVERY user and filtered on the device
     * (a privacy and performance problem) and added one more listener on each onResume.
     * Now only my own chats are queried, with listeners that live as long as the screen.
     */
    private void loadChats() {

        firestore.collection("Chats")
                .whereEqualTo("senderId", currentUid)
                .addSnapshotListener(this, (value, error) -> {
                    if (value == null) return;
                    collect(value, asSender, true);
                });

        firestore.collection("Chats")
                .whereEqualTo("receiverId", currentUid)
                .addSnapshotListener(this, (value, error) -> {
                    if (value == null) return;
                    collect(value, asReceiver, false);
                });
    }

    @SuppressWarnings("unchecked")
    private void collect(QuerySnapshot snapshot, Map<String, ChatUser> target, boolean iAmSender) {

        target.clear();

        for (DocumentSnapshot document : snapshot.getDocuments()) {

            ChatUser chatUser = new ChatUser();

            String otherUid = iAmSender
                    ? document.getString("receiverId")
                    : document.getString("senderId");

            if (otherUid == null) continue;

            chatUser.setUid(otherUid);
            chatUser.setName(document.getString(iAmSender ? "receiverName" : "senderName"));
            chatUser.setImage(document.getString(iAmSender ? "receiverImage" : "senderImage"));
            chatUser.setLastMessage(document.getString("lastMessage"));

            Long time = document.getLong("lastTimestamp");
            chatUser.setLastTimestamp(time == null ? 0 : time);

            int unread = 0;

            Object unreadObject = document.get("unread");

            if (unreadObject instanceof Map) {
                Object count = ((Map<String, Object>) unreadObject).get(currentUid);
                if (count instanceof Number) {
                    unread = ((Number) count).intValue();
                }
            }

            chatUser.setUnreadCount(unread);

            // Use fresh profile data when we already have it
            String[] profile = profileCache.get(otherUid);

            if (profile != null) {
                chatUser.setName(profile[0]);
                chatUser.setImage(profile[1]);
            }

            target.put(document.getId(), chatUser);
        }

        rebuild();
        refreshProfiles();
    }

    private void rebuild() {

        // A chat id is unique and appears in exactly one of the two maps, but be defensive
        Map<String, ChatUser> merged = new HashMap<>(asSender);
        merged.putAll(asReceiver);

        chatUserList.clear();
        chatUserList.addAll(merged.values());

        Collections.sort(chatUserList, (a, b) ->
                Long.compare(b.getLastTimestamp(), a.getLastTimestamp()));

        adapter.notifyDataSetChanged();

        tvEmpty.setVisibility(chatUserList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void refreshProfiles() {

        List<String> missing = new ArrayList<>();

        for (ChatUser user : chatUserList) {
            if (user.getUid() != null && !profileCache.containsKey(user.getUid())
                    && !missing.contains(user.getUid())) {
                missing.add(user.getUid());
            }
        }

        for (String uid : missing) {

            // reserve the slot so we don't fetch twice while the request is in flight
            profileCache.put(uid, new String[]{null, null});

            firestore.collection("Users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener(doc -> {

                        if (isDestroyed()) return;

                        if (!doc.exists()) {
                            profileCache.remove(uid);
                            return;
                        }

                        String name = doc.getString("name");
                        String image = doc.getString("image");

                        profileCache.put(uid, new String[]{name, image});

                        for (ChatUser user : chatUserList) {
                            if (uid.equals(user.getUid())) {
                                if (name != null) user.setName(name);
                                user.setImage(image);
                            }
                        }

                        adapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> profileCache.remove(uid));
        }
    }
}
