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
import com.example.connecthub.models.User;

import java.util.List;

public class GroupMemberAdapter
        extends RecyclerView.Adapter<GroupMemberAdapter.MemberViewHolder> {

    private final List<User> members;
    private final List<String> admins;

    private final boolean isAdmin;

    private final OnRemoveClickListener listener;

    public interface OnRemoveClickListener {
        void onRemove(User user);
    }

    public GroupMemberAdapter(
            List<User> members,
            List<String> admins,
            boolean isAdmin,
            OnRemoveClickListener listener) {

        this.members = members;
        this.admins = admins;
        this.isAdmin = isAdmin;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group_member, parent, false);

        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull MemberViewHolder holder,
            int position) {

        User user = members.get(position);

        holder.tvName.setText(user.getName());

        Glide.with(holder.itemView.getContext())
                .load(user.getImage())
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(holder.imgProfile);

        if (admins.contains(user.getUid())) {

            holder.tvRole.setVisibility(View.VISIBLE);
            holder.tvRole.setText("Admin");

        } else {

            holder.tvRole.setVisibility(View.GONE);

        }

        if (isAdmin && !admins.contains(user.getUid())) {

            holder.btnRemove.setVisibility(View.VISIBLE);

            holder.btnRemove.setOnClickListener(v ->
                    listener.onRemove(user));

        } else {

            holder.btnRemove.setVisibility(View.GONE);

        }

    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class MemberViewHolder
            extends RecyclerView.ViewHolder {

        ImageView imgProfile;

        ImageView btnRemove;

        TextView tvName;

        TextView tvRole;
        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);

            imgProfile = itemView.findViewById(R.id.imgProfile);
            tvName = itemView.findViewById(R.id.tvName);
            btnRemove = itemView.findViewById(R.id.btnRemove);
            tvRole = itemView.findViewById(R.id.tvRole);
        }
    }
}