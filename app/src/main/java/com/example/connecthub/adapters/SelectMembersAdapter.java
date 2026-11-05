package com.example.connecthub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.connecthub.R;
import com.example.connecthub.models.User;

import java.util.ArrayList;
import java.util.List;

public class SelectMembersAdapter
        extends RecyclerView.Adapter<SelectMembersAdapter.ViewHolder> {

    private final List<User> userList;
    private final List<String> selectedUsers = new ArrayList<>();

    public SelectMembersAdapter(List<User> userList) {
        this.userList = userList;
    }

    public List<String> getSelectedUsers() {
        return selectedUsers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_select_user, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {

        User user = userList.get(position);

        holder.tvName.setText(user.getName() == null ? "" : user.getName());
        holder.tvUsername.setText(user.getUsername() == null ? "" : "@" + user.getUsername());

        if (user.getImage() != null && !user.getImage().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.getImage())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(holder.imgUser);
        } else {
            Glide.with(holder.itemView.getContext()).clear(holder.imgUser);
            holder.imgUser.setImageResource(R.drawable.ic_person);
        }

        // tapping the whole row toggles the checkbox
        holder.itemView.setOnClickListener(v -> holder.checkBox.toggle());

        holder.checkBox.setOnCheckedChangeListener(null);

        holder.checkBox.setChecked(
                selectedUsers.contains(user.getUid())
        );

        holder.checkBox.setOnCheckedChangeListener((buttonView, checked) -> {

            if (checked) {

                if (!selectedUsers.contains(user.getUid())) {
                    selectedUsers.add(user.getUid());
                }

            } else {

                selectedUsers.remove(user.getUid());

            }

        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvUsername;
        CheckBox checkBox;
        ImageView imgUser;

        ViewHolder(View itemView) {
            super(itemView);

            tvName = itemView.findViewById(R.id.tvName);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            checkBox = itemView.findViewById(R.id.checkbox);
            imgUser = itemView.findViewById(R.id.imgUser);
        }
    }
}