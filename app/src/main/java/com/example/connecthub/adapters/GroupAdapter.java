package com.example.connecthub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connecthub.R;
import com.example.connecthub.models.Group;

import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.ViewHolder> {
    public interface OnGroupClickListener {
        void onGroupClick(Group group);
    }

    private final List<Group> groups;
    private final OnGroupClickListener listener;

    public GroupAdapter(List<Group> groups,
                        OnGroupClickListener listener) {

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

        android.util.Log.d(
                "GROUP_ADAPTER",
                "Binding group = " + group.getGroupName()
        );

        holder.tvName.setText(group.getGroupName());
        holder.tvMembers.setText(group.getMembersCount() + " members");

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

        TextView tvName, tvMembers;

        ViewHolder(View itemView) {
            super(itemView);

            tvName = itemView.findViewById(R.id.tvGroupName);
            tvMembers = itemView.findViewById(R.id.tvMembers);
        }
    }
}