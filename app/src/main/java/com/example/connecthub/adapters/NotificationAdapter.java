package com.example.connecthub.adapters;

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
import com.example.connecthub.models.Notification;
import com.example.connecthub.models.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import android.content.Intent;
import com.example.connecthub.activities.ProfileActivity;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final List<Notification> notificationList;

    public NotificationAdapter(List<Notification> notificationList) {
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {


        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);


        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull NotificationViewHolder holder,
            int position) {

        Notification notification = notificationList.get(position);

        if ("comment".equals(notification.getType())) {

            holder.tvNotification.setText(
                    "commented:\n\"" + notification.getMessage().replace("commented: ", "") + "\""
            );

        } else if ("like".equals(notification.getType())) {

            holder.tvNotification.setText("liked your post ❤️");

        } else {

            holder.tvNotification.setText(notification.getMessage());

        }
        holder.tvTime.setText(getTimeAgo(notification.getTimestamp()));
        holder.itemView.setOnClickListener(v -> {

            if ("follow".equals(notification.getType())) {

                Intent intent = new Intent(
                        holder.itemView.getContext(),
                        ProfileActivity.class
                );

                intent.putExtra(
                        "uid",
                        notification.getSenderId()
                );

                holder.itemView.getContext().startActivity(intent);

            }

            else if ("like".equals(notification.getType())
                    || "comment".equals(notification.getType())) {

                Intent intent = new Intent(
                        holder.itemView.getContext(),
                        CommentActivity.class
                );

                intent.putExtra(
                        "postId",
                        notification.getPostId()
                );

                holder.itemView.getContext().startActivity(intent);

            }

        });

        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(notification.getSenderId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    User user = documentSnapshot.toObject(User.class);

                    if (user != null) {

                        holder.tvName.setText(user.getName());

                        Glide.with(holder.itemView.getContext())
                                .load(user.getImage())
                                .placeholder(R.drawable.ic_profile)
                                .error(R.drawable.ic_person)
                                .into(holder.imgProfile);
                    }

                });

    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {

        ImageView imgProfile;
        TextView tvName;
        TextView tvNotification;
        TextView tvTime;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);

            imgProfile = itemView.findViewById(R.id.imgProfile);
            tvName = itemView.findViewById(R.id.tvName);
            tvNotification = itemView.findViewById(R.id.tvNotification);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
    private String getTimeAgo(long time) {

        long now = System.currentTimeMillis();
        long diff = now - time;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + " min ago";
        } else if (hours < 24) {
            return hours + " hr ago";
        } else if (days < 7) {
            return days + " day ago";
        } else {
            return (days / 7) + " week ago";
        }
    }
}