package com.example.connecthub.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.connecthub.R;
import com.example.connecthub.activities.ProfileActivity;
import com.example.connecthub.models.User;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private final Context context;
    private final List<User> userList;

    public UserAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {

        User user = userList.get(position);

        holder.tvName.setText(user.getName() == null ? "" : user.getName());
        holder.tvUsername.setText(user.getUsername() == null ? "" : "@" + user.getUsername());

        if (user.getImage() != null && !user.getImage().isEmpty()) {
            Glide.with(context)
                    .load(user.getImage())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(holder.imgUser);
        } else {
            Glide.with(context).clear(holder.imgUser);
            holder.imgUser.setImageResource(R.drawable.ic_person);
        }

        // Open the profile (it has Follow + Message buttons)
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProfileActivity.class);
            intent.putExtra("uid", user.getUid());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {

        final ShapeableImageView imgUser;
        final TextView tvName;
        final TextView tvUsername;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            imgUser = itemView.findViewById(R.id.imgUser);
            tvName = itemView.findViewById(R.id.tvName);
            tvUsername = itemView.findViewById(R.id.tvUsername);
        }
    }
}
