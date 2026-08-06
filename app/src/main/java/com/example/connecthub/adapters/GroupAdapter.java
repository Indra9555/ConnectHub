package com.example.connecthub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connecthub.R;
import com.example.connecthub.models.Group;
import com.example.connecthub.models.GroupMemberInfo;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.util.Map;

public class GroupAdapter
        extends RecyclerView.Adapter<GroupAdapter.ViewHolder> {

    public interface OnGroupClickListener {
        void onGroupClick(Group group);
    }

    private final List<Group> groups;
    private final OnGroupClickListener listener;

    public GroupAdapter(
            List<Group> groups,
            OnGroupClickListener listener
    ) {
        this.groups = groups;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {

        Group group = groups.get(position);

        holder.tvName.setText(group.getGroupName());

        String myUid = FirebaseAuth.getInstance()
                .getCurrentUser()
                .getUid();

        boolean active = false;

        Map<String, GroupMemberInfo> memberInfo =
                group.getMemberInfo();

        if (memberInfo != null &&
                memberInfo.containsKey(myUid)) {

            GroupMemberInfo info = memberInfo.get(myUid);

            if (info != null) {
                active = info.isActive();
            }
        }

        if (active) {

            holder.tvMembers.setText(
                    group.getMembersCount() + " members"
            );

        } else {

            holder.tvMembers.setText("You left");

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

        TextView tvName;
        TextView tvMembers;

        ViewHolder(View itemView) {

            super(itemView);

            tvName = itemView.findViewById(R.id.tvGroupName);
            tvMembers = itemView.findViewById(R.id.tvMembers);

        }

    }

}