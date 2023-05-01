package com.screenmeet.live.feature.call.people

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.screenmeet.live.R
import com.screenmeet.live.databinding.ItemParticipantBinding
import com.screenmeet.sdk.Participant

typealias ActionClick = (Participant) -> Unit

class PeopleAdapter(
    private val clickListener: ActionClick
) : ListAdapter<Participant, PeopleAdapter.ViewHolder>(ParticipantComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        val binding = ItemParticipantBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.bind(getItem(position), clickListener)
    }

    class ViewHolder(val binding: ItemParticipantBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(participant: Participant, clickAction: ActionClick) {
            binding.apply {
                root.setOnClickListener { clickAction(participant) }
                nameTv.text = participant.identity.name
                val audioImage = if (participant.mediaState.isAudioSharing) {
                    R.drawable.mic
                } else {
                    R.drawable.mic_off
                }
                audioIv.setImageResource(audioImage)
                hostIv.isVisible = participant.identity.isHost
            }
        }
    }

    internal class ParticipantComparator : DiffUtil.ItemCallback<Participant>() {
        override fun areItemsTheSame(oldItem: Participant, newItem: Participant): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Participant, newItem: Participant): Boolean {
            return oldItem == newItem
        }
    }
}
