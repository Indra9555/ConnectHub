package com.example.connecthub.adapters;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connecthub.R;
import com.example.connecthub.activities.ImageViewerActivity;
import com.example.connecthub.models.Message;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.net.Uri;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int SENT = 1;
    private static final int RECEIVED = 2;

    private final List<Message> messageList;
    private final java.util.HashMap<String, Message> messageMap = new java.util.HashMap<>();

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }
    public interface OnMessageLongClickListener {
        void onMessageLongClick(View anchor, Message message);
    }
    private OnMessageLongClickListener listener;

    public void setOnMessageLongClickListener(OnMessageLongClickListener listener) {
        this.listener = listener;
    }
    private OnReplyClickListener replyListener;
    public void setOnReplyClickListener(OnReplyClickListener listener) {
        this.replyListener = listener;
    }
    public interface OnReplyClickListener {
        void onReplyClick(String messageId);
    }
    @Override
    public int getItemViewType(int position) {

        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (messageList.get(position).getSenderId().equals(currentUid)) {
            return SENT;
        }

        return RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        if (viewType == SENT) {

            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);

            return new SentViewHolder(view);

        } else {

            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);

            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position) {

        Message message = messageList.get(position);



        String time = new SimpleDateFormat(
                "hh:mm a",
                Locale.getDefault()
        ).format(new Date(message.getTimestamp()));

        if (holder instanceof SentViewHolder) {

            SentViewHolder sentHolder = (SentViewHolder) holder;

            bindTime(sentHolder.tvTime, message);

            bindReplyPreview(
                    sentHolder.layoutReplyPreview,
                    sentHolder.imgReply,
                    sentHolder.tvReplySender,
                    sentHolder.tvReplyMessage,
                    message,
                    sentHolder.itemView
            );

            if ("image".equals(message.getType())) {

                bindImageMessage(
                        sentHolder.imgMessage,
                        sentHolder.tvMessage,
                        sentHolder.tvUploading,
                        message,
                        sentHolder.itemView
                );

            } else {

                sentHolder.imgMessage.setVisibility(View.GONE);

                bindTextMessage(
                        sentHolder.tvMessage,
                        message,
                        true
                );

            }

            bindSeen(sentHolder.tvSeen, message);

            View target = "image".equals(message.getType())
                    ? sentHolder.imgMessage
                    : sentHolder.tvMessage;

            target.setOnLongClickListener(v -> {

                if (listener != null) {
                    listener.onMessageLongClick(v, message);
                }

                return true;

            });
        } else {

            ReceivedViewHolder receivedHolder = (ReceivedViewHolder) holder;

            bindTime(receivedHolder.tvTime, message);

            bindReplyPreview(
                    receivedHolder.layoutReplyPreview,
                    receivedHolder.imgReply,
                    receivedHolder.tvReplySender,
                    receivedHolder.tvReplyMessage,
                    message,
                    receivedHolder.itemView
            );

            if ("image".equals(message.getType())) {

                bindImageMessage(
                        receivedHolder.imgMessage,
                        receivedHolder.tvMessage,
                        receivedHolder.tvUploading,
                        message,
                        receivedHolder.itemView
                );

            } else {

                receivedHolder.imgMessage.setVisibility(View.GONE);

                bindTextMessage(
                        receivedHolder.tvMessage,
                        message,
                        false
                );

            }

            View target = "image".equals(message.getType())
                    ? receivedHolder.imgMessage
                    : receivedHolder.tvMessage;

            target.setOnLongClickListener(v -> {

                if (listener != null) {
                    listener.onMessageLongClick(v, message);
                }

                return true;

            });

        }

    }
    public void rebuildMessageMap() {

        messageMap.clear();

        for (Message m : messageList) {

            messageMap.put(m.getMessageId(), m);
        }

    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }
    private void bindReplyPreview(
            LinearLayout layoutReplyPreview,
            ImageView imgReply,
            TextView tvReplySender,
            TextView tvReplyMessage,
            Message message,
            View itemView
    ) {

        if (message.getReplyMessageId() == null ||
                message.getReplyMessageId().isEmpty()) {

            layoutReplyPreview.setVisibility(View.GONE);
            return;
        }

        layoutReplyPreview.setVisibility(View.VISIBLE);

        tvReplySender.setText(message.getReplySender());

        Message replied = messageMap.get(message.getReplyMessageId());

        // Original message no longer exists
        if (replied == null) {

            imgReply.setVisibility(View.GONE);
            tvReplyMessage.setText("🚫 This message was deleted");
            return;
        }

        // Original message exists but is deleted
        if (replied.isDeleted()) {

            imgReply.setVisibility(View.GONE);
            tvReplyMessage.setText("🚫 This message was deleted");
            return;
        }

        // Original is an image
        if ("image".equals(replied.getType())) {

            imgReply.setVisibility(View.VISIBLE);

            Glide.with(itemView.getContext())
                    .load(replied.getImageUrl())
                    .into(imgReply);

            tvReplyMessage.setText("Photo");

        } else {

            imgReply.setVisibility(View.GONE);
            tvReplyMessage.setText(replied.getMessage());

        }

        layoutReplyPreview.setOnClickListener(v -> {

            if (replyListener != null) {
                replyListener.onReplyClick(message.getReplyMessageId());
            }

        });
    }
    private void bindTextMessage(TextView tvMessage, Message message, boolean isSender) {

        tvMessage.setVisibility(View.VISIBLE);

        if (message.isDeleted()) {

            if (isSender) {
                tvMessage.setText("🚫 You deleted this message");
            } else {
                tvMessage.setText("🚫 You deleted this message");
            }

        } else {

            tvMessage.setText(message.getMessage());

        }

    }
    private void bindImageMessage(
            ImageView imgMessage,
            TextView tvMessage,
            TextView tvUploading,
            Message message,
            View itemView
    ) {

        imgMessage.setVisibility(View.VISIBLE);
        tvMessage.setVisibility(View.GONE);

        if (message.isUploading()) {

            tvUploading.setVisibility(View.VISIBLE);

            Glide.with(itemView.getContext())
                    .load(message.getLocalImageUri())
                    .into(imgMessage);

        } else {

            tvUploading.setVisibility(View.GONE);

            Glide.with(itemView.getContext())
                    .load(message.getImageUrl())
                    .into(imgMessage);

        }

        imgMessage.setOnClickListener(v -> {

            if (message.isUploading()) return;

            Intent intent = new Intent(
                    itemView.getContext(),
                    ImageViewerActivity.class
            );

            intent.putExtra("image", message.getImageUrl());

            itemView.getContext().startActivity(intent);

        });

    }
    private void bindSeen(TextView tvSeen, Message message) {

        if (tvSeen == null) return;

        if (message.isSeen()) {

            tvSeen.setText("✓✓");
            tvSeen.setTextColor(0xFF2196F3);

        } else {

            tvSeen.setText("✓");
            tvSeen.setTextColor(0xFF888888);

        }

    }
    private void bindTime(TextView tvTime, Message message) {

        String time = new SimpleDateFormat(
                "hh:mm a",
                Locale.getDefault()
        ).format(new Date(message.getTimestamp()));

        tvTime.setText(time);

    }

    static class SentViewHolder extends RecyclerView.ViewHolder {

        ImageView imgMessage;

        TextView tvMessage;
        TextView tvTime;
        TextView tvSeen;
        TextView tvUploading;
        LinearLayout layoutReplyPreview;
        TextView tvReplySender;
        TextView tvReplyMessage;
        ImageView imgReply;
        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvSeen = itemView.findViewById(R.id.tvSeen);
            imgMessage = itemView.findViewById(R.id.imgMessage);
            tvUploading = itemView.findViewById(R.id.tvUploading);
            layoutReplyPreview = itemView.findViewById(R.id.layoutReplyPreview);
            tvReplySender = itemView.findViewById(R.id.tvReplySender);
            tvReplyMessage = itemView.findViewById(R.id.tvReplyMessage);
            imgReply = itemView.findViewById(R.id.imgReply);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {

        ImageView imgMessage;

        TextView tvMessage;
        TextView tvTime;
        TextView tvSeen;
        TextView tvUploading;
        LinearLayout layoutReplyPreview;
        TextView tvReplySender;
        TextView tvReplyMessage;
        ImageView imgReply;

        public ReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            imgMessage = itemView.findViewById(R.id.imgMessage);
            tvUploading = itemView.findViewById(R.id.tvUploading);
            layoutReplyPreview = itemView.findViewById(R.id.layoutReplyPreview);
            tvReplySender = itemView.findViewById(R.id.tvReplySender);
            tvReplyMessage = itemView.findViewById(R.id.tvReplyMessage);
            imgReply = itemView.findViewById(R.id.imgReply);
        }
    }
}
