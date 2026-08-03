package com.example.connecthub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.connecthub.R;
import com.example.connecthub.models.User;

import java.util.List;

public class AddMemberAdapter
        extends RecyclerView.Adapter<AddMemberAdapter.ViewHolder> {

    public interface OnAddClickListener {
        void onAdd(User user);
    }

    private final List<User> users;
    private final OnAddClickListener listener;

    public AddMemberAdapter(List<User> users,
                            OnAddClickListener listener) {

        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_add_member, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {

        User user = users.get(position);

        holder.tvName.setText(user.getName());
        holder.tvUsername.setText("@" + user.getUsername());

        Glide.with(holder.itemView.getContext())
                .load(user.getImage())
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(holder.imgUser);

        holder.btnAdd.setOnClickListener(v ->
                listener.onAdd(user));

    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imgUser;
        TextView tvName;
        TextView tvUsername;
        ImageButton btnAdd;

        ViewHolder(View itemView) {
            super(itemView);

            imgUser = itemView.findViewById(R.id.imgUser);
            tvName = itemView.findViewById(R.id.tvName);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            btnAdd = itemView.findViewById(R.id.btnAdd);
        }
    }
}