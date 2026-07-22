package com.example.connecthub.activities;

import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.connecthub.R;
import com.example.connecthub.adapters.MessageAdapter;
import com.example.connecthub.helpers.ChatHelper;
import com.example.connecthub.models.Message;
import com.example.connecthub.network.CloudinaryUploader;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {
    private final Handler typingHandler = new Handler(Looper.getMainLooper());

    private final Runnable stopTypingRunnable = () -> {

        if (FirebaseAuth.getInstance().getCurrentUser() == null)
            return;

        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .update("typingTo", "");

    };
    private TextView tvChatName;
    private TextView tvUserStatus;

    private MaterialToolbar chatToolbar;
    private RecyclerView recyclerMessages;
    private EditText etMessage;
    private ImageButton btnSend;

    private List<Message> messageList;
    private MessageAdapter adapter;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    private String receiverId;
    private ImageView imgUser;
    private ImageButton btnImage;

    private Uri selectedImageUri;

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {

                        if (uri != null) {

                            selectedImageUri = uri;

                            uploadChatImage(uri);

                        }

                    });
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (view, insets) -> {

            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

            view.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    Math.max(systemBars.bottom, ime.bottom)
            );

            return insets;
        });
        imgUser = findViewById(R.id.imgUser);


        chatToolbar = findViewById(R.id.chatToolbar);
        tvChatName = findViewById(R.id.tvChatName);
        tvUserStatus = findViewById(R.id.tvUserStatus);
        recyclerMessages = findViewById(R.id.recyclerMessages);
        etMessage = findViewById(R.id.etMessage);
        btnImage = findViewById(R.id.btnImage);
        btnImage.setOnClickListener(v ->
                imagePicker.launch("image/*"));
        etMessage.addTextChangedListener(new android.text.TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}


            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (FirebaseAuth.getInstance().getCurrentUser() == null)
                    return;

                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                if (s.length() > 0) {

                    firestore.collection("Users")
                            .document(uid)
                            .update("typingTo", receiverId);

                    typingHandler.removeCallbacks(stopTypingRunnable);

                    typingHandler.postDelayed(stopTypingRunnable, 2000);

                } else {

                    typingHandler.removeCallbacks(stopTypingRunnable);

                    firestore.collection("Users")
                            .document(uid)
                            .update("typingTo", "");

                }

            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}

        });
        btnSend = findViewById(R.id.btnSend);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        receiverId = getIntent().getStringExtra("uid");
        resetUnreadCounter();

        setSupportActionBar(chatToolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        chatToolbar.setNavigationOnClickListener(v -> finish());

        messageList = new ArrayList<>();
        adapter = new MessageAdapter(messageList);

        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerMessages.setAdapter(adapter);

        loadMessages();
        loadUserStatus();

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {

        String text = etMessage.getText().toString().trim();

        if (text.isEmpty()) {
            return;
        }

        String senderId = auth.getCurrentUser().getUid();

        Message message = new Message(
                senderId,
                receiverId,
                text,
                "",
                "text",
                System.currentTimeMillis()
        );

        firestore.collection("Messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {

                    etMessage.setText("");
                    firestore.collection("Users")
                            .document(senderId)
                            .update("typingTo", "");
                    firestore.collection("Users")
                            .document(senderId)
                            .get()
                            .addOnSuccessListener(senderDoc -> {

                                firestore.collection("Users")
                                        .document(receiverId)
                                        .get()
                                        .addOnSuccessListener(receiverDoc -> {

                                            String chatId =
                                                    ChatHelper.getChatId(
                                                            senderId,
                                                            receiverId
                                                    );

                                            java.util.Map<String,Object> chatMap =
                                                    new java.util.HashMap<>();

                                            chatMap.put("senderId", senderId);
                                            chatMap.put("receiverId", receiverId);

                                            chatMap.put(
                                                    "senderName",
                                                    senderDoc.getString("name")
                                            );

                                            chatMap.put(
                                                    "receiverName",
                                                    receiverDoc.getString("name")
                                            );

                                            chatMap.put(
                                                    "senderImage",
                                                    senderDoc.getString("image")
                                            );

                                            chatMap.put(
                                                    "receiverImage",
                                                    receiverDoc.getString("image")
                                            );

                                            chatMap.put(
                                                    "lastMessage",
                                                    text
                                            );

                                            chatMap.put(
                                                    "lastTimestamp",
                                                    System.currentTimeMillis()
                                            );
                                            chatMap.put("senderUnread", 0);
                                            chatMap.put("receiverUnread", 1);

                                            firestore.collection("Chats")
                                                    .document(chatId)
                                                    .get()
                                                    .addOnSuccessListener(chatDoc -> {

                                                        if (chatDoc.exists()) {

                                                            Long senderUnread =
                                                                    chatDoc.getLong("senderUnread");

                                                            Long receiverUnread =
                                                                    chatDoc.getLong("receiverUnread");

                                                            if (senderId.equals(chatDoc.getString("senderId"))) {

                                                                chatMap.put(
                                                                        "senderUnread",
                                                                        0
                                                                );

                                                                chatMap.put(
                                                                        "receiverUnread",
                                                                        receiverUnread == null
                                                                                ? 1
                                                                                : receiverUnread + 1
                                                                );

                                                            } else {

                                                                chatMap.put(
                                                                        "receiverUnread",
                                                                        0
                                                                );

                                                                chatMap.put(
                                                                        "senderUnread",
                                                                        senderUnread == null
                                                                                ? 1
                                                                                : senderUnread + 1
                                                                );

                                                            }

                                                        }

                                                        firestore.collection("Chats")
                                                                .document(chatId)
                                                                .set(chatMap);

                                                    });

                                        });

                            });

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            ChatActivity.this,
                            e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();

                });

    }

    private void loadMessages() {

        String senderId = auth.getCurrentUser().getUid();

        firestore.collection("Messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {

                    if (value == null) {
                        return;
                    }

                    messageList.clear();

                    value.getDocuments().forEach(document -> {

                        Message message = document.toObject(Message.class);

                        if (message == null) {
                            return;
                        }

                        // Mark received messages as seen
                        if (message.getReceiverId().equals(senderId)
                                && !message.isSeen()) {

                            document.getReference().update("seen", true);
                        }

                        boolean chat1 =
                                message.getSenderId().equals(senderId)
                                        && message.getReceiverId().equals(receiverId);

                        boolean chat2 =
                                message.getSenderId().equals(receiverId)
                                        && message.getReceiverId().equals(senderId);

                        if (chat1 || chat2) {
                            messageList.add(message);
                        }

                    });

                    adapter.notifyDataSetChanged();

                    if (!messageList.isEmpty()) {
                        recyclerMessages.scrollToPosition(messageList.size() - 1);
                    }

                });
    }
    private void resetUnreadCounter() {

        String senderId = auth.getCurrentUser().getUid();

        String chatId = ChatHelper.getChatId(
                senderId,
                receiverId
        );

        firestore.collection("Chats")
                .document(chatId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) return;

                    if (senderId.equals(document.getString("senderId"))) {

                        document.getReference()
                                .update("senderUnread", 0);

                    } else {

                        document.getReference()
                                .update("receiverUnread", 0);

                    }

                });

    }
    private void loadUserStatus() {

        firestore.collection("Users")
                .document(receiverId)
                .addSnapshotListener((document, error) -> {

                    if (document == null || !document.exists()) {
                        return;
                    }

                    String name = document.getString("name");
                    String image = document.getString("image");

                    Glide.with(this)
                            .load(image)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .into(imgUser);

                    tvChatName.setText(name);
                    String typingTo = document.getString("typingTo");

                    String myUid = FirebaseAuth.getInstance()
                            .getCurrentUser()
                            .getUid();

                    if (typingTo != null && typingTo.equals(myUid)) {

                        tvUserStatus.setText("Typing...");

                        return;
                    }

                    Boolean online = document.getBoolean("online");

                    if (online != null && online) {

                        tvUserStatus.setText("🟢 Online");

                    } else {

                        Long lastSeen = document.getLong("lastSeen");

                        if (lastSeen != null) {

                            tvUserStatus.setText(
                                    "Last seen " + getLastSeen(lastSeen)
                            );

                        } else {

                            tvUserStatus.setText("Offline");

                        }

                    }

                });

    }
    private String getLastSeen(long time) {

        long diff = System.currentTimeMillis() - time;

        long seconds = diff / 1000;

        if (seconds < 60) {
            return "just now";
        }

        long minutes = seconds / 60;

        if (minutes < 60) {
            return minutes + " min ago";
        }

        long hours = minutes / 60;

        if (hours < 24) {
            return hours + " hr ago";
        }

        long days = hours / 24;

        return days + " day ago";

    }
    @Override
    protected void onPause() {
        super.onPause();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }

        firestore.collection("Users")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .update("typingTo", "");
    }
    private void uploadChatImage(Uri imageUri) {

        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show();

        CloudinaryUploader.uploadImage(
                this,
                imageUri,
                new CloudinaryUploader.UploadCallback() {

                    @Override
                    public void onSuccess(String imageUrl) {

                        runOnUiThread(() -> {

                            sendImageMessage(imageUrl);

                        });

                    }

                    @Override
                    public void onFailure(Exception e) {

                        runOnUiThread(() ->

                                Toast.makeText(
                                        ChatActivity.this,
                                        e.getMessage(),
                                        Toast.LENGTH_SHORT
                                ).show());

                    }
                });

    }
    private void sendImageMessage(String imageUrl) {

        String senderId = auth.getCurrentUser().getUid();

        Message message = new Message(
                senderId,
                receiverId,
                "",
                imageUrl,
                "image",
                System.currentTimeMillis()
        );

        firestore.collection("Messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {

                    Toast.makeText(
                            this,
                            "Image sent",
                            Toast.LENGTH_SHORT
                    ).show();

                });

    }
}