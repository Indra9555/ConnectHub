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
    private String ownerUid = "";

    private final OnRemoveClickListener listener;
    private final OnPromoteClickListener promoteListener;
    private final OnDemoteClickListener demoteListener;

    public interface OnRemoveClickListener {
        void onRemove(User user);
    }

    public interface OnPromoteClickListener {
        void onPromote(User user);
    }

    public interface OnDemoteClickListener {
        void onDemote(User user);
    }

    public GroupMemberAdapter(
            List<User> members,
            List<String> admins,
            boolean isAdmin,
            OnRemoveClickListener listener,
            OnPromoteClickListener promoteListener,
            OnDemoteClickListener demoteListener) {

        this.members = members;
        this.admins = admins;
        this.isAdmin = isAdmin;
        this.listener = listener;
        this.promoteListener = promoteListener;
        this.demoteListener = demoteListener;
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

        String myUid = FirebaseAuth.getInstance()
                .getCurrentUser()
                .getUid();

        boolean userIsAdmin =
                admins != null &&
                        admins.contains(user.getUid());

        // ==========================
        // Name
        // ==========================

        boolean userIsOwner = user.getUid() != null && user.getUid().equals(ownerUid);

        holder.tvName.setText(user.getName() == null ? "" : user.getName()
                + (user.getUid() != null && user.getUid().equals(myUid) ? " (You)" : ""));

        // ==========================
        // Profile Image
        // ==========================

        Glide.with(holder.itemView.getContext())
                .load(user.getImage() == null || user.getImage().isEmpty() ? null : user.getImage())
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(holder.imgProfile);

        // ==========================
        // Admin Label
        // ==========================

        if (userIsAdmin) {

            holder.tvRole.setVisibility(View.VISIBLE);
            holder.tvRole.setText("Admin");

        } else {

            holder.tvRole.setVisibility(View.GONE);
        }

        // ==========================
// Promote / Demote Button
// ==========================

        if (isAdmin && !user.getUid().equals(myUid)) {

            holder.btnPromote.setVisibility(View.VISIBLE);

            if (userIsAdmin) {

                // Admin → Demote

                holder.btnPromote.setImageResource(R.drawable.ic_star);
                holder.btnPromote.setContentDescription(
                        "Demote admin"
                );

                holder.btnPromote.setOnClickListener(v ->
                        demoteListener.onDemote(user)
                );

            } else {

                // Member → Promote

                holder.btnPromote.setImageResource(R.drawable.ic_star_outline);
                holder.btnPromote.setContentDescription(
                        "Promote to admin"
                );

                holder.btnPromote.setOnClickListener(v ->
                        promoteListener.onPromote(user)
                );
            }

        } else {

            holder.btnPromote.setVisibility(View.GONE);
            holder.btnPromote.setOnClickListener(null);
        }

        // ==========================
        // Remove Button
        // ==========================

        if (isAdmin && !user.getUid().equals(myUid)) {

            holder.btnRemove.setVisibility(View.VISIBLE);

            holder.btnRemove.setOnClickListener(v ->
                    listener.onRemove(user)
            );

        } else {

            holder.btnRemove.setVisibility(View.GONE);
            holder.btnRemove.setOnClickListener(null);
        }
    }

    public void setOwner(String ownerUid) {
        this.ownerUid = ownerUid == null ? "" : ownerUid;
    }

    public void updateAdmins(
            List<String> admins,
            boolean isAdmin) {

        this.admins.clear();

        if (admins != null) {
            this.admins.addAll(admins);
        }

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
        ImageView btnPromote;

        TextView tvName;
        TextView tvRole;

        public MemberViewHolder(
                @NonNull View itemView) {

            super(itemView);

            imgProfile =
                    itemView.findViewById(R.id.imgProfile);

            tvName =
                    itemView.findViewById(R.id.tvName);

            btnRemove =
                    itemView.findViewById(R.id.btnRemove);

            tvRole =
                    itemView.findViewById(R.id.tvRole);

            btnPromote =
                    itemView.findViewById(R.id.btnPromote);
        }
    }
}