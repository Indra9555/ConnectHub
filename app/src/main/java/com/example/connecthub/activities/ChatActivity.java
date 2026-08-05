package com.example.connecthub.activities;

import android.content.Intent;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;


import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import java.io.File;

import com.example.connecthub.adapters.MessageDiffCallback;
import com.example.connecthub.chat.VoiceUploadManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.connecthub.R;
import com.example.connecthub.adapters.MessageAdapter;
import com.example.connecthub.chat.ChatRepository;
import com.example.connecthub.chat.PresenceManager;
import com.example.connecthub.chat.TypingManager;
import com.example.connecthub.chat.WaveformGenerator;
import com.example.connecthub.helpers.ChatHelper;
import com.example.connecthub.helpers.SwipeToReplyCallback;
import com.example.connecthub.models.Group;
import com.example.connecthub.models.Message;
import com.example.connecthub.chat.ChatUploadManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import com.google.firebase.firestore.FieldValue;

public class ChatActivity extends AppCompatActivity {
    private final Handler typingHandler = new Handler(Looper.getMainLooper());
    private Handler recordingHandler = new Handler(Looper.getMainLooper());

    private Runnable recordingRunnable;

    private float startX;
    private boolean cancelRecording = false;
    private static final float CANCEL_DISTANCE = 250f;

    private final Runnable stopTypingRunnable = () -> {

        if (FirebaseAuth.getInstance().getCurrentUser() == null)
            return;

        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .update("typingTo", "");

    };
    private ChatRepository chatRepository;
    private ChatUploadManager chatUploadManager;
    private TypingManager typingManager;
    private PresenceManager presenceManager;
    private TextView tvChatName;
    private TextView tvUserStatus;
    private boolean groupMessagesLoaded = false;

    private MaterialToolbar chatToolbar;
    private RecyclerView recyclerMessages;
    private ListenerRegistration groupMessageListener;
    private EditText etMessage;
    private ImageButton btnSend;
    private boolean firstLoad = true;

    private List<Message> messageList;
    private MessageAdapter adapter;private LinearLayout layoutInput;
    private LinearLayout layoutRecording;

    private TextView tvRecordingTime;
    private TextView tvSlideCancel;
    private boolean isCurrentMember = true;


    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    private String receiverId;
    private ImageView imgUser;
    private ImageButton btnImage;
    private String messageId;

    private Uri selectedImageUri;
    private Message replyingMessage = null;
    private LinearLayout layoutReply;
    private TextView tvReplySender;
    private TextView tvReplyMessage;

    private ImageButton btnMic;
    private MediaRecorder mediaRecorder;
    private File voiceFile;
    private boolean isRecordingVoice = false;
    private long voiceStartTime = 0L;
    private boolean isGroup = false;

    private String groupId;
    private String groupName;
    private final ActivityResultLauncher<String> audioPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startVoiceRecording();
                } else {
                    Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show();
                }
            });

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
        chatRepository = new ChatRepository();
        chatUploadManager = new ChatUploadManager();
        typingManager = new TypingManager();
        presenceManager = new PresenceManager();



        EdgeToEdge.enable(this);


        setContentView(R.layout.activity_chat);
        isGroup = getIntent().getBooleanExtra("isGroup", false);

        if (isGroup) {

            groupId = getIntent().getStringExtra("groupId");
            groupName = getIntent().getStringExtra("groupName");

        } else {

            receiverId = getIntent().getStringExtra("receiverId");

        }
        layoutInput = findViewById(R.id.layoutInput);
        layoutRecording = findViewById(R.id.layoutRecording);

        tvRecordingTime = findViewById(R.id.tvRecordingTime);
        tvSlideCancel = findViewById(R.id.tvSlideCancel);
        layoutReply = findViewById(R.id.layoutReply);
        tvReplySender = findViewById(R.id.tvReplySender);
        tvReplyMessage = findViewById(R.id.tvReplyMessage);
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
        chatToolbar.setOnClickListener(v -> {

            if (isGroup) {

                Intent intent =
                        new Intent(
                                ChatActivity.this,
                                GroupInfoActivity.class
                        );

                intent.putExtra("groupId", groupId);

                startActivity(intent);

            }

        });
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
                            .update("typingTo", isGroup ? groupId : receiverId);

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
        if (!isGroup) {
            resetUnreadCounter();
        }

        setSupportActionBar(chatToolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        chatToolbar.setNavigationOnClickListener(v -> finish());

        messageList = new ArrayList<>();
        adapter = new MessageAdapter(messageList);
        adapter.setOnMessageDoubleTapListener(message -> {

            addReaction(message, "❤️");

        });
        adapter.setOnReplyClickListener(messageId -> {

            for (int i = 0; i < messageList.size(); i++) {

                if (messageList.get(i).getMessageId().equals(messageId)) {

                    recyclerMessages.smoothScrollToPosition(i);

                    break;

                }

            }

        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);

        recyclerMessages.setLayoutManager(layoutManager);
        recyclerMessages.setAdapter(adapter);

        adapter.registerAdapterDataObserver(
                new RecyclerView.AdapterDataObserver() {

                    @Override
                    public void onItemRangeInserted(int positionStart, int itemCount) {

                        recyclerMessages.smoothScrollToPosition(
                                adapter.getItemCount() - 1
                        );
                    }

                });

        ItemTouchHelper itemTouchHelper =
                new ItemTouchHelper(
                        new SwipeToReplyCallback(position -> {

                            Message message = messageList.get(position);

                            replyingMessage = message;

                            layoutReply.setVisibility(View.VISIBLE);

                            tvReplySender.setText(
                                    message.getSenderId().equals(auth.getCurrentUser().getUid())
                                            ? "You"
                                            : tvChatName.getText()
                            );

                            if ("image".equals(message.getType())) {
                                tvReplyMessage.setText("📷 Photo");
                            } else if ("voice".equals(message.getType())) {
                                tvReplyMessage.setText("🎤 Voice message");
                            } else {
                                tvReplyMessage.setText(message.getMessage());
                            }

                            adapter.notifyItemChanged(position);

                        })
                );

        itemTouchHelper.attachToRecyclerView(recyclerMessages);

        adapter.setOnMessageLongClickListener((anchor, message) -> {


            androidx.appcompat.widget.PopupMenu popupMenu =
                    new androidx.appcompat.widget.PopupMenu(ChatActivity.this, anchor);

            popupMenu.getMenuInflater().inflate(R.menu.message_menu, popupMenu.getMenu());

            // Hide "Delete for everyone" if this isn't your message
            if (!message.getSenderId().equals(auth.getCurrentUser().getUid())) {
                popupMenu.getMenu().findItem(R.id.menu_delete_everyone).setVisible(false);
            }

            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.menu_react) {

                    showReactionPicker(anchor, message);

                    return true;
                }

                if (item.getItemId() == R.id.menu_reply) {

                    replyingMessage = message;

                    layoutReply.setVisibility(View.VISIBLE);

                    tvReplySender.setText(
                            message.getSenderId().equals(auth.getCurrentUser().getUid())
                                    ? "You"
                                    : tvChatName.getText()
                    );

                    if ("image".equals(message.getType())) {
                        tvReplyMessage.setText("📷 Photo");
                    } else if ("voice".equals(message.getType())) {
                        tvReplyMessage.setText("🎤 Voice message");
                    } else {
                        tvReplyMessage.setText(message.getMessage());
                    }

                    return true;
                }

                if (item.getItemId() == R.id.menu_copy) {

                    android.content.ClipboardManager clipboard =
                            (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

                    clipboard.setPrimaryClip(
                            android.content.ClipData.newPlainText(
                                    "message",
                                    message.getMessage()
                            )
                    );

                    Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();

                    return true;
                }

                if (item.getItemId() == R.id.menu_delete_me) {

                    Toast.makeText(this,
                            "Delete for Me will be added later",
                            Toast.LENGTH_SHORT).show();


                    return true;
                }

                if (item.getItemId() == R.id.menu_delete_everyone) {

                    firestore.collection("Messages")
                            .document(message.getMessageId())
                            .update(
                                    "deleted", true,
                                    "message", "",
                                    "imageUrl", "",
                                    "type", "text"
                            );

                    return true;

                }

                return false;
            });

            popupMenu.show();

        });

        if (isGroup) {

            verifyGroupMembership();

        } else {

            loadPrivateMessages();

        }
        loadUserStatus();
        btnMic = findViewById(R.id.btnMic);

        btnMic.setOnTouchListener((v, event) -> {

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:

                    startX = event.getRawX();
                    cancelRecording = false;

                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED) {

                        startVoiceRecording();

                    } else {

                        audioPermissionLauncher.launch(
                                Manifest.permission.RECORD_AUDIO
                        );

                    }

                    return true;

                case MotionEvent.ACTION_MOVE:

                    float diff = startX - event.getRawX();

                    if (diff > 0) {

                        tvSlideCancel.setTranslationX(-diff / 3f);

                        if (diff > CANCEL_DISTANCE) {

                            cancelRecording = true;

                            tvSlideCancel.setText("Release to cancel");

                        } else {

                            cancelRecording = false;

                            tvSlideCancel.setText("◀ Slide to cancel");

                        }

                    }

                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:

                    tvSlideCancel.animate()
                            .translationX(0)
                            .setDuration(200)
                            .start();

                    if (cancelRecording) {

                        cancelVoiceRecording();

                    } else {

                        stopVoiceRecording();

                    }

                    return true;
            }

            return false;
        });
        btnSend.setOnClickListener(v -> sendMessage());

    }

    private void sendMessage() {

        if (isGroup) {

            sendGroupMessage();

        } else {

            sendPrivateMessage();

        }

    }

    private void sendPrivateMessage() {

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
        if (replyingMessage != null) {

            message.setReplyMessageId(replyingMessage.getMessageId());

            if ("image".equals(replyingMessage.getType())) {

                message.setReplyMessage("Photo");
                message.setReplyImageUrl(replyingMessage.getImageUrl());

            } else {

                message.setReplyMessage(replyingMessage.getMessage());

            }
            message.setReplySender(
                    replyingMessage.getSenderId().equals(senderId)
                            ? "You"
                            : tvChatName.getText().toString()
            );

            message.setReplyType(replyingMessage.getType());

            replyingMessage = null;
            layoutReply.setVisibility(View.GONE);
        }

        firestore.collection("Messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    documentReference.update(
                            "messageId",
                            documentReference.getId()
                    );
                    etMessage.setText("");
                    replyingMessage = null;
                    layoutReply.setVisibility(View.GONE);
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
    private void sendGroupMessage() {
        if (!isCurrentMember) {

            Toast.makeText(
                    this,
                    "You left this group",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String text = etMessage.getText().toString().trim();

        if (text.isEmpty()) {
            return;
        }

        String senderId = auth.getCurrentUser().getUid();

        Message message = new Message(
                senderId,
                "",
                text,
                "",
                "text",
                System.currentTimeMillis()
        );

        message.setGroupId(groupId);

        firestore.collection("GroupMessages")
                .document(groupId)
                .collection("Messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {

                    documentReference.update(
                            "messageId",
                            documentReference.getId()
                    );

                    etMessage.setText("");

                    replyingMessage = null;
                    layoutReply.setVisibility(View.GONE);

                    firestore.collection("Groups")
                            .document(groupId)
                            .update(
                                    "lastMessage", text,
                                    "lastTimestamp", System.currentTimeMillis()
                            );

                })
                .addOnFailureListener(e ->

                        Toast.makeText(
                                ChatActivity.this,
                                e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show()

                );

    }

    private void loadMessages() {

        if (isGroup) {

            firestore.collection("GroupMessages")
                    .document(groupId)
                    .collection("Messages")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .addSnapshotListener((value, error) -> {

                        if (value == null) return;

                        List<Message> newList = new ArrayList<>();

                        for (DocumentSnapshot doc : value.getDocuments()) {

                            Message message = doc.toObject(Message.class);

                            if (message == null) continue;

                            message.setMessageId(doc.getId());

                            newList.add(message);
                        }

                        DiffUtil.DiffResult diff =
                                DiffUtil.calculateDiff(
                                        new MessageDiffCallback(messageList, newList)
                                );

                        messageList.clear();
                        messageList.addAll(newList);

                        adapter.rebuildMessageMap();

                        diff.dispatchUpdatesTo(adapter);

                        recyclerMessages.scrollToPosition(messageList.size() - 1);

                    });

            return;
        } else {

            loadPrivateMessages();

        }

    }

    private void loadPrivateMessages() {

        String senderId = auth.getCurrentUser().getUid();

        firestore.collection("Messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {

                    if (value == null) return;

                    List<Message> newList = new ArrayList<>();

                    for (DocumentSnapshot document : value.getDocuments()) {

                        Message message = document.toObject(Message.class);

                        if (message == null) continue;

                        message.setMessageId(document.getId());

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
                            newList.add(message);
                        }
                    }

                    DiffUtil.DiffResult diffResult =
                            DiffUtil.calculateDiff(
                                    new MessageDiffCallback(messageList, newList)
                            );

                    messageList.clear();
                    messageList.addAll(newList);

                    adapter.rebuildMessageMap();

                    diffResult.dispatchUpdatesTo(adapter);

                });

    }
    private void loadGroupMessages() {

        String myUid = auth.getCurrentUser().getUid();

        firestore.collection("Groups")
                .document(groupId)
                .get()
                .addOnSuccessListener(groupDoc -> {

                    if (!groupDoc.exists()) return;

                    Group group = groupDoc.toObject(Group.class);

                    if (group == null) return;

                    Long joinTime = null;

                    if (group.getMemberJoinedAt() != null) {
                        joinTime = group.getMemberJoinedAt().get(myUid);
                    }

                    if (joinTime == null) {
                        joinTime = 0L;
                    }

                    // Remove previous listener
                    if (groupMessageListener != null) {
                        groupMessageListener.remove();
                    }

                    groupMessageListener =
                            firestore.collection("GroupMessages")
                                    .document(groupId)
                                    .collection("Messages")
                                    .whereGreaterThanOrEqualTo("timestamp", joinTime)
                                    .orderBy("timestamp", Query.Direction.ASCENDING)
                                    .addSnapshotListener((value, error) -> {

                                        if (error != null) {
                                            error.printStackTrace();
                                            return;
                                        }

                                        if (value == null) return;

                                        List<Message> newList = new ArrayList<>();

                                        for (DocumentSnapshot doc : value.getDocuments()) {

                                            Message message = doc.toObject(Message.class);

                                            if (message == null) continue;

                                            message.setMessageId(doc.getId());

                                            newList.add(message);
                                        }

                                        DiffUtil.DiffResult diff =
                                                DiffUtil.calculateDiff(
                                                        new MessageDiffCallback(
                                                                messageList,
                                                                newList
                                                        )
                                                );

                                        messageList.clear();
                                        messageList.addAll(newList);

                                        adapter.rebuildMessageMap();

                                        diff.dispatchUpdatesTo(adapter);

                                        if (!messageList.isEmpty()) {
                                            recyclerMessages.scrollToPosition(
                                                    messageList.size() - 1
                                            );
                                        }

                                    });

                });

    }
    private void loadOldGroupMessages() {

        firestore.collection("GroupMessages")
                .document(groupId)
                .collection("Messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(value -> {

                    List<Message> newList = new ArrayList<>();

                    for (DocumentSnapshot doc : value.getDocuments()) {

                        Message message = doc.toObject(Message.class);

                        if (message == null) continue;

                        message.setMessageId(doc.getId());

                        newList.add(message);
                    }

                    DiffUtil.DiffResult diff =
                            DiffUtil.calculateDiff(
                                    new MessageDiffCallback(
                                            messageList,
                                            newList
                                    )
                            );

                    messageList.clear();
                    messageList.addAll(newList);

                    adapter.rebuildMessageMap();

                    diff.dispatchUpdatesTo(adapter);

                    if (!messageList.isEmpty()) {

                        recyclerMessages.scrollToPosition(
                                messageList.size() - 1
                        );

                    }

                });

    }
    private void verifyGroupMembership() {

        String myUid = auth.getCurrentUser().getUid();

        firestore.collection("Groups")
                .document(groupId)
                .addSnapshotListener((document, error) -> {

                    if (error != null) {
                        Log.e("Group", error.getMessage());
                        return;
                    }

                    if (document == null || !document.exists()) {

                        Toast.makeText(
                                ChatActivity.this,
                                "Group no longer exists",
                                Toast.LENGTH_SHORT
                        ).show();

                        finish();
                        return;
                    }

                    Group group = document.toObject(Group.class);

                    if (group == null) {
                        finish();
                        return;
                    }


                    if (group.getMembers().contains(myUid)) {

                        isCurrentMember = true;

                        layoutInput.setVisibility(View.VISIBLE);

                        tvUserStatus.setText("👥 Group");


                        loadGroupMessages();

                    }


                    else {

                        isCurrentMember = false;

                        layoutInput.setVisibility(View.GONE);

                        tvUserStatus.setText("You left this group");

                        // Stop live listener
                        if (groupMessageListener != null) {

                            groupMessageListener.remove();
                            groupMessageListener = null;

                        }


                        loadOldGroupMessages();

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

        if (isGroup) {

            tvChatName.setText(groupName);

            tvUserStatus.setText("👥 Group");

            imgUser.setImageResource(android.R.drawable.ic_menu_myplaces);

            return;
        }

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

        ChatUploadManager.uploadImage(
                this,
                imageUri,
                new ChatUploadManager.UploadListener() {

                    @Override
                    public void onStart() {

                        String senderId = auth.getCurrentUser().getUid();

                        Message tempMessage = new Message(
                                senderId,
                                receiverId,
                                "",
                                "",
                                "image",
                                System.currentTimeMillis()
                        );

                        tempMessage.setUploading(true);
                        tempMessage.setLocalImageUri(imageUri);

                        messageList.add(tempMessage);
                        adapter.notifyItemInserted(messageList.size() - 1);
                        recyclerMessages.scrollToPosition(messageList.size() - 1);

                    }

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
        message.setUploading(false);
        firestore.collection("Messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {}



                );

    }
    private void showReactionPicker(View anchor, Message message) {

        View popupView = getLayoutInflater().inflate(
                R.layout.reaction_popup,
                null
        );

        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        popupWindow.setElevation(12f);

        popupView.findViewById(R.id.reactionHeart).setOnClickListener(v -> {
            addReaction(message, "❤️");
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.reactionLaugh).setOnClickListener(v -> {
            addReaction(message, "😂");
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.reactionWow).setOnClickListener(v -> {
            addReaction(message, "😮");
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.reactionSad).setOnClickListener(v -> {
            addReaction(message, "😢");
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.reactionAngry).setOnClickListener(v -> {
            addReaction(message, "😡");
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.reactionLike).setOnClickListener(v -> {
            addReaction(message, "👍");
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.reactionDislike).setOnClickListener(v -> {
            addReaction(message, "👎");
            popupWindow.dismiss();
        });

        popupWindow.showAsDropDown(anchor, 0, -anchor.getHeight() * 2);
    }

    private void addReaction(Message message, String emoji) {

        String uid = auth.getCurrentUser().getUid();

        DocumentReference messageRef = firestore
                .collection("Messages")
                .document(message.getMessageId());

        messageRef.get().addOnSuccessListener(snapshot -> {

            Message latest = snapshot.toObject(Message.class);

            if (latest == null) return;

            java.util.Map<String, Object> reactions = snapshot.get("reactions") instanceof java.util.Map
                    ? (java.util.Map<String, Object>) snapshot.get("reactions")
                    : new java.util.HashMap<>();

            Object currentReaction = reactions.get(uid);

            // Already reacted with ❤️ → remove it
            if (emoji.equals(currentReaction)) {

                messageRef.update(
                        "reactions." + uid,
                        com.google.firebase.firestore.FieldValue.delete()
                );

            } else {

                // Add or change reaction
                messageRef.update(
                        "reactions." + uid,
                        emoji
                );

            }

        });

    }

    private void showReactionPopup(View anchor, Message message) {

        View popupView = getLayoutInflater().inflate(
                R.layout.reaction_popup,
                null
        );

        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        popupWindow.setElevation(12f);

        popupView.findViewById(R.id.reactionHeart).setOnClickListener(v -> {
            addReaction(message, "❤️");
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.reactionLaugh).setOnClickListener(v -> {
            addReaction(message, "😂");
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.reactionWow).setOnClickListener(v -> {
            addReaction(message, "😮");
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.reactionSad).setOnClickListener(v -> {
            addReaction(message, "😢");
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.reactionAngry).setOnClickListener(v -> {
            addReaction(message, "😡");
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.reactionLike).setOnClickListener(v -> {
            addReaction(message, "👍");
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.reactionDislike).setOnClickListener(v -> {
            addReaction(message, "👎");
            popupWindow.dismiss();
        });

        popupWindow.showAsDropDown(anchor, 0, -anchor.getHeight() * 2);

    }
    private void startVoiceRecording() {
        if (isRecordingVoice) return;

        try {
            voiceFile = new File(getCacheDir(), "voice_" + System.currentTimeMillis() + ".m4a");

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setOutputFile(voiceFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecordingVoice = true;
            voiceStartTime = System.currentTimeMillis();
            recordingRunnable = new Runnable() {
                @Override
                public void run() {

                    long elapsed = System.currentTimeMillis() - voiceStartTime;

                    tvRecordingTime.setText(formatVoiceDuration(elapsed));

                    recordingHandler.postDelayed(this, 200);
                }
            };

            recordingHandler.post(recordingRunnable);
            layoutInput.setVisibility(View.GONE);
            layoutRecording.setVisibility(View.VISIBLE);

            Toast.makeText(this, "Recording...", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            isRecordingVoice = false;
            releaseVoiceRecorder();
            Toast.makeText(this, "Recording failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopVoiceRecording() {

        if (!isRecordingVoice) return;

        long duration = System.currentTimeMillis() - voiceStartTime;
        isRecordingVoice = false;

        try {
            mediaRecorder.stop();
        } catch (Exception ignored) {

            if (voiceFile != null && voiceFile.exists()) {
                voiceFile.delete();
            }

            releaseVoiceRecorder();
            recordingHandler.removeCallbacks(recordingRunnable);

            tvSlideCancel.setText("◀ Slide to cancel");
            tvRecordingTime.setText("00:00");

            layoutRecording.setVisibility(View.GONE);
            layoutInput.setVisibility(View.VISIBLE);

            return;
        }

        releaseVoiceRecorder();

        if (voiceFile == null || !voiceFile.exists()) {
            recordingHandler.removeCallbacks(recordingRunnable);

            tvSlideCancel.setText("◀ Slide to cancel");
            tvRecordingTime.setText("00:00");

            layoutRecording.setVisibility(View.GONE);
            layoutInput.setVisibility(View.VISIBLE);

            return;
        }

        if (duration < 800) {

            voiceFile.delete();

            recordingHandler.removeCallbacks(recordingRunnable);

            tvSlideCancel.setText("◀ Slide to cancel");
            tvRecordingTime.setText("00:00");

            layoutRecording.setVisibility(View.GONE);
            layoutInput.setVisibility(View.VISIBLE);

            Toast.makeText(this, "Voice message too short", Toast.LENGTH_SHORT).show();

            return;
        }
        uploadVoiceMessage(voiceFile, duration);

        recordingHandler.removeCallbacks(recordingRunnable);

        tvSlideCancel.setText("◀ Slide to cancel");
        tvRecordingTime.setText("00:00");

        layoutRecording.setVisibility(View.GONE);
        layoutInput.setVisibility(View.VISIBLE);
    }

    private void cancelVoiceRecording() {

        isRecordingVoice = false;

        try {

            if (mediaRecorder != null) {

                mediaRecorder.stop();

            }

        } catch (Exception ignored) {
        }

        releaseVoiceRecorder();

        if (voiceFile != null && voiceFile.exists()) {

            voiceFile.delete();

        }

        recordingHandler.removeCallbacks(recordingRunnable);

        tvSlideCancel.setText("◀ Slide to cancel");

        tvRecordingTime.setText("00:00");

        layoutRecording.setVisibility(View.GONE);
        layoutInput.setVisibility(View.VISIBLE);

        Toast.makeText(this, "Recording cancelled", Toast.LENGTH_SHORT).show();
    }

    private void releaseVoiceRecorder() {
        try {
            if (mediaRecorder != null) {
                mediaRecorder.reset();
                mediaRecorder.release();
            }
        } catch (Exception ignored) {
        }
        mediaRecorder = null;
    }
    private void uploadVoiceMessage(File file, long durationMs) {
        VoiceUploadManager.uploadVoice(file, new VoiceUploadManager.UploadListener() {
            @Override
            public void onStart() {
                runOnUiThread(() ->
                        Toast.makeText(ChatActivity.this, "Uploading voice...", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onSuccess(String voiceUrl) {
                runOnUiThread(() -> sendVoiceMessage(file, voiceUrl, durationMs));
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(ChatActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private String formatVoiceDuration(long durationMs) {

        long seconds = durationMs / 1000;

        long minutes = seconds / 60;

        seconds = seconds % 60;

        return String.format(
                java.util.Locale.getDefault(),
                "%d:%02d",
                minutes,
                seconds
        );
    }

    private void sendVoiceMessage(
            File voiceFile,
            String voiceUrl,
            long durationMs
    ) {
        String senderId = auth.getCurrentUser().getUid();

        Message message = new Message(
                senderId,
                receiverId,
                "",
                "",
                "voice",
                System.currentTimeMillis()
        );

        message.setVoiceUrl(voiceUrl);
        message.setVoiceDuration(durationMs);
        message.setWaveform(
                WaveformGenerator.generate(voiceFile)
        );
        message.setUploading(false);

        firestore.collection("Messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    documentReference.update("messageId", documentReference.getId());

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
                                            String chatId = ChatHelper.getChatId(senderId, receiverId);

                                            java.util.Map<String, Object> chatMap = new java.util.HashMap<>();
                                            chatMap.put("senderId", senderId);
                                            chatMap.put("receiverId", receiverId);
                                            chatMap.put("senderName", senderDoc.getString("name"));
                                            chatMap.put("receiverName", receiverDoc.getString("name"));
                                            chatMap.put("senderImage", senderDoc.getString("image"));
                                            chatMap.put("receiverImage", receiverDoc.getString("image"));
                                            chatMap.put("lastMessage", "🎤 Voice message");
                                            chatMap.put("lastTimestamp", System.currentTimeMillis());
                                            chatMap.put("senderUnread", 0);
                                            chatMap.put("receiverUnread", 1);

                                            firestore.collection("Chats")
                                                    .document(chatId)
                                                    .set(chatMap);
                                        });
                            });
                });
    }

}