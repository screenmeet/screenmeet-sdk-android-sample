package com.screenmeet.live.feature.call

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.screenmeet.live.R
import com.screenmeet.live.databinding.LayoutParticipantBinding
import com.screenmeet.sdk.Identity
import com.screenmeet.sdk.Participant
import com.screenmeet.sdk.ScreenMeet
import org.webrtc.EglBase
import org.webrtc.RendererCommon

class ParticipantsAdapter(
    private val eglBase: EglBase.Context
) : ListAdapter<Participant, ParticipantsAdapter.ViewHolder>(ParticipantComparator()) {

    private var recyclerSize = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        recyclerSize = parent.measuredWidth
        val binding = LayoutParticipantBinding.inflate(inflater)
        binding.renderer.init(eglBase, null)
        binding.renderer.setZOrderOnTop(true)
        binding.renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val participant = getItem(position)
        holder.displayParticipant(participant, recyclerSize)
    }

    fun dispose(recycler: RecyclerView) {
        currentList.forEachIndexed { i, participant ->
            val viewHolder = recycler.findViewHolderForAdapterPosition(i) as? ViewHolder
            viewHolder?.dispose()
            participant.clearSinks()
        }
    }

    class ViewHolder(val binding: LayoutParticipantBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun displayParticipant(
            participant: Participant,
            recyclerSize: Int
        ) {
            binding.apply {
                itemView.isVisible = true
                itemView.layoutParams =
                    RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, recyclerSize)
                nameTv.text = participant.identity.name

                hostImage.isVisible = participant.identity.role == Identity.Role.HOST

                if (participant.mediaState.isAudioActive) {
                    microButton.setImageResource(R.drawable.mic)
                } else microButton.setImageResource(R.drawable.mic_off)

                when (participant.mediaState.videoState.source) {
                    ScreenMeet.VideoSource.BACK_CAMERA,
                    ScreenMeet.VideoSource.FRONT_CAMERA,
                    ScreenMeet.VideoSource.CUSTOM_CAMERA -> {
                        cameraButton.setImageResource(R.drawable.videocam)
                    }
                    ScreenMeet.VideoSource.SCREEN -> {
                        cameraButton.setImageResource(R.drawable.screenshot)
                    }
                    ScreenMeet.VideoSource.NONE -> {
                        cameraButton.setImageResource(R.drawable.videocam_off)
                    }
                }
                updateTrack(participant)
            }
        }

        fun updateTrack(participant: Participant) {
            val videoTrack = participant.videoTrack
            if (videoTrack != null) {
                videoTrack.addSink(binding.renderer)
            } else binding.renderer.clearImage()
            binding.renderer.isVisible = videoTrack != null
        }

        fun dispose() {
            binding.renderer.clearImage()
            binding.renderer.isVisible = false
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
