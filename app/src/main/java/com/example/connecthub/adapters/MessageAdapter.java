package com.example.connecthub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connecthub.R;
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

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
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

            sentHolder.tvTime.setText(time);

            if ("image".equals(message.getType())) {

                sentHolder.imgMessage.setVisibility(View.VISIBLE);
                sentHolder.tvMessage.setVisibility(View.GONE);

                Glide.with(sentHolder.itemView.getContext())
                        .load(message.getImageUrl())
                        .into(sentHolder.imgMessage);

            } else {

                sentHolder.imgMessage.setVisibility(View.GONE);
                sentHolder.tvMessage.setVisibility(View.VISIBLE);
                sentHolder.tvMessage.setText(message.getMessage());

            }

            if (message.isSeen()) {

                sentHolder.tvSeen.setText("✓✓");
                sentHolder.tvSeen.setTextColor(0xFF2196F3);

            } else {

                sentHolder.tvSeen.setText("✓");
                sentHolder.tvSeen.setTextColor(0xFF888888);

            }

        } else {

            ReceivedViewHolder receivedHolder = (ReceivedViewHolder) holder;

            receivedHolder.tvTime.setText(time);

            if ("image".equals(message.getType())) {

                receivedHolder.imgMessage.setVisibility(View.VISIBLE);
                receivedHolder.tvMessage.setVisibility(View.GONE);

                Glide.with(receivedHolder.itemView.getContext())
                        .load(message.getImageUrl())
                        .into(receivedHolder.imgMessage);

            } else {

                receivedHolder.imgMessage.setVisibility(View.GONE);
                receivedHolder.tvMessage.setVisibility(View.VISIBLE);
                receivedHolder.tvMessage.setText(message.getMessage());

            }

        }
    }


    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {

        ImageView imgMessage;

        TextView tvMessage;
        TextView tvTime;
        TextView tvSeen;
        TextView tvUploading;

        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvSeen = itemView.findViewById(R.id.tvSeen);
            imgMessage = itemView.findViewById(R.id.imgMessage);
            tvUploading = itemView.findViewById(R.id.tvUploading);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {

        ImageView imgMessage;

        TextView tvMessage;
        TextView tvTime;
        TextView tvSeen;
        TextView tvUploading;

        public ReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            imgMessage = itemView.findViewById(R.id.imgMessage);
            tvUploading = itemView.findViewById(R.id.tvUploading);
        }
    }
}
