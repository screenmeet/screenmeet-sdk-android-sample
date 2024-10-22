package com.screenmeet.live.feature.call

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.screenmeet.live.R
import com.screenmeet.live.databinding.LayoutParticipantBinding
import com.screenmeet.live.tools.DoubleTapListener
import com.screenmeet.sdk.VideoElement
import kotlinx.coroutines.CoroutineScope
import org.webrtc.EglBase
import org.webrtc.RendererCommon

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
        binding.renderer.init(
            eglBase,
            object : RendererCommon.RendererEvents {
                override fun onFrameResolutionChanged(
                    videoWidth: Int,
                    videoHeight: Int,
                    rotation: Int
                ) {
                    binding.renderer.post {
                        binding.renderer.layoutParams = fitFrame(
                            videoWidth,
                            videoHeight,
                            binding.root.width,
                            binding.root.height
                        )
                    }
                }

                override fun onFirstFrameRendered() {
                }
            }
        )
        return ViewHolder(binding)
    }

    private fun fitFrame(
        videoWidth: Int,
        videoHeight: Int,
        containerWidth: Int,
        containerHeight: Int
    ): FrameLayout.LayoutParams {
        val videoAspect = videoWidth.toFloat() / videoHeight
        val containerAspect = containerWidth.toFloat() / containerHeight

        val newWidth: Int
        val newHeight: Int

        if (videoAspect > containerAspect) {
            newWidth = containerWidth
            newHeight = (containerWidth / videoAspect).toInt()
        } else {
            newHeight = containerHeight
            newWidth = (containerHeight * videoAspect).toInt()
        }

        val offsetX = (containerWidth - newWidth) / 2
        val offsetY = (containerHeight - newHeight) / 2

        return FrameLayout.LayoutParams(newWidth, newHeight).apply {
            setMargins(offsetX, offsetY, 0, 0)
        }
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
    class ViewHolder(val binding: LayoutParticipantBinding) :
        RecyclerView.ViewHolder(binding.root) {

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
                    val color = ContextCompat.getColor(context, R.color.error_red)
                    microButton.setColorFilter(color)
                }
            }
        }

        fun dispose() {
            binding.renderer.clear()
        }
    }

    internal class UiVideoComparator : DiffUtil.ItemCallback<VideoElement>() {
        override fun areItemsTheSame(oldItem: VideoElement, newItem: VideoElement): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: VideoElement, newItem: VideoElement): Boolean =
            oldItem == newItem
    }
}
