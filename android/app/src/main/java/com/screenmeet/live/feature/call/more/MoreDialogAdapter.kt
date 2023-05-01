package com.screenmeet.live.feature.call.more

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.screenmeet.live.databinding.ItemMoreActionBinding

typealias ActionClick = (MoreActionItem) -> Unit

class MoreDialogAdapter(
    private val clickListener: ActionClick
) : ListAdapter<MoreActionItem, MoreDialogAdapter.ViewHolder>(ItemsComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        val binding = ItemMoreActionBinding.inflate(inflater)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.bind(getItem(position), clickListener)
    }

    class ViewHolder(val binding: ItemMoreActionBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MoreActionItem, clickAction: ActionClick) {
            binding.apply {
                val context = root.context
                root.setOnClickListener { clickAction(item) }
                actionText.text = context.getString(item.text)
                actionImage.setImageResource(item.image)
            }
        }
    }

    internal class ItemsComparator : DiffUtil.ItemCallback<MoreActionItem>() {
        override fun areItemsTheSame(oldItem: MoreActionItem, newItem: MoreActionItem): Boolean {
            return oldItem.text == newItem.text
        }

        override fun areContentsTheSame(oldItem: MoreActionItem, newItem: MoreActionItem): Boolean {
            return oldItem == newItem
        }
    }
}
