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
import com.example.connecthub.models.GroupMemberInfo;
import com.example.connecthub.models.MembershipPeriod;
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
import java.util.Map;

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
    private ListenerRegistration deletedGroupMessagesListener;
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

    private String myName = "";
    private ListenerRegistration deletedMessagesListener;

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
        firestore.collection("Users")
                .document(auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(doc -> {

                    if (doc.exists()) {

                        myName = doc.getString("name");

                    }

                });

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

                    if (isGroup) {

                        deleteGroupMessageForMe(message);

                    } else {

                        deleteForMe(message);

                    }

                    return true;
                }



                if (item.getItemId() == R.id.menu_delete_everyone) {

                    DocumentReference ref;

                    if (isGroup) {

                        ref = firestore.collection("GroupMessages")
                                .document(groupId)
                                .collection("Messages")
                                .document(message.getMessageId());

                    } else {

                        ref = firestore.collection("Messages")
                                .document(message.getMessageId());

                    }

                    ref.update(
                            "deleted", true,
                            "message", "",
                            "imageUrl", "",
                            "voiceUrl", "",
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
        message.setSenderName(myName);
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
                            : replyingMessage.getSenderName()
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

        message.setSenderName(myName);

        message.setGroupId(groupId);

        // ==========================
        // Reply Support
        // ==========================

        if (replyingMessage != null) {

            message.setReplyMessageId(replyingMessage.getMessageId());

            if ("image".equals(replyingMessage.getType())) {

                message.setReplyMessage("Photo");
                message.setReplyImageUrl(replyingMessage.getImageUrl());

            } else if ("voice".equals(replyingMessage.getType())) {

                message.setReplyMessage("Voice message");

            } else {

                message.setReplyMessage(replyingMessage.getMessage());

            }

            message.setReplySender(
                    replyingMessage.getSenderId().equals(senderId)
                            ? "You"
                            : replyingMessage.getSenderName()
            );

            message.setReplyType(replyingMessage.getType());
        }

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
    private void markGroupMessagesAsSeen() {

        if (!isCurrentMember || groupId == null) {
            return;
        }

        String currentUid = auth.getCurrentUser().getUid();

        firestore.collection("GroupMessages")
                .document(groupId)
                .collection("Messages")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {

                        Message message =
                                document.toObject(Message.class);

                        if (message == null) {
                            continue;
                        }

                        // Don't mark our own messages as seen
                        if (currentUid.equals(message.getSenderId())) {
                            continue;
                        }

                        document.getReference().update(
                                "seenBy." + currentUid,
                                true
                        );
                    }

                })
                .addOnFailureListener(e -> {
                    Log.e(
                            "GROUP_SEEN",
                            "Failed to mark messages as seen",
                            e
                    );
                });
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

        String myUid = auth.getCurrentUser().getUid();

        // Remove old deleted-message listener if one exists
        if (deletedMessagesListener != null) {
            deletedMessagesListener.remove();
            deletedMessagesListener = null;
        }

        /*
         * Listen to messages deleted FOR THIS USER.
         *
         * Whenever deleteForMe() adds/removes a document here,
         * this listener fires again and the chat is rebuilt.
         */
        deletedMessagesListener =
                firestore.collection("DeletedMessages")
                        .document(myUid)
                        .collection("Messages")
                        .addSnapshotListener((deletedSnapshot, deletedError) -> {

                            if (deletedError != null) {
                                Log.e(
                                        "DeletedMessages",
                                        "Error loading deleted messages",
                                        deletedError
                                );
                                return;
                            }

                            if (deletedSnapshot == null) return;

                            java.util.Set<String> deletedMessageIds =
                                    new java.util.HashSet<>();

                            for (DocumentSnapshot doc :
                                    deletedSnapshot.getDocuments()) {

                                deletedMessageIds.add(doc.getId());
                            }

                            /*
                             * Now listen to the actual chat messages.
                             */
                            firestore.collection("Messages")
                                    .orderBy(
                                            "timestamp",
                                            Query.Direction.ASCENDING
                                    )
                                    .addSnapshotListener((value, error) -> {

                                        if (error != null) {
                                            Log.e(
                                                    "Messages",
                                                    "Error loading messages",
                                                    error
                                            );
                                            return;
                                        }

                                        if (value == null) return;

                                        List<Message> newList =
                                                new ArrayList<>();

                                        for (DocumentSnapshot document :
                                                value.getDocuments()) {

                                            Message message =
                                                    document.toObject(
                                                            Message.class
                                                    );

                                            if (message == null) continue;

                                            String messageId =
                                                    document.getId();

                                            message.setMessageId(messageId);

                                            /*
                                             * ==================================
                                             * DELETE FOR ME FILTER
                                             * ==================================
                                             *
                                             * If this message exists inside
                                             * DeletedMessages/{myUid}/Messages,
                                             * don't add it to the RecyclerView.
                                             */
                                            if (deletedMessageIds.contains(
                                                    messageId
                                            )) {
                                                continue;
                                            }

                                            /*
                                             * ==================================
                                             * CHECK PRIVATE CHAT
                                             * ==================================
                                             */

                                            String messageSender =
                                                    message.getSenderId();

                                            String messageReceiver =
                                                    message.getReceiverId();

                                            if (messageSender == null ||
                                                    messageReceiver == null) {
                                                continue;
                                            }

                                            boolean chat1 =
                                                    messageSender.equals(myUid)
                                                            &&
                                                            messageReceiver.equals(
                                                                    receiverId
                                                            );

                                            boolean chat2 =
                                                    messageSender.equals(
                                                            receiverId
                                                    )
                                                            &&
                                                            messageReceiver.equals(
                                                                    myUid
                                                            );

                                            if (!chat1 && !chat2) {
                                                continue;
                                            }

                                            /*
                                             * ==================================
                                             * MARK RECEIVED MESSAGE AS SEEN
                                             * ==================================
                                             */

                                            if (messageReceiver.equals(myUid)
                                                    && !message.isSeen()) {

                                                document.getReference()
                                                        .update(
                                                                "seen",
                                                                true
                                                        );
                                            }

                                            newList.add(message);
                                        }

                                        /*
                                         * ==================================
                                         * SCROLL LOGIC
                                         * ==================================
                                         */

                                        LinearLayoutManager lm =
                                                (LinearLayoutManager)
                                                        recyclerMessages
                                                                .getLayoutManager();

                                        boolean shouldScroll = false;

                                        if (lm != null &&
                                                !messageList.isEmpty()) {

                                            int lastVisible =
                                                    lm.findLastCompletelyVisibleItemPosition();

                                            shouldScroll =
                                                    lastVisible >=
                                                            messageList.size() - 2;
                                        }

                                        int oldSize =
                                                messageList.size();

                                        /*
                                         * ==================================
                                         * DIFF UPDATE
                                         * ==================================
                                         */

                                        DiffUtil.DiffResult diffResult =
                                                DiffUtil.calculateDiff(
                                                        new MessageDiffCallback(
                                                                messageList,
                                                                newList
                                                        )
                                                );

                                        messageList.clear();
                                        messageList.addAll(newList);

                                        adapter.rebuildMessageMap();

                                        diffResult.dispatchUpdatesTo(adapter);

                                        /*
                                         * ==================================
                                         * ONLY SCROLL IF A NEW MESSAGE
                                         * WAS ADDED
                                         * ==================================
                                         */

                                        if (shouldScroll &&
                                                newList.size() > oldSize) {

                                            recyclerMessages.scrollToPosition(
                                                    newList.size() - 1
                                            );
                                        }

                                    });

                        });
    }


    private void loadGroupMessages() {

        String myUid = auth.getCurrentUser().getUid();

        firestore.collection("Groups")
                .document(groupId)
                .get()
                .addOnSuccessListener(groupDoc -> {

                    if (!groupDoc.exists()) return;

                    Group group =
                            groupDoc.toObject(Group.class);

                    if (group == null) return;

                    Map<String, List<MembershipPeriod>> memberHistory =
                            group.getMemberHistory();

                    if (memberHistory == null ||
                            !memberHistory.containsKey(myUid)) {

                        return;
                    }

                    List<MembershipPeriod> periods =
                            memberHistory.get(myUid);

                    /*
                     * ==========================================
                     * REMOVE OLD GROUP MESSAGE LISTENER
                     * ==========================================
                     */

                    if (groupMessageListener != null) {

                        groupMessageListener.remove();
                        groupMessageListener = null;
                    }

                    /*
                     * ==========================================
                     * REMOVE OLD DELETED-MESSAGE LISTENER
                     * ==========================================
                     */

                    if (deletedGroupMessagesListener != null) {

                        deletedGroupMessagesListener.remove();
                        deletedGroupMessagesListener = null;
                    }

                    /*
                     * ==========================================
                     * LISTEN TO "DELETE FOR ME" MESSAGES
                     * ==========================================
                     */

                    deletedGroupMessagesListener =
                            firestore.collection("DeletedGroupMessages")
                                    .document(myUid)
                                    .collection("Groups")
                                    .document(groupId)
                                    .collection("Messages")
                                    .addSnapshotListener(
                                            (deletedSnapshot,
                                             deletedError) -> {

                                                if (deletedError != null) {

                                                    Log.e(
                                                            "DeletedGroupMessages",
                                                            "Error loading deleted group messages",
                                                            deletedError
                                                    );

                                                    return;
                                                }

                                                if (deletedSnapshot == null) {
                                                    return;
                                                }

                                                /*
                                                 * Store all message IDs
                                                 * deleted by THIS user.
                                                 */

                                                java.util.Set<String>
                                                        deletedMessageIds =
                                                        new java.util.HashSet<>();

                                                for (DocumentSnapshot doc :
                                                        deletedSnapshot
                                                                .getDocuments()) {

                                                    deletedMessageIds.add(
                                                            doc.getId()
                                                    );
                                                }

                                                /*
                                                 * ==================================
                                                 * GROUP MESSAGE LISTENER
                                                 * ==================================
                                                 */

                                                groupMessageListener =
                                                        firestore.collection(
                                                                        "GroupMessages"
                                                                )
                                                                .document(groupId)
                                                                .collection(
                                                                        "Messages"
                                                                )
                                                                .orderBy(
                                                                        "timestamp",
                                                                        Query.Direction.ASCENDING
                                                                )
                                                                .addSnapshotListener(
                                                                        (
                                                                                value,
                                                                                error
                                                                        ) -> {

                                                                            if (error != null) {

                                                                                Log.e(
                                                                                        "GroupMessages",
                                                                                        "Error loading group messages",
                                                                                        error
                                                                                );

                                                                                return;
                                                                            }

                                                                            if (value == null) {
                                                                                return;
                                                                            }

                                                                            List<Message>
                                                                                    newList =
                                                                                    new ArrayList<>();

                                                                            /*
                                                                             * ==================================
                                                                             * PROCESS EVERY GROUP MESSAGE
                                                                             * ==================================
                                                                             */

                                                                            for (
                                                                                    DocumentSnapshot doc :
                                                                                    value.getDocuments()
                                                                            ) {

                                                                                Message message =
                                                                                        doc.toObject(
                                                                                                Message.class
                                                                                        );

                                                                                if (message == null) {
                                                                                    continue;
                                                                                }

                                                                                String messageId =
                                                                                        doc.getId();

                                                                                message.setMessageId(
                                                                                        messageId
                                                                                );

                                                                                /*
                                                                                 * ==================================
                                                                                 * DELETE FOR ME FILTER
                                                                                 * ==================================
                                                                                 *
                                                                                 * If THIS USER deleted this
                                                                                 * message, don't show it.
                                                                                 */

                                                                                if (
                                                                                        deletedMessageIds.contains(
                                                                                                messageId
                                                                                        )
                                                                                ) {

                                                                                    continue;
                                                                                }

                                                                                /*
                                                                                 * ==================================
                                                                                 * MEMBERSHIP HISTORY FILTER
                                                                                 * ==================================
                                                                                 */

                                                                                long messageTime =
                                                                                        message.getTimestamp();

                                                                                boolean visible =
                                                                                        false;

                                                                                for (
                                                                                        MembershipPeriod period :
                                                                                        periods
                                                                                ) {

                                                                                    if (period == null) {
                                                                                        continue;
                                                                                    }

                                                                                    long joined =
                                                                                            period.getJoinedAt();

                                                                                    Long left =
                                                                                            period.getLeftAt();

                                                                                    /*
                                                                                     * User is currently
                                                                                     * in the group.
                                                                                     */

                                                                                    if (left == null) {

                                                                                        if (
                                                                                                messageTime >=
                                                                                                        joined
                                                                                        ) {

                                                                                            visible = true;
                                                                                            break;
                                                                                        }

                                                                                    }

                                                                                    /*
                                                                                     * User previously
                                                                                     * left the group.
                                                                                     */

                                                                                    else {

                                                                                        if (
                                                                                                messageTime >=
                                                                                                        joined
                                                                                                        &&
                                                                                                        messageTime <=
                                                                                                                left
                                                                                        ) {

                                                                                            visible = true;
                                                                                            break;
                                                                                        }
                                                                                    }
                                                                                }

                                                                                /*
                                                                                 * Add only messages that
                                                                                 * belong to one of the
                                                                                 * user's membership periods.
                                                                                 */

                                                                                if (visible) {

                                                                                    newList.add(
                                                                                            message
                                                                                    );
                                                                                }
                                                                            }

                                                                            /*
                                                                             * ==================================
                                                                             * SCROLL POSITION
                                                                             * ==================================
                                                                             */

                                                                            LinearLayoutManager lm =
                                                                                    (
                                                                                            LinearLayoutManager
                                                                                            )
                                                                                            recyclerMessages
                                                                                                    .getLayoutManager();

                                                                            boolean shouldScroll =
                                                                                    false;

                                                                            if (
                                                                                    lm != null
                                                                                            &&
                                                                                            !messageList
                                                                                                    .isEmpty()
                                                                            ) {

                                                                                int lastVisible =
                                                                                        lm.findLastCompletelyVisibleItemPosition();

                                                                                shouldScroll =
                                                                                        lastVisible >=
                                                                                                messageList.size() - 2;
                                                                            }

                                                                            int oldSize =
                                                                                    messageList.size();

                                                                            /*
                                                                             * ==================================
                                                                             * DIFFUTIL
                                                                             * ==================================
                                                                             */

                                                                            DiffUtil.DiffResult diff =
                                                                                    DiffUtil.calculateDiff(
                                                                                            new MessageDiffCallback(
                                                                                                    messageList,
                                                                                                    newList
                                                                                            )
                                                                                    );

                                                                            messageList.clear();
                                                                            messageList.addAll(
                                                                                    newList
                                                                            );

                                                                            adapter.rebuildMessageMap();

                                                                            diff.dispatchUpdatesTo(
                                                                                    adapter
                                                                            );

                                                                            /*
                                                                             * ==================================
                                                                             * SCROLL ONLY FOR NEW MESSAGE
                                                                             * ==================================
                                                                             */

                                                                            if (
                                                                                    shouldScroll
                                                                                            &&
                                                                                            newList.size() >
                                                                                                    oldSize
                                                                            ) {

                                                                                recyclerMessages
                                                                                        .scrollToPosition(
                                                                                                newList.size() - 1
                                                                                        );
                                                                            }

                                                                        }
                                                                );

                                            }
                                    );

                });
    }


    private void loadOldGroupMessages() {

        String myUid = auth.getCurrentUser().getUid();

        firestore.collection("Groups")
                .document(groupId)
                .get()
                .addOnSuccessListener(groupDoc -> {

                    if (!groupDoc.exists()) return;

                    Group group = groupDoc.toObject(Group.class);

                    if (group == null) return;

                    Map<String, List<MembershipPeriod>> memberHistory =
                            group.getMemberHistory();

                    if (memberHistory == null ||
                            !memberHistory.containsKey(myUid)) {
                        return;
                    }

                    List<MembershipPeriod> periods =
                            memberHistory.get(myUid);

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
                                    DocumentReference msgRef =
                                            doc.getReference();

                                    Map<String, Long> readBy =
                                            message.getReadBy();

                                    if (readBy == null || !readBy.containsKey(myUid)) {

                                        msgRef.update(
                                                "readBy." + myUid,
                                                System.currentTimeMillis()
                                        );

                                    }

                                    long messageTime = message.getTimestamp();

                                    boolean visible = false;

                                    for (MembershipPeriod period : periods) {

                                        long joined = period.getJoinedAt();

                                        Long left = period.getLeftAt();

                                        if (left == null) {

                                            if (messageTime >= joined) {
                                                visible = true;
                                                break;
                                            }

                                        } else {

                                            if (messageTime >= joined &&
                                                    messageTime <= left) {

                                                visible = true;
                                                break;
                                            }

                                        }

                                    }

                                    if (visible) {
                                        newList.add(message);
                                    }

                                }
                                LinearLayoutManager lm =
                                        (LinearLayoutManager) recyclerMessages.getLayoutManager();

                                boolean shouldScroll =
                                        lm != null &&
                                                lm.findLastCompletelyVisibleItemPosition()
                                                        >= messageList.size() - 2;

                                int oldSize = messageList.size();

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

                                if (shouldScroll && newList.size() > oldSize) {

                                    recyclerMessages.scrollToPosition(
                                            newList.size() - 1
                                    );

                                }

                            });

                });

    }
    private void deleteForMe(Message message) {

        String uid = auth.getCurrentUser().getUid();

        if (message.getMessageId() == null ||
                message.getMessageId().isEmpty()) {

            Toast.makeText(
                    this,
                    "Unable to delete message",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        firestore.collection("DeletedMessages")
                .document(uid)
                .collection("Messages")
                .document(message.getMessageId())
                .set(new java.util.HashMap<String, Object>() {{
                    put("deletedAt", FieldValue.serverTimestamp());
                }})
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            ChatActivity.this,
                            "Message deleted",
                            Toast.LENGTH_SHORT
                    ).show();

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            ChatActivity.this,
                            "Delete failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();

                });
    }

    private void deleteGroupMessageForMe(Message message) {

        String uid = auth.getCurrentUser().getUid();

        String messageId = message.getMessageId();

        if (messageId == null || messageId.isEmpty()) {

            Toast.makeText(
                    this,
                    "Unable to delete message",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        firestore.collection("DeletedGroupMessages")
                .document(uid)
                .collection("Groups")
                .document(groupId)
                .collection("Messages")
                .document(messageId)
                .set(new java.util.HashMap<String, Object>() {{
                    put("deletedAt", FieldValue.serverTimestamp());
                    put("groupId", groupId);
                    put("messageId", messageId);
                }})
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            ChatActivity.this,
                            "Message deleted",
                            Toast.LENGTH_SHORT
                    ).show();

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            ChatActivity.this,
                            "Delete failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();

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
                                this,
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

                    Map<String, GroupMemberInfo> memberInfo =
                            group.getMemberInfo();

                    if (memberInfo == null ||
                            !memberInfo.containsKey(myUid)) {

                        layoutInput.setVisibility(View.GONE);

                        tvUserStatus.setText("You left this group");

                        if (groupMessageListener != null) {
                            groupMessageListener.remove();
                            groupMessageListener = null;
                        }

                        if (!groupMessagesLoaded) {

                            groupMessagesLoaded = true;

                            loadOldGroupMessages();

                        }

                        return;
                    }

                    GroupMemberInfo info = memberInfo.get(myUid);

                    if (info.isActive()) {

                        isCurrentMember = true;

                        layoutInput.setVisibility(View.VISIBLE);

                        tvUserStatus.setText("👥 Group");

                        if (groupMessageListener == null) {

                            loadGroupMessages();

                        }

                    } else {

                        isCurrentMember = false;

                        layoutInput.setVisibility(View.GONE);

                        tvUserStatus.setText("You left this group");

                        if (groupMessageListener != null) {

                            groupMessageListener.remove();

                            groupMessageListener = null;

                        }

                        if (!groupMessagesLoaded) {

                            groupMessagesLoaded = true;

                            loadOldGroupMessages();

                        }

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

        if (isGroup) {

            if (!isCurrentMember) {

                Toast.makeText(
                        this,
                        "You left this group",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            String senderId =
                    auth.getCurrentUser().getUid();

            Message message = new Message(
                    senderId,
                    "",
                    "",
                    imageUrl,
                    "image",
                    System.currentTimeMillis()
            );

            message.setGroupId(groupId);
            message.setUploading(false);

            /*
             * Reply information
             */
            if (replyingMessage != null) {

                message.setReplyMessageId(
                        replyingMessage.getMessageId()
                );

                message.setReplySender(
                        replyingMessage.getSenderId()
                                .equals(senderId)
                                ? "You"
                                : replyingMessage.getSenderName()
                );

                message.setReplyType(
                        replyingMessage.getType()
                );

                if ("image".equals(
                        replyingMessage.getType()
                )) {

                    message.setReplyMessage("Photo");

                    message.setReplyImageUrl(
                            replyingMessage.getImageUrl()
                    );

                } else if ("voice".equals(
                        replyingMessage.getType()
                )) {

                    message.setReplyMessage(
                            "🎤 Voice message"
                    );

                } else {

                    message.setReplyMessage(
                            replyingMessage.getMessage()
                    );
                }

                replyingMessage = null;
                layoutReply.setVisibility(View.GONE);
            }

            firestore.collection("GroupMessages")
                    .document(groupId)
                    .collection("Messages")
                    .add(message)
                    .addOnSuccessListener(documentReference -> {

                        documentReference.update(
                                "messageId",
                                documentReference.getId()
                        );

                        /*
                         * Update group preview
                         */
                        firestore.collection("Groups")
                                .document(groupId)
                                .update(
                                        "lastMessage",
                                        "📷 Photo",
                                        "lastTimestamp",
                                        System.currentTimeMillis()
                                );

                    })
                    .addOnFailureListener(e -> {

                        Toast.makeText(
                                ChatActivity.this,
                                "Image send failed: "
                                        + e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();

                    });

            return;
        }

        /*
         * ==============================
         * PRIVATE CHAT
         * ==============================
         */

        String senderId =
                auth.getCurrentUser().getUid();

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
                .addOnSuccessListener(documentReference -> {

                    documentReference.update(
                            "messageId",
                            documentReference.getId()
                    );

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            ChatActivity.this,
                            "Image send failed: "
                                    + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();

                });
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

        DocumentReference messageRef;

        if (isGroup) {

            messageRef = firestore.collection("GroupMessages")
                    .document(groupId)
                    .collection("Messages")
                    .document(message.getMessageId());

        } else {

            messageRef = firestore.collection("Messages")
                    .document(message.getMessageId());

        }

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

        if (isGroup) {

            if (!isCurrentMember) {

                Toast.makeText(
                        this,
                        "You left this group",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            String senderId =
                    auth.getCurrentUser().getUid();

            Message message = new Message(
                    senderId,
                    "",
                    "",
                    "",
                    "voice",
                    System.currentTimeMillis()
            );

            message.setGroupId(groupId);

            message.setVoiceUrl(voiceUrl);

            message.setVoiceDuration(durationMs);

            message.setWaveform(
                    WaveformGenerator.generate(voiceFile)
            );

            message.setUploading(false);

            /*
             * Reply information
             */
            if (replyingMessage != null) {

                message.setReplyMessageId(
                        replyingMessage.getMessageId()
                );

                message.setReplySender(
                        replyingMessage.getSenderId()
                                .equals(senderId)
                                ? "You"
                                : replyingMessage.getSenderName()
                );

                message.setReplyType(
                        replyingMessage.getType()
                );

                if ("image".equals(
                        replyingMessage.getType()
                )) {

                    message.setReplyMessage("Photo");

                    message.setReplyImageUrl(
                            replyingMessage.getImageUrl()
                    );

                } else if ("voice".equals(
                        replyingMessage.getType()
                )) {

                    message.setReplyMessage(
                            "🎤 Voice message"
                    );

                } else {

                    message.setReplyMessage(
                            replyingMessage.getMessage()
                    );
                }

                replyingMessage = null;
                layoutReply.setVisibility(View.GONE);
            }

            firestore.collection("GroupMessages")
                    .document(groupId)
                    .collection("Messages")
                    .add(message)
                    .addOnSuccessListener(documentReference -> {

                        documentReference.update(
                                "messageId",
                                documentReference.getId()
                        );

                        firestore.collection("Groups")
                                .document(groupId)
                                .update(
                                        "lastMessage",
                                        "🎤 Voice message",
                                        "lastTimestamp",
                                        System.currentTimeMillis()
                                );

                        /*
                         * We can delete the local recording after
                         * Cloudinary upload + Firestore save.
                         */
                        if (voiceFile != null &&
                                voiceFile.exists()) {

                            voiceFile.delete();
                        }

                    })
                    .addOnFailureListener(e -> {

                        Toast.makeText(
                                ChatActivity.this,
                                "Voice send failed: "
                                        + e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();

                    });

            return;
        }

        /*
         * ==========================================
         * PRIVATE CHAT
         * ==========================================
         */

        String senderId =
                auth.getCurrentUser().getUid();

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

                    documentReference.update(
                            "messageId",
                            documentReference.getId()
                    );

                    firestore.collection("Users")
                            .document(senderId)
                            .update(
                                    "typingTo",
                                    ""
                            );

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            ChatActivity.this,
                            "Voice send failed: "
                                    + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();

                });
    }



}