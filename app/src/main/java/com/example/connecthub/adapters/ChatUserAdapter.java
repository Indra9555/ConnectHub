package com.example.connecthub.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.connecthub.R;
import com.example.connecthub.activities.ChatActivity;
import com.example.connecthub.helpers.TimeUtils;
import com.example.connecthub.models.ChatUser;

import java.util.List;

public class ChatUserAdapter extends RecyclerView.Adapter<ChatUserAdapter.ChatViewHolder> {

    private final List<ChatUser> chatUserList;

    public ChatUserAdapter(List<ChatUser> chatUserList) {
        this.chatUserList = chatUserList;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_user, parent, false);

        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ChatViewHolder holder,
            int position) {

        ChatUser user = chatUserList.get(position);

        holder.tvName.setText(user.getName());

        holder.tvLastMessage.setText(user.getLastMessage());

        holder.tvTime.setText(
                TimeUtils.getTimeAgo(user.getLastTimestamp())
        );
        if (user.getUnreadCount() > 0) {

            holder.tvUnread.setVisibility(View.VISIBLE);

            holder.tvUnread.setText(
                    String.valueOf(user.getUnreadCount())
            );

        } else {

            holder.tvUnread.setVisibility(View.GONE);

        }

        Glide.with(holder.itemView.getContext())
                .load(user.getImage())
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(holder.imgProfile);

        holder.itemView.setOnClickListener(v -> {

            Intent intent = new Intent(
                    holder.itemView.getContext(),
                    ChatActivity.class
            );

            intent.putExtra("uid", user.getUid());

            holder.itemView.getContext().startActivity(intent);

        });

    }

    @Override
    public int getItemCount() {
        return chatUserList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvUnread;

        ImageView imgProfile;

        TextView tvName;

        TextView tvLastMessage;

        TextView tvTime;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);

            imgProfile = itemView.findViewById(R.id.imgProfile);
            tvName = itemView.findViewById(R.id.tvName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvUnread = itemView.findViewById(R.id.tvUnread);
        }
    }
}