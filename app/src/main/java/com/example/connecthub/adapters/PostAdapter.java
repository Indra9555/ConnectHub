package com.example.connecthub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.connecthub.helpers.NotificationHelper;
import com.bumptech.glide.Glide;
import com.example.connecthub.R;
import com.example.connecthub.activities.ProfileActivity;
import com.example.connecthub.helpers.TimeUtils;
import com.example.connecthub.models.Post;
import com.example.connecthub.helpers.LikeHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import android.content.Intent;
import com.example.connecthub.activities.CommentActivity;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private final List<Post> postList;


    public PostAdapter(List<Post> postList) {
        this.postList = postList;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);

        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull PostViewHolder holder,
            int position) {

        Post post = postList.get(position);
        holder.likeCount.setText(
                "❤️ " + post.getLikesCount() + " Likes"
        );


        holder.commentCount.setText(
                "💬 " + post.getCommentsCount() + " Comments"
        );
        holder.userImage.setOnClickListener(v ->
                openProfile(holder, post));

        holder.name.setOnClickListener(v ->
                openProfile(holder, post));

        holder.username.setOnClickListener(v ->
                openProfile(holder, post));

        // Name
        holder.name.setText(post.getName());

        // Username
        holder.username.setText("@" + post.getUsername());

        // Post content
        holder.content.setText(post.getContent());
        if(post.getPostImage() != null &&
                !post.getPostImage().isEmpty()){


            holder.postImage.setVisibility(View.VISIBLE);


            Glide.with(holder.itemView.getContext())
                    .load(post.getPostImage())
                    .centerCrop()
                    .into(holder.postImage);


        }
        else{


            holder.postImage.setVisibility(View.GONE);


        }

        // Time
        holder.time.setText(
                TimeUtils.getTimeAgo(post.getTimestamp())
        );

        // Profile Image
        if (post.getUserImage() != null &&
                !post.getUserImage().isEmpty()) {

            Glide.with(holder.itemView.getContext())
                    .load(post.getUserImage())
                    .placeholder(R.drawable.ic_person)
                    .into(holder.userImage);

        } else {

            holder.userImage.setImageResource(R.drawable.ic_person);

        }

        // Like button
        LikeHelper.isLiked(post.getPostId(), liked -> {

            if (liked) {

                holder.like.setText("❤️ Liked (" + post.getLikesCount() + ")");

            } else {

                holder.like.setText("🤍 Like (" + post.getLikesCount() + ")");

            }

        });
        holder.like.setOnClickListener(v -> {

            LikeHelper.isLiked(post.getPostId(), liked -> {

                LikeHelper.toggleLike(post.getPostId());

                int newCount;

                if (!liked) {

                    String currentUid =
                            FirebaseAuth.getInstance()
                                    .getCurrentUser()
                                    .getUid();

                    NotificationHelper.sendNotification(
                            currentUid,
                            post.getUid(),
                            "like",
                            post.getPostId(),
                            "liked your post"
                    );

                    newCount = post.getLikesCount() + 1;

                    holder.like.setText(
                            "❤️ Liked (" + newCount + ")"
                    );

                } else {

                    newCount = Math.max(0, post.getLikesCount() - 1);

                    holder.like.setText(
                            "🤍 Like (" + newCount + ")"
                    );

                }

                holder.likeCount.setText(
                        "❤️ " + newCount + " Likes"
                );

                post.setLikesCount(newCount);

            });

        });

        // Comment button
        holder.comment.setOnClickListener(v -> {

            Intent intent = new Intent(
                    holder.itemView.getContext(),
                    CommentActivity.class
            );

            intent.putExtra(
                    "postId",
                    post.getPostId()
            );

            holder.itemView.getContext().startActivity(intent);

        });

        // Share button
        holder.share.setOnClickListener(v -> {

            Intent shareIntent = new Intent(
                    Intent.ACTION_SEND
            );

            shareIntent.setType("text/plain");

            shareIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    post.getContent()
            );

            holder.itemView.getContext()
                    .startActivity(
                            Intent.createChooser(
                                    shareIntent,
                                    "Share post"
                            )
                    );

        });

    }
    private void openProfile(PostViewHolder holder, Post post) {

        Intent intent = new Intent(
                holder.itemView.getContext(),
                ProfileActivity.class
        );

        intent.putExtra("uid", post.getUid());

        holder.itemView.getContext().startActivity(intent);

    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {

        ImageView userImage;

        TextView name;
        TextView username;
        TextView content;
        TextView time;

        MaterialButton like;
        MaterialButton comment;
        MaterialButton share;
        TextView likeCount;
        TextView commentCount;
        ImageView postImage;

        public PostViewHolder(@NonNull View itemView) {

            super(itemView);
            postImage =
                    itemView.findViewById(R.id.postImage);

            likeCount =
                    itemView.findViewById(R.id.postLikeCount);


            commentCount =
                    itemView.findViewById(R.id.postCommentCount);

            userImage = itemView.findViewById(R.id.postUserImage);

            name = itemView.findViewById(R.id.postName);

            username = itemView.findViewById(R.id.postUsername);

            content = itemView.findViewById(R.id.postContent);

            time = itemView.findViewById(R.id.postTime);

            like = itemView.findViewById(R.id.postLike);

            comment = itemView.findViewById(R.id.postComment);

            share = itemView.findViewById(R.id.postShare);
        }
    }
}