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
import com.example.connecthub.models.Group;
import com.example.connecthub.models.GroupMemberInfo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.Map;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.ViewHolder> {

    public interface OnGroupClickListener {
        void onGroupClick(Group group);
    }

    private final List<Group> groups;
    private final OnGroupClickListener listener;

    public GroupAdapter(List<Group> groups, OnGroupClickListener listener) {
        this.groups = groups;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Group group = groups.get(position);

        holder.tvName.setText(group.getGroupName() == null ? "Group" : group.getGroupName());

        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();

        boolean active = false;

        Map<String, GroupMemberInfo> memberInfo = group.getMemberInfo();

        if (me != null && memberInfo != null) {
            GroupMemberInfo info = memberInfo.get(me.getUid());
            active = info != null && info.isActive();
        }

        if (active) {
            int count = group.getMembersCount();
            holder.tvMembers.setText(count + (count == 1 ? " member" : " members"));
        } else {
            holder.tvMembers.setText("You left this group");
        }

        String last = group.getLastMessage();

        if (last == null || last.isEmpty()) {
            holder.tvLastMessage.setVisibility(View.GONE);
        } else {
            holder.tvLastMessage.setVisibility(View.VISIBLE);
            holder.tvLastMessage.setText(last);
        }

        if (group.getGroupImage() != null && !group.getGroupImage().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(group.getGroupImage())
                    .placeholder(R.drawable.ic_group_avatar)
                    .error(R.drawable.ic_group_avatar)
                    .into(holder.imgGroup);
        } else {
            Glide.with(holder.itemView.getContext()).clear(holder.imgGroup);
            holder.imgGroup.setImageResource(R.drawable.ic_group_avatar);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGroupClick(group);
            }
        });
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final ImageView imgGroup;
        final TextView tvName;
        final TextView tvMembers;
        final TextView tvLastMessage;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgGroup = itemView.findViewById(R.id.imgGroup);
            tvName = itemView.findViewById(R.id.tvGroupName);
            tvMembers = itemView.findViewById(R.id.tvMembers);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
        }
    }
}
