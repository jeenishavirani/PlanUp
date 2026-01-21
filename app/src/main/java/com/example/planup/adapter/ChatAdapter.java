package com.example.planup.adapter;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.planup.R;
import com.example.planup.model.ChatMessage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ChatMessage> messages;
    private final Set<Integer> animatedPositions = new HashSet<>();

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getSender();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        if (viewType == ChatMessage.USER) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_user, parent, false);
            return new UserVH(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_ai, parent, false);
            return new AiVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);

        if (holder instanceof UserVH) {
            ((UserVH) holder).tv.setText(msg.getMessage());
        } else {
            AiVH aiVH = (AiVH) holder;
            String text = msg.getMessage();
            
            // Typewriter effect for AI responses
            if (msg.getSender() == ChatMessage.AI && !animatedPositions.contains(position)) {
                animatedPositions.add(position);
                animateText(aiVH.tv, text);
            } else {
                aiVH.tv.setText(text);
            }
        }
        
        // General entry animation for the bubble
        setAnimation(holder.itemView, position);
    }

    private void animateText(TextView textView, String text) {
        final int delay = 15; // Speed of typing in ms
        textView.setText("");
        Handler handler = new Handler(Looper.getMainLooper());
        
        new Thread(() -> {
            for (int i = 0; i <= text.length(); i++) {
                String partialText = text.substring(0, i);
                handler.post(() -> textView.setText(partialText));
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    private void setAnimation(View viewToAnimate, int position) {
        // Simple slide up and fade for every new bubble
        Animation animation = AnimationUtils.loadAnimation(viewToAnimate.getContext(), android.R.anim.fade_in);
        animation.setDuration(400);
        viewToAnimate.startAnimation(animation);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class UserVH extends RecyclerView.ViewHolder {
        TextView tv;
        UserVH(View v) {
            super(v);
            tv = v.findViewById(R.id.tvMessage);
        }
    }

    static class AiVH extends RecyclerView.ViewHolder {
        TextView tv;
        AiVH(View v) {
            super(v);
            tv = v.findViewById(R.id.tvMessage);
        }
    }
}
