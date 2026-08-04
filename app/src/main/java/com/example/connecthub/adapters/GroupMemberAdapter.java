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
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class GroupMemberAdapter
        extends RecyclerView.Adapter<GroupMemberAdapter.MemberViewHolder> {

    private final List<User> members;
    private List<String> admins;
    private boolean isAdmin;



    private final OnRemoveClickListener listener;
    private final OnPromoteClickListener promoteListener;
    public interface OnRemoveClickListener {
        void onRemove(User user);
    }
    public interface OnPromoteClickListener {
        void onPromote(User user);
    }

    public GroupMemberAdapter(
            List<User> members,
            List<String> admins,
            boolean isAdmin,
            OnRemoveClickListener listener,
            OnPromoteClickListener promoteListener) {

        this.members = members;
        this.admins = admins;
        this.isAdmin = isAdmin;
        this.listener = listener;
        this.promoteListener = promoteListener;
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

        // Debug Logs
        android.util.Log.d("ADMIN_CHECK",
                "User = " + user.getName());

        android.util.Log.d("ADMIN_CHECK",
                "UID = " + user.getUid());

        android.util.Log.d("ADMIN_CHECK",
                "Admins = " + admins);

        android.util.Log.d("ADMIN_CHECK",
                "Contains = " + admins.contains(user.getUid()));

        // Name
        holder.tvName.setText(user.getName());

        // Profile Image
        Glide.with(holder.itemView.getContext())
                .load(user.getImage())
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(holder.imgProfile);

        // Admin Label
        if (admins != null && admins.contains(user.getUid())) {

            holder.tvRole.setVisibility(View.VISIBLE);
            holder.tvRole.setText("Admin");

        } else {

            holder.tvRole.setVisibility(View.GONE);

        }

        // ==========================
        // Promote Button
        // ==========================

        if (isAdmin && !admins.contains(user.getUid())) {

            holder.btnPromote.setVisibility(View.VISIBLE);

            holder.btnPromote.setOnClickListener(v ->
                    promoteListener.onPromote(user));

        } else {

            holder.btnPromote.setVisibility(View.GONE);

        }

        // ==========================
        // Remove Button
        // ==========================

        String myUid = FirebaseAuth.getInstance()
                .getCurrentUser()
                .getUid();

        if (isAdmin && !user.getUid().equals(myUid)) {

            holder.btnRemove.setVisibility(View.VISIBLE);

            holder.btnRemove.setOnClickListener(v ->
                    listener.onRemove(user));

        } else {

            holder.btnRemove.setVisibility(View.GONE);

        }

    }

    public void updateAdmins(List<String> admins, boolean isAdmin) {

        this.admins.clear();
        this.admins.addAll(admins);

        this.isAdmin = isAdmin;

        notifyDataSetChanged();
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
        ImageView btnPromote;
        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);

            imgProfile = itemView.findViewById(R.id.imgProfile);
            tvName = itemView.findViewById(R.id.tvName);
            btnRemove = itemView.findViewById(R.id.btnRemove);
            tvRole = itemView.findViewById(R.id.tvRole);
            btnPromote = itemView.findViewById(R.id.btnPromote);
        }
    }
}