package com.screenmeet.live.feature.chat

import android.content.res.ColorStateList
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.screenmeet.live.R
import com.screenmeet.live.databinding.ItemChatMessageBinding
import com.screenmeet.sdk.domain.entity.ChatMessage

class ChatAdapter : ListAdapter<ChatMessage, ChatAdapter.ViewHolder>(ChatMessageComparator()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemChatMessageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(
        binding.root
    ) {
        fun bind(chatMessage: ChatMessage) {
            binding.messageText.text = chatMessage.message
            binding.senderName.text = chatMessage.from.name
            val res = binding.root.context.resources
            if (chatMessage.isOwn) {
                val color = res.getColor(R.color.grey_300)
                binding.messageLayout.backgroundTintList = ColorStateList.valueOf(color)
                binding.container.gravity = Gravity.END
            } else {
                binding.container.gravity = Gravity.START
                val color = res.getColor(R.color.colorAccent)
                binding.messageLayout.backgroundTintList = ColorStateList.valueOf(color)
            }
        }
    }

    internal class ChatMessageComparator : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }
}
