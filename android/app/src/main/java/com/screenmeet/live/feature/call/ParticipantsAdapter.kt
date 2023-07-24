package com.screenmeet.live.feature.call

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.screenmeet.live.R
import com.screenmeet.live.databinding.LayoutParticipantBinding
import com.screenmeet.live.util.DoubleTapListener
import com.screenmeet.sdk.VideoElement
import kotlinx.coroutines.CoroutineScope
import org.webrtc.EglBase

typealias PinClick = (VideoElement) -> Unit

class ParticipantsAdapter(
    private val scope: CoroutineScope,
    private val eglBase: EglBase.Context,
    private val onClick: PinClick
) : ListAdapter<VideoElement, ParticipantsAdapter.ViewHolder>(UiVideoComparator()) {

    val itemSpacing = 30

    private var recyclerSize = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        recyclerSize = parent.measuredWidth
        val binding = LayoutParticipantBinding.inflate(inflater)
        binding.renderer.init(eglBase, null)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val participant = getItem(position)
        holder.bind(scope, participant, recyclerSize - itemSpacing * 3, onClick)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.dispose()
    }

    fun dispose(recycler: RecyclerView) {
        currentList.forEachIndexed { i, _ ->
            val viewHolder = recycler.findViewHolderForAdapterPosition(i) as? ViewHolder
            viewHolder?.dispose()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    class ViewHolder(
        val binding: LayoutParticipantBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(scope: CoroutineScope, video: VideoElement, recyclerSize: Int, onClick: PinClick) {
            binding.apply {
                val layoutParams = RecyclerView.LayoutParams(
                    recyclerSize / 2,
                    recyclerSize / 2
                )
                itemView.layoutParams = layoutParams

                val videoTrack = video.track
                val hasTrack = videoTrack != null
                renderer.render(videoTrack)

                if (hasTrack) {
                    renderer.listenFramesStuck(scope) { stuck ->
                        frameStuckSpinner.isVisible = stuck
                    }
                } else {
                    frameStuckSpinner.isVisible = false
                }

                nameTv.text = video.userName
                logo.isVisible = !hasTrack
                renderer.isVisible = hasTrack
                root.setOnTouchListener(
                    DoubleTapListener(root.context) {
                        onClick(video)
                    }
                )

                pinButton.setOnClickListener {
                    onClick(video)
                }

                if (video.isAudioSharing) {
                    microButton.setImageResource(R.drawable.mic)
                    microButton.backgroundTintList = null
                    microButton.colorFilter = null
                } else {
                    val context = root.context
                    microButton.setImageResource(R.drawable.mic_off)
                    val color = ContextCompat.getColor(context, R.color.bright_red)
                    microButton.setColorFilter(color)
                }
            }
        }

        fun dispose() {
            binding.renderer.clear()
        }
    }

    internal class UiVideoComparator : DiffUtil.ItemCallback<VideoElement>() {
        override fun areItemsTheSame(oldItem: VideoElement, newItem: VideoElement): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: VideoElement, newItem: VideoElement): Boolean {
            return oldItem == newItem
        }
    }
}
