package com.screenmeet.sdkdemo.feature.chat;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.screenmeet.sdk.domain.entity.ChatMessage;
import com.screenmeet.sdkdemo.R;
import com.screenmeet.sdkdemo.databinding.ItemChatMessageBinding;

public class ChatAdapter extends ListAdapter<ChatMessage, ChatAdapter.ViewHolder> {

    public ChatAdapter() { super(new ChatMessageComparator()); }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(
                ItemChatMessageBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private ItemChatMessageBinding binding;

        public ViewHolder(ItemChatMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ChatMessage chatMessage){
            binding.messageText.setText(chatMessage.getMessage());
            binding.senderName.setText(chatMessage.getFrom().getName());
            Resources res = binding.getRoot().getContext().getResources();

            if(chatMessage.isOwn()) {
                int color = res.getColor(R.color.grey_300);
                binding.messageLayout.setBackgroundTintList(ColorStateList.valueOf(color));
                binding.container.setGravity(Gravity.END);
            } else {
                binding.container.setGravity(Gravity.START);
                int color = res.getColor(R.color.colorAccent);
                binding.messageLayout.setBackgroundTintList(ColorStateList.valueOf(color));
            }
        }
    }

    static class ChatMessageComparator extends DiffUtil.ItemCallback<ChatMessage> {

        @Override
        public boolean areItemsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            return oldItem.equals(newItem);
        }
    }
}