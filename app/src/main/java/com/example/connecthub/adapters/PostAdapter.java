package com.example.connecthub.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.connecthub.R;
import com.example.connecthub.activities.CommentActivity;
import com.example.connecthub.activities.ImageViewerActivity;
import com.example.connecthub.activities.ProfileActivity;
import com.example.connecthub.helpers.LikeHelper;
import com.example.connecthub.helpers.PostHelper;
import com.example.connecthub.helpers.TimeUtils;
import com.example.connecthub.models.Post;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private static final int LIKE_RED = 0xFFE53935;

    private final List<Post> postList;

    /** postId -> liked by me. Avoids one Firestore read per bind / per scroll. */
    private final Map<String, Boolean> likedCache = new HashMap<>();

    public PostAdapter(List<Post> postList) {
        this.postList = postList;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {

        Post post = postList.get(position);
        Context context = holder.itemView.getContext();

        holder.boundPostId = post.getPostId();

        // ---------- header ----------
        String name = post.getName() == null || post.getName().isEmpty()
                ? "Unknown user" : post.getName();

        holder.name.setText(name);
        holder.username.setText(post.getUsername() == null ? "" : "@" + post.getUsername());
        holder.time.setText("· " + TimeUtils.getTimeAgo(post.getTimestamp()));

        if (post.getUserImage() != null && !post.getUserImage().isEmpty()) {
            Glide.with(context)
                    .load(post.getUserImage())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(holder.userImage);
        } else {
            Glide.with(context).clear(holder.userImage);
            holder.userImage.setImageResource(R.drawable.ic_person);
        }

        View.OnClickListener openProfile = v -> {
            Intent intent = new Intent(context, ProfileActivity.class);
            intent.putExtra("uid", post.getUid());
            context.startActivity(intent);
        };

        holder.userImage.setOnClickListener(openProfile);
        holder.authorArea.setOnClickListener(openProfile);

        // ---------- content ----------
        String content = post.getContent();

        if (content == null || content.isEmpty()) {
            holder.content.setVisibility(View.GONE);
        } else {
            holder.content.setVisibility(View.VISIBLE);
            holder.content.setText(content);
        }

        String image = post.getPostImage();

        if (image != null && !image.isEmpty()) {

            holder.postImage.setVisibility(View.VISIBLE);

            Glide.with(context)
                    .load(image)
                    .centerCrop()
                    .into(holder.postImage);

            holder.postImage.setOnClickListener(v -> {
                Intent intent = new Intent(context, ImageViewerActivity.class);
                intent.putExtra("image", image);
                context.startActivity(intent);
            });

        } else {
            Glide.with(context).clear(holder.postImage);
            holder.postImage.setVisibility(View.GONE);
            holder.postImage.setOnClickListener(null);
        }

        // ---------- stats ----------
        renderStats(holder, post);

        // ---------- like ----------
        Boolean cached = likedCache.get(post.getPostId());

        renderLike(holder, cached != null && cached);

        if (cached == null && post.getPostId() != null) {

            String requestedId = post.getPostId();

            LikeHelper.isLiked(requestedId, liked -> {
                likedCache.put(requestedId, liked);

                // The holder may have been recycled for another post meanwhile
                if (requestedId.equals(holder.boundPostId)) {
                    renderLike(holder, liked);
                }
            });
        }

        holder.like.setOnClickListener(v -> toggleLike(holder, post));

        // ---------- comment ----------
        holder.comment.setOnClickListener(v -> {
            Intent intent = new Intent(context, CommentActivity.class);
            intent.putExtra("postId", post.getPostId());
            context.startActivity(intent);
        });

        // ---------- share ----------
        holder.share.setOnClickListener(v -> {

            StringBuilder text = new StringBuilder();

            if (content != null && !content.isEmpty()) {
                text.append(content);
            }

            if (image != null && !image.isEmpty()) {
                if (text.length() > 0) text.append("\n");
                text.append(image);
            }

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, text.toString());

            context.startActivity(Intent.createChooser(shareIntent, "Share post"));
        });

        // ---------- more (own posts only) ----------
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();

        boolean mine = me != null && me.getUid().equals(post.getUid());

        holder.more.setVisibility(mine ? View.VISIBLE : View.GONE);

        holder.more.setOnClickListener(v -> {

            PopupMenu menu = new PopupMenu(context, holder.more);
            menu.getMenu().add(0, 1, 0, "Delete post");

            menu.setOnMenuItemClickListener(item -> {
                confirmDelete(context, post);
                return true;
            });

            menu.show();
        });
    }

    private void renderStats(PostViewHolder holder, Post post) {

        int likes = post.getLikesCount();
        int comments = post.getCommentsCount();

        holder.stats.setText(
                likes + (likes == 1 ? " like" : " likes")
                        + "  ·  "
                        + comments + (comments == 1 ? " comment" : " comments")
        );
    }

    private void renderLike(PostViewHolder holder, boolean liked) {

        MaterialButton button = holder.like;

        button.setText(liked ? "Liked" : "Like");
        button.setIconResource(liked ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);

        int color = liked
                ? LIKE_RED
                : MaterialColors.getColor(button,
                com.google.android.material.R.attr.colorOnSurfaceVariant);

        button.setIconTint(ColorStateList.valueOf(color));
        button.setTextColor(color);
    }

    private void toggleLike(PostViewHolder holder, Post post) {

        if (holder.likeBusy || post.getPostId() == null) {
            return;
        }

        Context context = holder.itemView.getContext();

        holder.likeBusy = true;

        String postId = post.getPostId();

        boolean wasLiked = Boolean.TRUE.equals(likedCache.get(postId));

        // Optimistic UI
        int optimisticCount = Math.max(0, post.getLikesCount() + (wasLiked ? -1 : 1));

        likedCache.put(postId, !wasLiked);
        post.setLikesCount(optimisticCount);
        renderLike(holder, !wasLiked);
        renderStats(holder, post);

        LikeHelper.toggleLike(postId, post.getUid(), new LikeHelper.ToggleCallback() {

            @Override
            public void onSuccess(boolean liked, long newCount) {
                holder.likeBusy = false;

                likedCache.put(postId, liked);
                post.setLikesCount((int) newCount);

                if (postId.equals(holder.boundPostId)) {
                    renderLike(holder, liked);
                    renderStats(holder, post);
                }
            }

            @Override
            public void onFailure(@NonNull Exception e) {
                holder.likeBusy = false;

                // roll back
                likedCache.put(postId, wasLiked);
                post.setLikesCount(Math.max(0, post.getLikesCount() + (wasLiked ? 1 : -1)));

                if (postId.equals(holder.boundPostId)) {
                    renderLike(holder, wasLiked);
                    renderStats(holder, post);
                }

                Toast.makeText(context, "Couldn't update like. Check your connection.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDelete(Context context, Post post) {

        new MaterialAlertDialogBuilder(context)
                .setTitle("Delete post?")
                .setMessage("This will permanently delete the post and its comments.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) ->
                        PostHelper.deletePost(post.getPostId(), new PostHelper.Callback() {

                            @Override
                            public void onSuccess() {
                                int index = postList.indexOf(post);

                                if (index != -1) {
                                    postList.remove(index);
                                    notifyItemRemoved(index);
                                }

                                Toast.makeText(context, "Post deleted",
                                        Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(context, "Couldn't delete post",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }))
                .show();
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {

        final ShapeableImageView userImage;
        final ShapeableImageView postImage;
        final View authorArea;

        final TextView name;
        final TextView username;
        final TextView content;
        final TextView time;
        final TextView stats;

        final MaterialButton like;
        final MaterialButton comment;
        final MaterialButton share;
        final ImageButton more;

        String boundPostId;
        boolean likeBusy;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);

            userImage = itemView.findViewById(R.id.postUserImage);
            postImage = itemView.findViewById(R.id.postImage);
            authorArea = itemView.findViewById(R.id.postAuthorArea);

            name = itemView.findViewById(R.id.postName);
            username = itemView.findViewById(R.id.postUsername);
            content = itemView.findViewById(R.id.postContent);
            time = itemView.findViewById(R.id.postTime);
            stats = itemView.findViewById(R.id.postStats);

            like = itemView.findViewById(R.id.postLike);
            comment = itemView.findViewById(R.id.postComment);
            share = itemView.findViewById(R.id.postShare);
            more = itemView.findViewById(R.id.postMore);
        }
    }
}
