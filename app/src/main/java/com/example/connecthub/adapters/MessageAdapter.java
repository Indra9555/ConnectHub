package com.example.connecthub.adapters;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.media.MediaPlayer;
import android.media.AudioAttributes;
import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connecthub.R;
import com.example.connecthub.activities.ImageViewerActivity;
import com.example.connecthub.models.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.masoudss.lib.WaveformSeekBar;

import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.net.Uri;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    private MediaPlayer currentPlayer;
    private float playbackSpeed = 1.0f;
    private String currentPlayingMessageId = "";
    private static final int SENT = 1;
    private static final int RECEIVED = 2;

    private final List<Message> messageList;
    private int lastAnimatedPosition = -1;
    private final java.util.HashMap<String, Message> messageMap = new java.util.HashMap<>();

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
        setHasStableIds(true);
    }
    @Override
    public long getItemId(int position) {
        return messageList.get(position)
                .getMessageId()
                .hashCode();
    }
    public interface OnMessageLongClickListener {
        void onMessageLongClick(View anchor, Message message);
    }
    private OnMessageLongClickListener listener;

    public void setOnMessageLongClickListener(OnMessageLongClickListener listener) {
        this.listener = listener;
    }
    private OnReplyClickListener replyListener;
    public void setOnReplyClickListener(OnReplyClickListener listener) {
        this.replyListener = listener;
    }
    public interface OnReplyClickListener {
        void onReplyClick(String messageId);
    }

    public interface OnMessageDoubleTapListener {
        void onDoubleTap(Message message);
    }

    private OnMessageDoubleTapListener doubleTapListener;

    public void setOnMessageDoubleTapListener(OnMessageDoubleTapListener listener) {
        this.doubleTapListener = listener;
    }

    @Override
    public int getItemViewType(int position) {

        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (messageList.get(position).getSenderId().equals(currentUid)) {
            return SENT;
        }

        return RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        if (viewType == SENT) {

            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);

            return new SentViewHolder(view);

        } else {

            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);

            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        Message message = messageList.get(position);

        bindTime(
                holder instanceof SentViewHolder
                        ? ((SentViewHolder) holder).tvTime
                        : ((ReceivedViewHolder) holder).tvTime,
                message
        );

        if (holder instanceof SentViewHolder) {

            SentViewHolder sentHolder = (SentViewHolder) holder;

            bindReplyPreview(
                    sentHolder.layoutReplyPreview,
                    sentHolder.imgReply,
                    sentHolder.tvReplySender,
                    sentHolder.tvReplyMessage,
                    message,
                    sentHolder.itemView
            );

            if ("image".equals(message.getType())) {

                bindImageMessage(
                        sentHolder.imgMessage,
                        sentHolder.tvMessage,
                        sentHolder.tvUploading,
                        message,
                        sentHolder.itemView
                );

                sentHolder.layoutVoice.setVisibility(View.GONE);
                sentHolder.tvMessage.setVisibility(View.GONE);

            } else if ("voice".equals(message.getType())) {

                sentHolder.imgMessage.setVisibility(View.GONE);
                sentHolder.tvMessage.setVisibility(View.GONE);
                bindVoiceMessage(
                        sentHolder.layoutVoice,
                        sentHolder.btnVoicePlay,
                        sentHolder.waveformSeekBar,
                        sentHolder.tvVoiceDuration,
                        sentHolder.tvSpeed,
                        message
                );

            } else {

                sentHolder.imgMessage.setVisibility(View.GONE);
                sentHolder.layoutVoice.setVisibility(View.GONE);
                bindTextMessage(sentHolder.tvMessage, message, true);
            }

            bindSeen(sentHolder.tvSeen, message);
            bindReaction(sentHolder.tvReaction, message);

            sentHolder.layoutBubble.setOnLongClickListener(v -> {
                if (listener != null)
                    listener.onMessageLongClick(v, message);
                return true;
            });

            attachDoubleTap(sentHolder.layoutBubble, message);

        } else {

            ReceivedViewHolder receivedHolder = (ReceivedViewHolder) holder;

            bindReplyPreview(
                    receivedHolder.layoutReplyPreview,
                    receivedHolder.imgReply,
                    receivedHolder.tvReplySender,
                    receivedHolder.tvReplyMessage,
                    message,
                    receivedHolder.itemView
            );

            if ("image".equals(message.getType())) {

                bindImageMessage(
                        receivedHolder.imgMessage,
                        receivedHolder.tvMessage,
                        receivedHolder.tvUploading,
                        message,
                        receivedHolder.itemView
                );

                receivedHolder.layoutVoice.setVisibility(View.GONE);
                receivedHolder.tvMessage.setVisibility(View.GONE);

            } else if ("voice".equals(message.getType())) {

                receivedHolder.imgMessage.setVisibility(View.GONE);
                receivedHolder.tvMessage.setVisibility(View.GONE);
                bindVoiceMessage(
                        receivedHolder.layoutVoice,
                        receivedHolder.btnVoicePlay,
                        receivedHolder.waveformSeekBar,
                        receivedHolder.tvVoiceDuration,
                        receivedHolder.tvSpeed,
                        message
                );

            } else {

                receivedHolder.imgMessage.setVisibility(View.GONE);
                receivedHolder.layoutVoice.setVisibility(View.GONE);
                bindTextMessage(receivedHolder.tvMessage, message, false);
            }

            bindReaction(receivedHolder.tvReaction, message);

            receivedHolder.layoutBubble.setOnLongClickListener(v -> {
                if (listener != null)
                    listener.onMessageLongClick(v, message);
                return true;
            });

            attachDoubleTap( receivedHolder.layoutBubble, message);
        }

        if (position > lastAnimatedPosition) {
            Animation animation = AnimationUtils.loadAnimation(
                    holder.itemView.getContext(),
                    holder instanceof SentViewHolder
                            ? R.anim.slide_in_right
                            : R.anim.slide_in_left
            );
            holder.itemView.startAnimation(animation);
            lastAnimatedPosition = position;
        }
    }
    public void rebuildMessageMap() {

        messageMap.clear();

        for (Message m : messageList) {

            messageMap.put(m.getMessageId(), m);
        }

    }


    @Override
    public int getItemCount() {
        return messageList.size();
    }
    private void bindReplyPreview(
            LinearLayout layoutReplyPreview,
            ImageView imgReply,
            TextView tvReplySender,
            TextView tvReplyMessage,
            Message message,
            View itemView
    ) {

        if (message.getReplyMessageId() == null ||
                message.getReplyMessageId().isEmpty()) {

            layoutReplyPreview.setVisibility(View.GONE);
            return;
        }

        layoutReplyPreview.setVisibility(View.VISIBLE);

        tvReplySender.setText(message.getReplySender());

        Message replied = messageMap.get(message.getReplyMessageId());

        // Original message no longer exists
        if (replied == null) {

            imgReply.setVisibility(View.GONE);
            tvReplyMessage.setText("🚫 This message was deleted");
            return;
        }

        // Original message exists but is deleted
        if (replied.isDeleted()) {

            imgReply.setVisibility(View.GONE);
            tvReplyMessage.setText("🚫 This message was deleted");
            return;
        }

        // Original is an image
        if ("image".equals(replied.getType())) {

            imgReply.setVisibility(View.VISIBLE);

            Glide.with(itemView.getContext())
                    .load(replied.getImageUrl())
                    .into(imgReply);

            tvReplyMessage.setText("Photo");

        } else {

            imgReply.setVisibility(View.GONE);
            tvReplyMessage.setText(replied.getMessage());

        }

        layoutReplyPreview.setOnClickListener(v -> {

            if (replyListener != null) {
                replyListener.onReplyClick(message.getReplyMessageId());
            }

        });
    }
    private void bindTextMessage(TextView tvMessage, Message message, boolean isSender) {

        tvMessage.setVisibility(View.VISIBLE);

        if (message.isDeleted()) {

            if (isSender) {
                tvMessage.setText("🚫 You deleted this message");

                tvMessage.setTextColor(0xFF000000);
            } else {
                tvMessage.setText("🚫 This message was deleted");

                tvMessage.setTextColor(0xFF000000);
            }

        } else {

            tvMessage.setText(message.getMessage());

        }

    }
    private void bindImageMessage(
            ImageView imgMessage,
            TextView tvMessage,
            TextView tvUploading,
            Message message,
            View itemView
    ) {

        imgMessage.setVisibility(View.VISIBLE);
        tvMessage.setVisibility(View.GONE);

        if (message.isUploading()) {

            tvUploading.setVisibility(View.VISIBLE);

            Glide.with(itemView.getContext())
                    .load(message.getLocalImageUri())
                    .into(imgMessage);

        } else {

            tvUploading.setVisibility(View.GONE);

            Glide.with(itemView.getContext())
                    .load(message.getImageUrl())
                    .into(imgMessage);

        }

        imgMessage.setOnClickListener(v -> {

            if (message.isUploading()) return;

            Intent intent = new Intent(
                    itemView.getContext(),
                    ImageViewerActivity.class
            );

            intent.putExtra("image", message.getImageUrl());

            itemView.getContext().startActivity(intent);

        });

    }
    private void bindSeen(TextView tvSeen, Message message) {

        if (tvSeen == null) return;

        if (message.isSeen()) {

            tvSeen.setText("✓✓");
            tvSeen.setTextColor(0xFF2196F3);

        } else {

            tvSeen.setText("✓");
            tvSeen.setTextColor(0xFF888888);

        }

    }

    private void attachDoubleTap(View target, Message message) {

        GestureDetector detector = new GestureDetector(
                target.getContext(),
                new GestureDetector.SimpleOnGestureListener() {

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {

                        if (doubleTapListener != null) {
                            doubleTapListener.onDoubleTap(message);
                        }

                        return true;
                    }
                });

        target.setOnTouchListener((v, event) -> detector.onTouchEvent(event));
    }


    private void bindReaction(TextView tvReaction, Message message) {
        if (message.isDeleted()) {
            tvReaction.setVisibility(View.GONE);
            return;
        }

        if (message.getReactions() == null || message.getReactions().isEmpty()) {
            tvReaction.setVisibility(View.GONE);
            return;
        }

        java.util.LinkedHashSet<String> uniqueReactions =
                new java.util.LinkedHashSet<>(message.getReactions().values());

        StringBuilder builder = new StringBuilder();

        int shown = 0;

        for (String emoji : uniqueReactions) {

            if (shown == 3) break;

            builder.append(emoji).append(" ");
            shown++;
        }

        if (uniqueReactions.size() > 3) {

            builder.append("+")
                    .append(uniqueReactions.size() - 3);

        }

        tvReaction.setText(builder.toString().trim());

        if (tvReaction.getVisibility() != View.VISIBLE) {

            tvReaction.setVisibility(View.VISIBLE);

            Animation pop = AnimationUtils.loadAnimation(
                    tvReaction.getContext(),
                    R.anim.pop
            );

            tvReaction.startAnimation(pop);

        }
    }

    private void bindTime(TextView tvTime, Message message) {

        String time = new SimpleDateFormat(
                "hh:mm a",
                Locale.getDefault()
        ).format(new Date(message.getTimestamp()));

        tvTime.setText(time);

    }

    static class SentViewHolder extends RecyclerView.ViewHolder {

        ImageView imgMessage;

        TextView tvMessage;
        TextView tvTime;
        TextView tvSeen;
        TextView tvUploading;
        LinearLayout layoutReplyPreview;
        TextView tvReplySender;
        TextView tvReplyMessage;
        ImageView imgReply;
        TextView tvReaction;
        LinearLayout layoutVoice;
        TextView btnVoicePlay;
        TextView tvVoiceDuration;
        WaveformSeekBar waveformSeekBar;
        TextView tvSpeed;
        LinearLayout layoutBubble;

        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvSeen = itemView.findViewById(R.id.tvSeen);
            imgMessage = itemView.findViewById(R.id.imgMessage);
            tvUploading = itemView.findViewById(R.id.tvUploading);
            layoutReplyPreview = itemView.findViewById(R.id.layoutReplyPreview);
            tvReplySender = itemView.findViewById(R.id.tvReplySender);
            tvReplyMessage = itemView.findViewById(R.id.tvReplyMessage);
            imgReply = itemView.findViewById(R.id.imgReply);
            tvReaction = itemView.findViewById(R.id.tvReaction);
            layoutVoice = itemView.findViewById(R.id.layoutVoice);
            btnVoicePlay = itemView.findViewById(R.id.btnVoicePlay);
            tvVoiceDuration = itemView.findViewById(R.id.tvVoiceDuration);
            waveformSeekBar =
                    itemView.findViewById(R.id.waveformSeekBar);
            tvSpeed = itemView.findViewById(R.id.tvSpeed);
            layoutBubble = itemView.findViewById(R.id.layoutBubble);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {

        ImageView imgMessage;

        TextView tvMessage;
        TextView tvTime;
        TextView tvSeen;
        TextView tvUploading;
        LinearLayout layoutReplyPreview;
        TextView tvReplySender;
        TextView tvReplyMessage;
        ImageView imgReply;
        TextView tvReaction;
        LinearLayout layoutVoice;
        TextView btnVoicePlay;
        TextView tvVoiceDuration;
        WaveformSeekBar waveformSeekBar;
        TextView tvSpeed;
        LinearLayout layoutBubble;
        public ReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            imgMessage = itemView.findViewById(R.id.imgMessage);
            tvUploading = itemView.findViewById(R.id.tvUploading);
            layoutReplyPreview = itemView.findViewById(R.id.layoutReplyPreview);
            tvReplySender = itemView.findViewById(R.id.tvReplySender);
            tvReplyMessage = itemView.findViewById(R.id.tvReplyMessage);
            imgReply = itemView.findViewById(R.id.imgReply);
            tvReaction = itemView.findViewById(R.id.tvReaction);
            layoutVoice = itemView.findViewById(R.id.layoutVoice);
            btnVoicePlay = itemView.findViewById(R.id.btnVoicePlay);
            tvVoiceDuration = itemView.findViewById(R.id.tvVoiceDuration);
            waveformSeekBar =
                    itemView.findViewById(R.id.waveformSeekBar);
            tvSpeed = itemView.findViewById(R.id.tvSpeed);
            layoutBubble = itemView.findViewById(R.id.layoutBubble);

        }
    }
    private void bindVoiceMessage(
            LinearLayout layoutVoice,
            TextView btnVoicePlay,
            WaveformSeekBar waveformSeekBar,
            TextView tvVoiceDuration,
            TextView tvSpeed,
            Message message
    ){

        layoutVoice.setVisibility(View.VISIBLE);

        tvVoiceDuration.setText(formatVoiceDuration(message.getVoiceDuration()));

        if (message.getWaveform() != null &&
                !message.getWaveform().isEmpty()) {

            int[] samples = new int[message.getWaveform().size()];

            for (int i = 0; i < samples.length; i++) {
                samples[i] = message.getWaveform().get(i);
            }

            waveformSeekBar.setSampleFrom(samples);
        }

        boolean isPlaying =
                message.getMessageId().equals(currentPlayingMessageId)
                        && currentPlayer != null
                        && currentPlayer.isPlaying();

        btnVoicePlay.setText(isPlaying ? "⏸" : "▶");

        btnVoicePlay.setOnClickListener(v -> {

            if (message.getVoiceUrl() == null
                    || message.getVoiceUrl().isEmpty()) {
                return;
            }

            // Resume
            if (currentPlayer != null
                    && message.getMessageId().equals(currentPlayingMessageId)
                    && !currentPlayer.isPlaying()) {

                currentPlayer.start();

                btnVoicePlay.setText("⏸");

                updateWaveform(
                        currentPlayer,
                        waveformSeekBar,
                        tvVoiceDuration
                );

                return;
            }

            // Pause
            if (currentPlayer != null
                    && message.getMessageId().equals(currentPlayingMessageId)
                    && currentPlayer.isPlaying()) {

                currentPlayer.pause();

                btnVoicePlay.setText("▶");

                return;
            }

            stopCurrentPlayback();

            playVoice(
                    message.getVoiceUrl(),
                    btnVoicePlay,
                    waveformSeekBar,
                    tvVoiceDuration,
                    message.getMessageId()
            );

        });
        waveformSeekBar.setOnProgressChanged(
                (waveformSeekBar1, progress, fromUser) -> {

                    if (fromUser
                            && currentPlayer != null
                            && message.getMessageId().equals(currentPlayingMessageId)) {

                        currentPlayer.seekTo((int) progress);

                        tvVoiceDuration.setText(
                                formatVoiceDuration((long) progress)
                                        + " / "
                                        + formatVoiceDuration(currentPlayer.getDuration())
                        );
                    }
                }
        );
        tvSpeed.setOnClickListener(v -> {

            if (playbackSpeed == 1.0f) {
                playbackSpeed = 1.5f;
            } else if (playbackSpeed == 1.5f) {
                playbackSpeed = 2.0f;
            } else {
                playbackSpeed = 1.0f;
            }

            tvSpeed.setText(playbackSpeed + "×");

            if (currentPlayer != null) {
                currentPlayer.setPlaybackParams(
                        currentPlayer.getPlaybackParams()
                                .setSpeed(playbackSpeed)
                );
            }

        });

    }

    private void playVoice(
            String url,
            TextView btnVoicePlay,
            WaveformSeekBar waveformSeekBar,
            TextView tvVoiceDuration,
            String messageId
    ) {
        try {
            MediaPlayer player = new MediaPlayer();
            player.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
            );
            player.setDataSource(url);

            player.setOnPreparedListener(mp -> {
                currentPlayer = mp;
                currentPlayingMessageId = messageId;

                waveformSeekBar.setMaxProgress(mp.getDuration());
                waveformSeekBar.setProgress(0);

                mp.start();

                btnVoicePlay.setText("⏸");

                updateWaveform(
                        mp,
                        waveformSeekBar,
                        tvVoiceDuration
                );
            });

            player.setOnCompletionListener(mp -> {

                btnVoicePlay.setText("▶");

                player.seekTo(0);

                waveformSeekBar.setProgress(0);

                tvVoiceDuration.setText(
                        "0:00 / "
                                + formatVoiceDuration(player.getDuration())
                );

                // Don't release the player here.
                currentPlayingMessageId = "";

            });

            player.setOnErrorListener((mp, what, extra) -> {
                stopCurrentPlayback();
                return true;
            });

            player.prepareAsync();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopCurrentPlayback() {
        try {
            if (currentPlayer != null) {
                if (currentPlayer.isPlaying()) {
                    currentPlayer.stop();
                }
                currentPlayer.reset();
                currentPlayer.release();
            }
        } catch (Exception ignored) {
        }
        currentPlayer = null;
        currentPlayingMessageId = "";
    }

    private String formatVoiceDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(java.util.Locale.getDefault(), "%d:%02d", minutes, seconds);
    }
    private void updateWaveform(
            MediaPlayer player,
            WaveformSeekBar waveformSeekBar,
            TextView duration
    ) {

        waveformSeekBar.setProgress(player.getCurrentPosition());

        duration.setText(
                formatVoiceDuration(player.getCurrentPosition())
                        + " / "
                        + formatVoiceDuration(player.getDuration())
        );

        waveformSeekBar.postDelayed(new Runnable() {

            @Override
            public void run() {

                try {

                    if (player != null
                            && currentPlayer == player
                            && player.isPlaying()) {

                        waveformSeekBar.setProgress(player.getCurrentPosition());

                        duration.setText(
                                formatVoiceDuration(player.getCurrentPosition())
                                        + " / "
                                        + formatVoiceDuration(player.getDuration())
                        );

                        waveformSeekBar.postDelayed(this, 200);

                    }

                } catch (IllegalStateException ignored) {
                    // MediaPlayer was released
                }
            }

        }, 200);

        waveformSeekBar.setOnProgressChanged((waveformSeekBar1, progress, fromUser) -> {

            if (fromUser && player != null) {
                player.seekTo((int) progress);
            }

        });
    }
}
