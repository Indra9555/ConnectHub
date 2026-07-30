package com.example.connecthub.adapters;

import androidx.recyclerview.widget.DiffUtil;

import com.example.connecthub.models.Message;

import java.util.List;
import java.util.Objects;

public class MessageDiffCallback extends DiffUtil.Callback {

    private final List<Message> oldList;
    private final List<Message> newList;

    public MessageDiffCallback(List<Message> oldList, List<Message> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {

        return oldList.get(oldItemPosition)
                .getMessageId()
                .equals(newList.get(newItemPosition).getMessageId());

    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {

        Message oldMsg = oldList.get(oldItemPosition);
        Message newMsg = newList.get(newItemPosition);

        return Objects.equals(oldMsg.getMessage(), newMsg.getMessage())
                && Objects.equals(oldMsg.getImageUrl(), newMsg.getImageUrl())
                && Objects.equals(oldMsg.getVoiceUrl(), newMsg.getVoiceUrl())
                && Objects.equals(oldMsg.getReactionSummary(), newMsg.getReactionSummary())
                && oldMsg.isSeen() == newMsg.isSeen()
                && oldMsg.isDeleted() == newMsg.isDeleted();

    }
}