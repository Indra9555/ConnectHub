package com.example.connecthub.adapters;

import android.content.Context;
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
import com.example.connecthub.activities.CommentActivity;
import com.example.connecthub.activities.ProfileActivity;
import com.example.connecthub.helpers.TimeUtils;
import com.example.connecthub.models.Notification;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationAdapter
        extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final List<Notification> notificationList;

    /** senderId -> {name, image}. One lookup per sender instead of one per bind. */
    private final Map<String, String[]> senderCache = new HashMap<>();

    public NotificationAdapter(List<Notification> notificationList) {
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {

        Notification notification = notificationList.get(position);
        Context context = holder.itemView.getContext();

        String type = notification.getType();
        String message = notification.getMessage() == null ? "" : notification.getMessage();

        if ("comment".equals(type)) {
            holder.tvNotification.setText(
                    "commented: \"" + message.replace("commented: ", "") + "\"");
        } else if ("like".equals(type)) {
            holder.tvNotification.setText("liked your post");
        } else if ("follow".equals(type)) {
            holder.tvNotification.setText("started following you");
        } else {
            holder.tvNotification.setText(message);
        }

        holder.tvTime.setText(TimeUtils.getTimeAgo(notification.getTimestamp()));

        // Unread notifications get a tinted background
        int highlight = MaterialColors.getColor(holder.itemView,
                com.google.android.material.R.attr.colorPrimaryContainer);
        int normal = MaterialColors.getColor(holder.itemView,
                com.google.android.material.R.attr.colorSurface);

        holder.card.setCardBackgroundColor(notification.isSeen() ? normal : highlight);

        holder.itemView.setOnClickListener(v -> {

            if ("follow".equals(type)) {

                Intent intent = new Intent(context, ProfileActivity.class);
                intent.putExtra("uid", notification.getSenderId());
                context.startActivity(intent);

            } else if (("like".equals(type) || "comment".equals(type))
                    && notification.getPostId() != null
                    && !notification.getPostId().isEmpty()) {

                Intent intent = new Intent(context, CommentActivity.class);
                intent.putExtra("postId", notification.getPostId());
                context.startActivity(intent);
            }
        });

        bindSender(holder, notification.getSenderId());
    }

    private void bindSender(NotificationViewHolder holder, String senderId) {

        holder.boundSenderId = senderId;

        if (senderId == null || senderId.isEmpty()) {
            holder.tvName.setText("Someone");
            holder.imgProfile.setImageResource(R.drawable.ic_person);
            return;
        }

        String[] cached = senderCache.get(senderId);

        if (cached != null) {
            applySender(holder, cached);
            return;
        }

        holder.tvName.setText("");
        holder.imgProfile.setImageResource(R.drawable.ic_person);

        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(senderId)
                .get()
                .addOnSuccessListener(doc -> {

                    String[] info = new String[]{
                            doc.exists() && doc.getString("name") != null
                                    ? doc.getString("name") : "Deleted user",
                            doc.getString("image")
                    };

                    senderCache.put(senderId, info);

                    // The holder may have been recycled for a different notification
                    if (senderId.equals(holder.boundSenderId)) {
                        applySender(holder, info);
                    }
                });
    }

    private void applySender(NotificationViewHolder holder, String[] info) {

        holder.tvName.setText(info[0]);

        if (info[1] != null && !info[1].isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(info[1])
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(holder.imgProfile);
        } else {
            Glide.with(holder.itemView.getContext()).clear(holder.imgProfile);
            holder.imgProfile.setImageResource(R.drawable.ic_person);
        }
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {

        final MaterialCardView card;
        final ImageView imgProfile;
        final TextView tvName;
        final TextView tvNotification;
        final TextView tvTime;

        String boundSenderId;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cardNotification);
            imgProfile = itemView.findViewById(R.id.imgProfile);
            tvName = itemView.findViewById(R.id.tvName);
            tvNotification = itemView.findViewById(R.id.tvNotification);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}
