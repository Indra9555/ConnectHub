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
import com.example.connecthub.helpers.TimeUtils;
import com.example.connecthub.models.Comment;

import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final List<Comment> commentList;

    public CommentAdapter(List<Comment> commentList) {
        this.commentList = commentList;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {

        Comment comment = commentList.get(position);

        holder.name.setText(comment.getName() == null ? "" : comment.getName());
        holder.username.setText(comment.getUsername() == null ? "" : "@" + comment.getUsername());
        holder.comment.setText(comment.getComment());
        holder.time.setText(TimeUtils.getTimeAgo(comment.getTimestamp()));

        if (comment.getUserImage() != null && !comment.getUserImage().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(comment.getUserImage())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(holder.userImage);
        } else {
            Glide.with(holder.itemView.getContext()).clear(holder.userImage);
            holder.userImage.setImageResource(R.drawable.ic_person);
        }
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {

        final ImageView userImage;
        final TextView name;
        final TextView username;
        final TextView comment;
        final TextView time;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            userImage = itemView.findViewById(R.id.commentUserImage);
            name = itemView.findViewById(R.id.commentName);
            username = itemView.findViewById(R.id.commentUsername);
            comment = itemView.findViewById(R.id.commentText);
            time = itemView.findViewById(R.id.commentTime);
        }
    }
}
