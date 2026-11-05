package com.example.connecthub.activities;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.connecthub.ConnectHubApp;
import com.example.connecthub.R;
import com.example.connecthub.adapters.MessageAdapter;
import com.example.connecthub.adapters.MessageDiffCallback;
import com.example.connecthub.helpers.ChatHelper;
import com.example.connecthub.helpers.SwipeToReplyCallback;
import com.example.connecthub.helpers.TimeUtils;
import com.example.connecthub.models.Group;
import com.example.connecthub.models.MembershipPeriod;
import com.example.connecthub.models.Message;
import com.example.connecthub.network.CloudinaryUploader;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * One screen for private chats and group chats.
 *
 * Rewritten to fix, among others:
 *  - private chats downloaded the WHOLE "Messages" collection (every user's messages) and
 *    filtered on the device -> now only the two queries for this conversation are used
 *  - snapshot listeners were created inside other listeners and never removed (leaks,
 *    duplicated messages, crashes after the screen closed) -> all listeners are attached in
 *    onStart and removed in onStop
 *  - image / voice messages never updated the chat list or unread counters
 *  - unread counters were swapped between the two users after the first reply
 *  - every key stroke wrote "typing" to Firestore
 *  - "mark as seen" rewrote every group message each time the screen opened
 *  - a failed image upload left an "Uploading..." bubble behind forever
 */
public class ChatActivity extends BaseActivity {

    private static final long MIN_VOICE_MS = 700;
    private static final long TYPING_TIMEOUT_MS = 2500;

    // ----- views -----
    private RecyclerView recyclerMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private ImageButton btnMic;
    private ImageButton btnImage;
    private ImageButton btnCancelReply;
    private TextView tvChatName;
    private TextView tvStatus;
    private ShapeableImageView imgUser;
    private TextView tvRecordingTime;
    private TextView tvSlideCancel;
    private TextView tvReplySender;
    private TextView tvReplyMessage;
    private TextView tvNotMember;
    private View layoutInput;
    private View layoutRecording;
    private View layoutReply;

    // ----- data -----
    private MessageAdapter adapter;
    private final List<Message> messageList = new ArrayList<>();
    private final List<Message> uploadingMessages = new ArrayList<>();

    private FirebaseFirestore firestore;
    private String myUid;
    private String myName = "";
    private String myImage = "";

    private boolean isGroup;
    private String groupId;
    private String receiverId;
    private String chatTitle = "";
    private String chatImage = "";

    private Group group;
    private boolean isCurrentMember = true;

    private Message replyingMessage;

    // ----- listeners -----
    private ListenerRegistration statusListener;
    private ListenerRegistration groupListener;
    private ListenerRegistration outgoingListener;
    private ListenerRegistration incomingListener;
    private ListenerRegistration groupMessagesListener;
    private ListenerRegistration deletedListener;

    private final Map<String, Message> outgoingMessages = new HashMap<>();
    private final Map<String, Message> incomingMessages = new HashMap<>();
    private final Set<String> deletedIds = new HashSet<>();
    private final List<Message> rawGroupMessages = new ArrayList<>();

    // ----- typing -----
    private final Handler typingHandler = new Handler(Looper.getMainLooper());
    private boolean typingSent = false;
    private final Runnable stopTypingRunnable = this::sendTypingStopped;

    // ----- voice recording -----
    private MediaRecorder mediaRecorder;
    private File voiceFile;
    private boolean isRecordingVoice = false;
    private long voiceStartTime;
    private float touchStartX;
    private final List<Integer> amplitudes = new ArrayList<>();
    private final Handler recordingHandler = new Handler(Looper.getMainLooper());
    private final Runnable recordingRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRecordingVoice) return;

            long elapsed = System.currentTimeMillis() - voiceStartTime;
            tvRecordingTime.setText(formatDuration(elapsed));

            try {
                if (mediaRecorder != null) {
                    amplitudes.add(mediaRecorder.getMaxAmplitude());
                }
            } catch (Exception ignored) {
            }

            recordingHandler.postDelayed(this, 100);
        }
    };

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) sendImage(uri);
            });

    private final ActivityResultLauncher<String> micPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted ->
                    Toast.makeText(this,
                            granted ? "Hold the microphone button to record"
                                    : "Microphone permission is needed for voice messages",
                            Toast.LENGTH_SHORT).show());

    // =====================================================================
    // Lifecycle
    // =====================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        isGroup = getIntent().getBooleanExtra("isGroup", false);
        groupId = getIntent().getStringExtra("groupId");

        receiverId = getIntent().getStringExtra("uid");

        if (receiverId == null) {
            receiverId = getIntent().getStringExtra("receiverId");
        }

        // Missing / invalid data (e.g. opened from an old notification) must not crash
        if (user == null || (isGroup && groupId == null) || (!isGroup && receiverId == null)
                || (!isGroup && receiverId.equals(user.getUid()))) {
            Toast.makeText(this, "Chat not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        myUid = user.getUid();
        firestore = FirebaseFirestore.getInstance();

        bindViews();
        setupRecycler();
        setupInput();
        setupHeader();
        loadMyProfile();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (firestore == null) return; // onCreate bailed out

        ConnectHubApp.activeChatKey = isGroup ? groupId : receiverId;

        startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (firestore == null) return;

        ConnectHubApp.activeChatKey = null;

        stopListening();
        sendTypingStopped();

        if (isRecordingVoice) {
            stopVoiceRecording(true);
        }
    }

    @Override
    protected void onDestroy() {
        typingHandler.removeCallbacksAndMessages(null);
        recordingHandler.removeCallbacksAndMessages(null);

        releaseRecorder();

        if (adapter != null) {
            adapter.release();
        }

        super.onDestroy();
    }

    // =====================================================================
    // Setup
    // =====================================================================

    private void bindViews() {
        recyclerMessages = findViewById(R.id.recyclerMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnMic = findViewById(R.id.btnMic);
        btnImage = findViewById(R.id.btnImage);
        btnCancelReply = findViewById(R.id.btnCancelReply);
        tvChatName = findViewById(R.id.tvChatName);
        tvStatus = findViewById(R.id.tvStatus);
        imgUser = findViewById(R.id.imgUser);
        tvRecordingTime = findViewById(R.id.tvRecordingTime);
        tvSlideCancel = findViewById(R.id.tvSlideCancel);
        tvReplySender = findViewById(R.id.tvReplySender);
        tvReplyMessage = findViewById(R.id.tvReplyMessage);
        tvNotMember = findViewById(R.id.tvNotMember);
        layoutInput = findViewById(R.id.layoutInput);
        layoutRecording = findViewById(R.id.layoutRecording);
        layoutReply = findViewById(R.id.layoutReply);

        MaterialToolbar toolbar = findViewById(R.id.chatToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupHeader() {

        if (isGroup) {

            chatTitle = getIntent().getStringExtra("groupName");
            if (chatTitle == null) chatTitle = "Group";

            tvChatName.setText(chatTitle);
            tvStatus.setText("");
            imgUser.setImageResource(R.drawable.ic_group_avatar);

        } else {
            tvChatName.setText("");
            tvStatus.setText("");
        }

        findViewById(R.id.layoutChatHeader).setOnClickListener(v -> {

            if (isGroup) {
                Intent intent = new Intent(this, GroupInfoActivity.class);
                intent.putExtra("groupId", groupId);
                startActivity(intent);
            } else {
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.putExtra("uid", receiverId);
                startActivity(intent);
            }
        });
    }

    private void setupRecycler() {

        adapter = new MessageAdapter(messageList);
        adapter.setGroupChat(isGroup);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);

        recyclerMessages.setLayoutManager(layoutManager);
        recyclerMessages.setAdapter(adapter);
        recyclerMessages.setItemAnimator(null);

        adapter.setOnMessageLongClickListener(this::showMessageMenu);

        adapter.setOnMessageDoubleTapListener(message -> {
            if (!message.isDeleted() && !message.isUploading()) {
                toggleReaction(message, "❤️");
            }
        });

        adapter.setOnReplyClickListener(messageId -> {
            for (int i = 0; i < messageList.size(); i++) {
                if (messageId.equals(messageList.get(i).getMessageId())) {
                    recyclerMessages.smoothScrollToPosition(i);
                    return;
                }
            }
        });

        // Swipe a message to reply
        new ItemTouchHelper(new SwipeToReplyCallback(position -> {

            adapter.notifyItemChanged(position); // snap the bubble back

            if (position >= 0 && position < messageList.size()) {
                setReplyTarget(messageList.get(position));
            }

        })).attachToRecyclerView(recyclerMessages);

        // Keep the newest message visible when the keyboard opens
        recyclerMessages.addOnLayoutChangeListener((v, left, top, right, bottom,
                                                    oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) {
                recyclerMessages.post(this::scrollToBottom);
            }
        });
    }

    private void setupInput() {

        btnSend.setOnClickListener(v -> sendTextMessage());
        btnImage.setOnClickListener(v -> imagePicker.launch("image/*"));
        btnCancelReply.setOnClickListener(v -> clearReply());

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                boolean hasText = s.toString().trim().length() > 0;

                btnSend.setVisibility(hasText ? View.VISIBLE : View.GONE);
                btnMic.setVisibility(hasText ? View.GONE : View.VISIBLE);

                if (!isGroup) {
                    onTyping(hasText);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        setupMicButton();
    }

    private void loadMyProfile() {
        firestore.collection("Users")
                .document(myUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.getString("name") != null) myName = doc.getString("name");
                    if (doc.getString("image") != null) myImage = doc.getString("image");
                });
    }

    // =====================================================================
    // Listeners (attached in onStart, removed in onStop)
    // =====================================================================

    private void startListening() {

        stopListening(); // never stack listeners

        if (isGroup) {
            listenToGroup();
        } else {
            listenToUserStatus();
            listenToPrivateMessages();
        }
    }

    private void stopListening() {
        remove(statusListener);
        remove(groupListener);
        remove(outgoingListener);
        remove(incomingListener);
        remove(groupMessagesListener);
        remove(deletedListener);

        statusListener = null;
        groupListener = null;
        outgoingListener = null;
        incomingListener = null;
        groupMessagesListener = null;
        deletedListener = null;
    }

    private void remove(ListenerRegistration registration) {
        if (registration != null) registration.remove();
    }

    // ---------------------------------------------------------------------
    // Private chat
    // ---------------------------------------------------------------------

    private void listenToUserStatus() {

        statusListener = firestore.collection("Users")
                .document(receiverId)
                .addSnapshotListener((doc, error) -> {

                    if (doc == null || !doc.exists()) return;

                    String name = doc.getString("name");
                    String image = doc.getString("image");

                    chatTitle = name == null ? "" : name;
                    chatImage = image == null ? "" : image;

                    tvChatName.setText(chatTitle);

                    if (!chatImage.isEmpty()) {
                        Glide.with(this)
                                .load(chatImage)
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person)
                                .into(imgUser);
                    } else {
                        imgUser.setImageResource(R.drawable.ic_person);
                    }

                    String typingTo = doc.getString("typingTo");
                    Boolean online = doc.getBoolean("online");
                    Long lastSeen = doc.getLong("lastSeen");

                    if (myUid.equals(typingTo)) {
                        tvStatus.setText("typing...");
                    } else if (Boolean.TRUE.equals(online)) {
                        tvStatus.setText("Online");
                    } else {
                        tvStatus.setText("Last seen " + TimeUtils.getLastSeen(lastSeen == null ? 0 : lastSeen));
                    }
                });
    }

    private void listenToPrivateMessages() {

        // messages I removed "for me"
        deletedListener = firestore.collection("DeletedMessages")
                .document(myUid)
                .collection("Messages")
                .addSnapshotListener((snapshot, error) -> {

                    if (snapshot == null) return;

                    deletedIds.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        deletedIds.add(doc.getId());
                    }

                    rebuildPrivateList();
                });

        // Two narrow queries instead of downloading the entire Messages collection.
        // (Plain equality filters: no composite index needed.)
        outgoingListener = firestore.collection("Messages")
                .whereEqualTo("senderId", myUid)
                .whereEqualTo("receiverId", receiverId)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {

                    if (snapshot == null) return;

                    collectMessages(snapshot, outgoingMessages);
                    rebuildPrivateList();
                });

        incomingListener = firestore.collection("Messages")
                .whereEqualTo("senderId", receiverId)
                .whereEqualTo("receiverId", myUid)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {

                    if (snapshot == null) return;

                    collectMessages(snapshot, incomingMessages);
                    rebuildPrivateList();

                    markIncomingAsSeen(snapshot);
                });

        resetUnreadCounter();
    }

    private void collectMessages(QuerySnapshot snapshot, Map<String, Message> target) {

        target.clear();

        for (DocumentSnapshot doc : snapshot.getDocuments()) {

            Message message = doc.toObject(Message.class);

            if (message == null) continue;

            message.setMessageId(doc.getId());
            message.setPending(doc.getMetadata().hasPendingWrites());

            target.put(doc.getId(), message);
        }
    }

    private void rebuildPrivateList() {

        List<Message> list = new ArrayList<>();

        for (Message message : outgoingMessages.values()) {
            if (!deletedIds.contains(message.getMessageId())) list.add(message);
        }

        for (Message message : incomingMessages.values()) {
            if (!deletedIds.contains(message.getMessageId())) list.add(message);
        }

        sortByTime(list);
        applyList(list);
    }

    private void markIncomingAsSeen(QuerySnapshot snapshot) {

        // Not while the app is in the background: the listener only lives between onStart/onStop
        WriteBatch batch = firestore.batch();
        int count = 0;

        for (DocumentSnapshot doc : snapshot.getDocuments()) {

            if (doc.getMetadata().hasPendingWrites()) continue;

            Boolean seen = doc.getBoolean("seen");

            if (!Boolean.TRUE.equals(seen) && count < 400) {
                batch.update(doc.getReference(), "seen", true);
                count++;
            }
        }

        if (count > 0) {
            batch.commit();
            resetUnreadCounter();
        }
    }

    private void resetUnreadCounter() {

        String chatId = ChatHelper.getChatId(myUid, receiverId);

        // update() on a not-yet-existing chat simply fails; that is fine
        firestore.collection("Chats")
                .document(chatId)
                .update(FieldPath.of("unread", myUid), 0);
    }

    // ---------------------------------------------------------------------
    // Group chat
    // ---------------------------------------------------------------------

    private void listenToGroup() {

        groupListener = firestore.collection("Groups")
                .document(groupId)
                .addSnapshotListener((doc, error) -> {

                    if (doc == null) return;

                    if (!doc.exists()) {
                        Toast.makeText(this, "This group no longer exists", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    Group loaded = doc.toObject(Group.class);

                    if (loaded == null) return;

                    loaded.setGroupId(doc.getId());
                    group = loaded;

                    chatTitle = loaded.getGroupName() == null ? "Group" : loaded.getGroupName();
                    tvChatName.setText(chatTitle);

                    if (loaded.getGroupImage() != null && !loaded.getGroupImage().isEmpty()) {
                        Glide.with(this)
                                .load(loaded.getGroupImage())
                                .placeholder(R.drawable.ic_group_avatar)
                                .into(imgUser);
                    }

                    isCurrentMember = loaded.getMembers() != null
                            && loaded.getMembers().contains(myUid);

                    int count = loaded.getMembersCount();

                    tvStatus.setText(isCurrentMember
                            ? count + (count == 1 ? " member" : " members")
                            : "You are no longer a member");

                    adapter.setGroupMemberCount(count);

                    layoutInput.setVisibility(isCurrentMember ? View.VISIBLE : View.GONE);
                    tvNotMember.setVisibility(isCurrentMember ? View.GONE : View.VISIBLE);

                    // Messages are subscribed to once (the group document fires for every new message)
                    if (groupMessagesListener == null) {
                        listenToGroupMessages();
                    } else {
                        rebuildGroupList();
                    }
                });
    }

    private void listenToGroupMessages() {

        deletedListener = firestore.collection("DeletedGroupMessages")
                .document(myUid)
                .collection("Groups")
                .document(groupId)
                .collection("Messages")
                .addSnapshotListener((snapshot, error) -> {

                    if (snapshot == null) return;

                    deletedIds.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        deletedIds.add(doc.getId());
                    }

                    rebuildGroupList();
                });

        groupMessagesListener = groupMessagesRef()
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {

                    if (snapshot == null) return;

                    rawGroupMessages.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {

                        Message message = doc.toObject(Message.class);

                        if (message == null) continue;

                        message.setMessageId(doc.getId());
                        message.setPending(doc.getMetadata().hasPendingWrites());

                        rawGroupMessages.add(message);
                    }

                    rebuildGroupList();
                    markGroupMessagesDeliveredAndSeen();
                });
    }

    private CollectionReference groupMessagesRef() {
        return firestore.collection("GroupMessages")
                .document(groupId)
                .collection("Messages");
    }

    /** Only messages sent while I was a member are visible; "delete for me" ones are hidden. */
    private void rebuildGroupList() {

        List<Message> list = new ArrayList<>();

        for (Message message : rawGroupMessages) {

            if (deletedIds.contains(message.getMessageId())) continue;

            if (isVisibleToMe(message.getTimestamp())) {
                list.add(message);
            }
        }

        applyList(list);
    }

    private boolean isVisibleToMe(long timestamp) {

        if (group == null) return false;

        Map<String, List<MembershipPeriod>> history = group.getMemberHistory();

        List<MembershipPeriod> periods = history == null ? null : history.get(myUid);

        // Groups created before membership history existed: members see everything
        if (periods == null || periods.isEmpty()) {
            return isCurrentMember;
        }

        for (MembershipPeriod period : periods) {

            boolean afterJoin = timestamp >= period.getJoinedAt();
            boolean beforeLeave = period.getLeftAt() == null || timestamp <= period.getLeftAt();

            if (afterJoin && beforeLeave) return true;
        }

        return false;
    }

    /** Writes only for messages that actually still need the flag (not every message every time). */
    private void markGroupMessagesDeliveredAndSeen() {

        if (!isCurrentMember) return;

        WriteBatch batch = firestore.batch();
        int count = 0;

        for (Message message : messageList) {

            if (myUid.equals(message.getSenderId()) || message.isPending()
                    || message.getMessageId() == null || message.getMessageId().isEmpty()
                    || message.getMessageId().startsWith("local_")) {
                continue;
            }

            boolean delivered = message.getDeliveredTo() != null
                    && Boolean.TRUE.equals(message.getDeliveredTo().get(myUid));
            boolean seen = message.getSeenBy() != null
                    && Boolean.TRUE.equals(message.getSeenBy().get(myUid));

            if (delivered && seen) continue;
            if (count >= 400) break;

            DocumentReference ref = groupMessagesRef().document(message.getMessageId());

            if (!delivered) {
                batch.update(ref, FieldPath.of("deliveredTo", myUid), true);
            }

            if (!seen) {
                batch.update(ref, FieldPath.of("seenBy", myUid), true);
            }

            count++;
        }

        if (count > 0) {
            batch.commit();
        }
    }

    // =====================================================================
    // List handling
    // =====================================================================

    private void sortByTime(List<Message> list) {
        Collections.sort(list, (a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
    }

    private boolean isNearBottom() {

        RecyclerView.LayoutManager manager = recyclerMessages.getLayoutManager();

        if (!(manager instanceof LinearLayoutManager) || adapter.getItemCount() == 0) {
            return true;
        }

        int last = ((LinearLayoutManager) manager).findLastVisibleItemPosition();

        return last >= adapter.getItemCount() - 3;
    }

    private void scrollToBottom() {
        if (adapter.getItemCount() > 0) {
            recyclerMessages.scrollToPosition(adapter.getItemCount() - 1);
        }
    }

    private void applyList(List<Message> loaded) {

        List<Message> newList = new ArrayList<>(loaded);
        newList.addAll(uploadingMessages);

        boolean wasAtBottom = isNearBottom();
        int oldSize = messageList.size();

        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(
                new MessageDiffCallback(new ArrayList<>(messageList), newList));

        messageList.clear();
        messageList.addAll(newList);

        adapter.rebuildMessageMap();

        diff.dispatchUpdatesTo(adapter);

        if (newList.size() > oldSize) {

            Message last = newList.get(newList.size() - 1);

            boolean mine = myUid.equals(last.getSenderId());

            if (wasAtBottom || mine || oldSize == 0) {
                scrollToBottom();
            }
        }
    }

    // =====================================================================
    // Sending
    // =====================================================================

    private Message newMessage(String type) {

        Message message = new Message();

        message.setSenderId(myUid);
        message.setSenderName(myName);
        message.setTimestamp(System.currentTimeMillis());
        message.setType(type);
        message.setMessage("");

        if (isGroup) {

            message.setGroupId(groupId);

            Map<String, Boolean> flags = new HashMap<>();
            flags.put(myUid, true);

            message.setDeliveredTo(new HashMap<>(flags));
            message.setSeenBy(new HashMap<>(flags));

        } else {
            message.setReceiverId(receiverId);
        }

        return message;
    }

    /** Copies the reply target into the message (works for text, image AND voice messages). */
    private void applyReply(Message message) {

        if (replyingMessage == null) return;

        message.setReplyMessageId(replyingMessage.getMessageId());
        message.setReplySender(replySenderName(replyingMessage));
        message.setReplyType(replyingMessage.getType() == null ? "text" : replyingMessage.getType());

        if ("image".equals(replyingMessage.getType())) {
            message.setReplyMessage("Photo");
            message.setReplyImageUrl(replyingMessage.getImageUrl());
        } else if ("voice".equals(replyingMessage.getType())) {
            message.setReplyMessage("🎤 Voice message");
        } else {
            message.setReplyMessage(replyingMessage.getMessage());
        }

        clearReply();
    }

    private String replySenderName(Message target) {

        if (myUid.equals(target.getSenderId())) return "You";

        if (target.getSenderName() != null && !target.getSenderName().isEmpty()) {
            return target.getSenderName();
        }

        return chatTitle;
    }

    private void sendTextMessage() {

        if (isGroup && !isCurrentMember) return;

        String text = etMessage.getText().toString().trim();

        if (text.isEmpty()) return;

        Message message = newMessage("text");
        message.setMessage(text);

        applyReply(message);

        // Clear straight away: Firestore queues the write while offline, and waiting for the
        // server used to leave the text in the box so it could be sent twice.
        etMessage.setText("");

        dispatch(message, text);
    }

    /** Single place that stores a message and updates the conversation summary. */
    private void dispatch(Message message, String preview) {

        DocumentReference ref = isGroup
                ? groupMessagesRef().document()
                : firestore.collection("Messages").document();

        message.setMessageId(ref.getId());

        ref.set(message).addOnFailureListener(e ->
                Toast.makeText(this, "Message not sent: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        if (isGroup) {

            Map<String, Object> updates = new HashMap<>();
            updates.put("lastMessage", (myName.isEmpty() ? "" : myName + ": ") + preview);
            updates.put("lastMessageTime", message.getTimestamp());

            firestore.collection("Groups").document(groupId).update(updates);

        } else {
            updateChatSummary(message, preview);
        }
    }

    /**
     * Updates the chat-list entry. Unread counters are stored per user in an "unread" map and
     * changed with an atomic increment (no read-modify-write, no swapped counters).
     */
    private void updateChatSummary(Message message, String preview) {

        Map<String, Object> unread = new HashMap<>();
        unread.put(receiverId, FieldValue.increment(1));
        unread.put(myUid, 0);

        Map<String, Object> chat = new HashMap<>();
        chat.put("senderId", myUid);
        chat.put("receiverId", receiverId);
        chat.put("senderName", myName);
        chat.put("senderImage", myImage);
        chat.put("receiverName", chatTitle);
        chat.put("receiverImage", chatImage);
        chat.put("lastMessage", preview);
        chat.put("lastTimestamp", message.getTimestamp());
        chat.put("unread", unread);

        firestore.collection("Chats")
                .document(ChatHelper.getChatId(myUid, receiverId))
                .set(chat, SetOptions.merge());
    }

    // ---------------------------------------------------------------------
    // Images
    // ---------------------------------------------------------------------

    private void sendImage(Uri uri) {

        if (isGroup && !isCurrentMember) return;

        // Temporary bubble while uploading
        Message temp = newMessage("image");
        temp.setMessageId("local_" + System.nanoTime());
        temp.setUploading(true);
        temp.setLocalImageUri(uri);

        uploadingMessages.add(temp);
        refreshAfterUploadChange();

        final Message replyTarget = replyingMessage;
        clearReply();

        CloudinaryUploader.uploadImage(this, uri, new CloudinaryUploader.UploadCallback() {

            @Override
            public void onSuccess(String url) {

                uploadingMessages.remove(temp);

                Message message = newMessage("image");
                message.setImageUrl(url);

                replyingMessage = replyTarget;
                applyReply(message);

                refreshAfterUploadChange();

                dispatch(message, "📷 Photo");
            }

            @Override
            public void onFailure(Exception e) {

                uploadingMessages.remove(temp);
                refreshAfterUploadChange();

                Toast.makeText(ChatActivity.this,
                        e.getMessage() == null ? "Image upload failed" : e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void refreshAfterUploadChange() {
        if (isGroup) {
            rebuildGroupList();
        } else {
            rebuildPrivateList();
        }
    }

    // ---------------------------------------------------------------------
    // Voice messages
    // ---------------------------------------------------------------------

    private boolean hasMicPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void setupMicButton() {

        final float cancelDistance = 120 * getResources().getDisplayMetrics().density;

        btnMic.setOnTouchListener((v, event) -> {

            if (isGroup && !isCurrentMember) return true;

            switch (event.getActionMasked()) {

                case MotionEvent.ACTION_DOWN:

                    if (!hasMicPermission()) {
                        micPermission.launch(Manifest.permission.RECORD_AUDIO);
                        return true;
                    }

                    touchStartX = event.getRawX();
                    startVoiceRecording();

                    if (v.getParent() != null) {
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                    }

                    return true;

                case MotionEvent.ACTION_MOVE:

                    if (isRecordingVoice) {

                        float diff = touchStartX - event.getRawX();

                        if (diff > 0) {
                            tvSlideCancel.setTranslationX(-diff / 3f);
                        }

                        tvSlideCancel.setText(diff > cancelDistance
                                ? "Release to cancel" : "◀ Slide to cancel");
                    }

                    return true;

                case MotionEvent.ACTION_UP:

                    if (isRecordingVoice) {
                        boolean cancel = (touchStartX - event.getRawX()) > cancelDistance;
                        stopVoiceRecording(cancel);
                    }

                    return true;

                case MotionEvent.ACTION_CANCEL:

                    if (isRecordingVoice) {
                        stopVoiceRecording(true);
                    }

                    return true;

                default:
                    return false;
            }
        });
    }

    private void startVoiceRecording() {

        if (isRecordingVoice) return;

        try {

            voiceFile = new File(getCacheDir(), "voice_" + System.currentTimeMillis() + ".m4a");

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(96000);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setOutputFile(voiceFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecordingVoice = true;
            voiceStartTime = System.currentTimeMillis();
            amplitudes.clear();

            tvRecordingTime.setText("0:00");
            tvSlideCancel.setTranslationX(0);
            tvSlideCancel.setText("◀ Slide to cancel");

            layoutRecording.setVisibility(View.VISIBLE);

            recordingHandler.post(recordingRunnable);

        } catch (Exception e) {

            isRecordingVoice = false;
            releaseRecorder();

            Toast.makeText(this, "Couldn't start recording", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopVoiceRecording(boolean cancel) {

        if (!isRecordingVoice) return;

        isRecordingVoice = false;

        recordingHandler.removeCallbacks(recordingRunnable);

        layoutRecording.setVisibility(View.GONE);
        tvSlideCancel.setTranslationX(0);

        long duration = System.currentTimeMillis() - voiceStartTime;

        boolean stoppedCleanly = true;

        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
            }
        } catch (RuntimeException e) {
            // thrown when the recording was too short to contain any audio
            stoppedCleanly = false;
        }

        releaseRecorder();

        File file = voiceFile;
        voiceFile = null;

        if (file == null) return;

        if (cancel || !stoppedCleanly || duration < MIN_VOICE_MS) {

            file.delete();

            if (!cancel) {
                Toast.makeText(this, "Hold the button to record a voice message",
                        Toast.LENGTH_SHORT).show();
            }

            return;
        }

        sendVoiceMessage(file, duration, buildWaveform());
    }

    /** Real waveform from the recorded loudness (the old one was fake / based on file size). */
    private List<Integer> buildWaveform() {

        final int bars = 48;

        List<Integer> waveform = new ArrayList<>();

        if (amplitudes.isEmpty()) {
            for (int i = 0; i < bars; i++) waveform.add(20);
            return waveform;
        }

        int max = 1;

        for (int amplitude : amplitudes) {
            max = Math.max(max, amplitude);
        }

        for (int i = 0; i < bars; i++) {

            int from = i * amplitudes.size() / bars;
            int to = Math.max(from + 1, (i + 1) * amplitudes.size() / bars);

            int peak = 0;

            for (int j = from; j < Math.min(to, amplitudes.size()); j++) {
                peak = Math.max(peak, amplitudes.get(j));
            }

            double normalized = Math.sqrt((double) peak / max);

            waveform.add(Math.max(8, (int) (normalized * 100)));
        }

        return waveform;
    }

    private void sendVoiceMessage(File file, long duration, List<Integer> waveform) {

        Toast.makeText(this, "Sending voice message...", Toast.LENGTH_SHORT).show();

        final Message replyTarget = replyingMessage;
        clearReply();

        CloudinaryUploader.uploadAudio(file, new CloudinaryUploader.UploadCallback() {

            @Override
            public void onSuccess(String url) {

                file.delete();

                Message message = newMessage("voice");
                message.setVoiceUrl(url);
                message.setVoiceDuration(duration);
                message.setWaveform(waveform);

                replyingMessage = replyTarget;
                applyReply(message);

                dispatch(message, "🎤 Voice message");
            }

            @Override
            public void onFailure(Exception e) {

                file.delete();

                Toast.makeText(ChatActivity.this,
                        "Voice message failed: " + (e.getMessage() == null ? "" : e.getMessage()),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void releaseRecorder() {

        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
            } catch (Exception ignored) {
            }
            mediaRecorder = null;
        }
    }

    private String formatDuration(long ms) {
        long seconds = ms / 1000;
        return String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60);
    }

    // ---------------------------------------------------------------------
    // Typing indicator (private chats) - written only when the state CHANGES
    // ---------------------------------------------------------------------

    private void onTyping(boolean hasText) {

        typingHandler.removeCallbacks(stopTypingRunnable);

        if (!hasText) {
            sendTypingStopped();
            return;
        }

        if (!typingSent) {
            typingSent = true;
            firestore.collection("Users").document(myUid).update("typingTo", receiverId);
        }

        typingHandler.postDelayed(stopTypingRunnable, TYPING_TIMEOUT_MS);
    }

    private void sendTypingStopped() {

        typingHandler.removeCallbacks(stopTypingRunnable);

        if (typingSent && firestore != null) {
            typingSent = false;
            firestore.collection("Users").document(myUid).update("typingTo", "");
        }
    }

    // =====================================================================
    // Reply / reactions / delete
    // =====================================================================

    private void setReplyTarget(Message message) {

        if (message == null || message.isDeleted() || message.isUploading()) return;

        if (isGroup && !isCurrentMember) return;

        replyingMessage = message;

        tvReplySender.setText("Replying to " + replySenderName(message));

        if ("image".equals(message.getType())) {
            tvReplyMessage.setText("📷 Photo");
        } else if ("voice".equals(message.getType())) {
            tvReplyMessage.setText("🎤 Voice message");
        } else {
            tvReplyMessage.setText(message.getMessage());
        }

        layoutReply.setVisibility(View.VISIBLE);

        etMessage.requestFocus();
    }

    private void clearReply() {
        replyingMessage = null;
        layoutReply.setVisibility(View.GONE);
    }

    private void showMessageMenu(View anchor, Message message) {

        if (message.isUploading()) return;

        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenuInflater().inflate(R.menu.message_menu, menu.getMenu());

        boolean mine = myUid.equals(message.getSenderId());
        boolean deleted = message.isDeleted();
        boolean isText = message.getType() == null || "text".equals(message.getType());
        boolean canInteract = !deleted && !(isGroup && !isCurrentMember);

        menu.getMenu().findItem(R.id.menu_react).setVisible(canInteract);
        menu.getMenu().findItem(R.id.menu_reply).setVisible(canInteract);
        menu.getMenu().findItem(R.id.menu_copy).setVisible(isText && !deleted);
        menu.getMenu().findItem(R.id.menu_delete_everyone).setVisible(mine && !deleted);

        menu.setOnMenuItemClickListener(item -> {

            int id = item.getItemId();

            if (id == R.id.menu_react) {
                showReactionPopup(anchor, message);
            } else if (id == R.id.menu_reply) {
                setReplyTarget(message);
            } else if (id == R.id.menu_copy) {
                copyToClipboard(message.getMessage());
            } else if (id == R.id.menu_delete_me) {
                deleteForMe(message);
            } else if (id == R.id.menu_delete_everyone) {
                deleteForEveryone(message);
            }

            return true;
        });

        menu.show();
    }

    private void copyToClipboard(String text) {

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        if (clipboard != null && text != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("message", text));
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
        }
    }

    private void showReactionPopup(View anchor, Message message) {

        View popupView = LayoutInflater.from(this).inflate(R.layout.reaction_popup, null);

        PopupWindow popup = new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);

        popup.setElevation(12f);

        int[] ids = {R.id.reactionHeart, R.id.reactionLaugh, R.id.reactionWow,
                R.id.reactionSad, R.id.reactionAngry, R.id.reactionLike, R.id.reactionDislike};

        for (int id : ids) {

            TextView emoji = popupView.findViewById(id);

            emoji.setOnClickListener(v -> {
                toggleReaction(message, emoji.getText().toString());
                popup.dismiss();
            });
        }

        popupView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        // show it just above the bubble
        popup.showAsDropDown(anchor, 0, -(anchor.getHeight() + popupView.getMeasuredHeight()));
    }

    /** Tapping the same reaction again removes it. */
    private void toggleReaction(Message message, String emoji) {

        if (message.getMessageId() == null || message.getMessageId().isEmpty()) return;

        String current = message.getReactions() == null
                ? null : message.getReactions().get(myUid);

        Object value = emoji.equals(current) ? FieldValue.delete() : emoji;

        messageRef(message).update(FieldPath.of("reactions", myUid), value)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Couldn't add reaction", Toast.LENGTH_SHORT).show());
    }

    private DocumentReference messageRef(Message message) {
        return isGroup
                ? groupMessagesRef().document(message.getMessageId())
                : firestore.collection("Messages").document(message.getMessageId());
    }

    private void deleteForMe(Message message) {

        String id = message.getMessageId();

        if (id == null || id.isEmpty()) return;

        Map<String, Object> data = new HashMap<>();
        data.put("deletedAt", FieldValue.serverTimestamp());

        DocumentReference ref;

        if (isGroup) {
            data.put("groupId", groupId);
            data.put("messageId", id);

            ref = firestore.collection("DeletedGroupMessages")
                    .document(myUid)
                    .collection("Groups")
                    .document(groupId)
                    .collection("Messages")
                    .document(id);
        } else {
            ref = firestore.collection("DeletedMessages")
                    .document(myUid)
                    .collection("Messages")
                    .document(id);
        }

        ref.set(data).addOnFailureListener(e ->
                Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show());
    }

    private void deleteForEveryone(Message message) {

        if (!myUid.equals(message.getSenderId())) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("deleted", true);
        updates.put("message", "");
        updates.put("imageUrl", "");
        updates.put("voiceUrl", "");
        updates.put("reactions", FieldValue.delete());

        messageRef(message).update(updates).addOnFailureListener(e ->
                Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show());
    }
}
