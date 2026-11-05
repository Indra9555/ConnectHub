package com.example.connecthub.helpers;

import android.graphics.Canvas;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class SwipeToReplyCallback extends ItemTouchHelper.SimpleCallback {

    public interface SwipeReplyListener {
        void onReply(int position);
    }

    private final SwipeReplyListener listener;

    public SwipeToReplyCallback(SwipeReplyListener listener) {
        super(0, ItemTouchHelper.LEFT);
        this.listener = listener;
    }

    @Override
    public boolean onMove(
            @NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder,
            @NonNull RecyclerView.ViewHolder target
    ) {
        return false;
    }

    @Override
    public void onSwiped(
            @NonNull RecyclerView.ViewHolder viewHolder,
            int direction
    ) {
        int position = viewHolder.getBindingAdapterPosition();

        // NO_POSITION (-1) can happen if the list changed during the swipe
        if (position != RecyclerView.NO_POSITION) {
            listener.onReply(position);
        }
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        // reply after a short swipe (default is half the screen width)
        return 0.2f;
    }

    @Override
    public void onChildDraw(
            @NonNull Canvas c,
            @NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder,
            float dX,
            float dY,
            int actionState,
            boolean isCurrentlyActive
    ) {

        if (dX < -250) {
            dX = -250;
        }

        super.onChildDraw(
                c,
                recyclerView,
                viewHolder,
                dX,
                dY,
                actionState,
                isCurrentlyActive
        );
    }
}