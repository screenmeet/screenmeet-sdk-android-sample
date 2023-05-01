package com.screenmeet.live.feature.call.chat

import android.content.res.ColorStateList
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
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
            var color: Int
            val gravity: Int

            if (chatMessage.isOwn) {
                gravity = Gravity.END
                color = res.getColor(R.color.enabled_button, null)
            } else {
                gravity = Gravity.START
                color = res.getColor(R.color.dark_grey, null)
            }

            if (chatMessage.status == ChatMessage.Status.IN_TRANSFER) {
                color = ColorUtils.setAlphaComponent(color, 100)
            }

            binding.messageLayout.backgroundTintList = ColorStateList.valueOf(color)
            binding.container.gravity = gravity
        }
    }

    internal class ChatMessageComparator : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id || oldItem.transferId == newItem.transferId
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }
}
