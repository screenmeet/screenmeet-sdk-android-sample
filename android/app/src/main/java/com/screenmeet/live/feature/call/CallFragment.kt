package com.screenmeet.live.feature.call

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.screenmeet.live.R
import com.screenmeet.live.databinding.FragmentCallBinding
import com.screenmeet.live.util.NAVIGATION_DESTINATION
import com.screenmeet.live.util.ParticipantsGridItemDecoration
import com.screenmeet.live.util.ParticipantsHorizontalItemDecoration
import com.screenmeet.live.util.getNavigationResult
import com.screenmeet.live.util.setButtonBackgroundColor
import com.screenmeet.live.util.tryOrNull
import com.screenmeet.live.util.viewBinding
import com.screenmeet.sdk.Feature
import com.screenmeet.sdk.Participant
import com.screenmeet.sdk.ScreenMeet
import com.screenmeet.sdk.ScreenMeet.VideoSource
import com.screenmeet.sdk.SessionEventListener
import com.screenmeet.sdk.VideoElement
import com.screenmeet.sdk.domain.entity.ChatMessage
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.RendererCommon

@AndroidEntryPoint
class CallFragment : Fragment(R.layout.fragment_call) {

    private val binding by viewBinding(FragmentCallBinding::bind)
    private val viewModel by viewModels<CallViewModel>()

    private lateinit var participantsAdapter: ParticipantsAdapter
    private lateinit var eglBase: EglBase.Context

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpView()
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.state.collect { state ->
                    updateParticipants(state.second)
                    switchActiveSpeaker(state.first)
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.showControls.collect { show ->
                    if (show) {
                        showControls()
                    } else {
                        hideControls()
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.hasUnread.collect { hasUnread ->
                    binding.unreadMark.isVisible = hasUnread
                    if (hasUnread && !viewModel.showControls.value) {
                        playChat()
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                for (event in viewModel.eventChannel) {
                    if (!viewModel.showControls.value) {
                        playChat()
                    }
                }
            }
        }

        getNavigationResult<Int?>(R.id.fragmentVideoCall, NAVIGATION_DESTINATION) { dest ->
            dest?.let {
                viewModel.navigate(it)
            }
        }
    }

    private fun setUpView() {
        eglBase = ScreenMeet.eglContext!!
        binding.apply {
            val context = binding.root.context
            root.applyInsetter {
                type(statusBars = true, navigationBars = true, displayCutout = true) { padding() }
            }

            participantsAdapter = ParticipantsAdapter(
                scope = lifecycleScope,
                eglBase = eglBase,
                onClick = viewModel::pinVideo
            )
            participantsRecycler.adapter = participantsAdapter

            val pinColor = ContextCompat.getColor(context, R.color.enabled_button)
            activeStreamRenderer.pinButton.setColorFilter(pinColor)
            activeStreamRenderer.pinButton.setOnClickListener {
                viewModel.pinVideo(null)
            }
            activeStreamRenderer.zoomRenderer.init(
                eglBase,
                object : RendererCommon.RendererEvents {

                    override fun onFirstFrameRendered() {}

                    override fun onFrameResolutionChanged(width: Int, height: Int, i2: Int) {
                        Handler(Looper.getMainLooper()).post {
                            activeStreamRenderer.zoomRenderer.let {
                                val layoutParams = it.layoutParams
                                layoutParams.width = width
                                layoutParams.height = height
                                activeStreamRenderer.zoomContainer.updateViewLayout(
                                    it,
                                    layoutParams
                                )
                                if (width > 0 && height > 0) {
                                    activeStreamRenderer.zoomContainer.isVisible = true
                                }
                            }
                        }
                    }
                }
            )
            activeStreamRenderer.zoomRenderer.listenFramesStuck(lifecycleScope) { stuck ->
                activeStreamRenderer.frameStuckSpinner.isVisible = stuck
            }

            more.setOnClickListener {
                viewModel.openMore()
            }

            hangUp.setButtonBackgroundColor(R.color.bright_red)
            hangUp.setOnClickListener {
                ScreenMeet.disconnect()
            }

            val openControlsListener: (v: View) -> Unit = {
                viewModel.controlsVisible(true)
            }
            container.setOnClickListener(openControlsListener)
            participantsRecycler.setOnClickListener(openControlsListener)
            activeStreamRenderer.root.setOnClickListener(openControlsListener)
            activeStreamRenderer.zoomRenderer.setOnClickListener(openControlsListener)
        }
    }

    private fun loadState() {
        val videos = ScreenMeet.uiVideos(includeLocal = true)
        val activeSpeaker = ScreenMeet.currentActiveSpeaker()

        viewModel.updateParticipants(videos)
        viewModel.markActiveSpeaker(activeSpeaker)
    }

    private fun updateParticipants(participants: List<VideoElement>) {
        binding.participantsRecycler.isVisible = participants.isNotEmpty()
        participantsAdapter.submitList(participants)
    }

    private fun switchActiveSpeaker(videoElement: VideoElement?) {
        binding.apply {
            val context = root.context
            val hasActiveStream = videoElement != null
            val hasTrack = videoElement?.track != null
            val isGridLayout = participantsRecycler.layoutManager is GridLayoutManager
            val isLinearLayout = participantsRecycler.layoutManager is LinearLayoutManager
            val needsNewLayout = participantsRecycler.layoutManager == null ||
                !hasActiveStream && isLinearLayout || hasActiveStream && isGridLayout

            activeStreamRenderer.root.isVisible = hasActiveStream
            if (needsNewLayout) {
                repeat(participantsRecycler.itemDecorationCount) {
                    participantsRecycler.removeItemDecorationAt(it)
                }
                val itemSpacing = participantsAdapter.itemSpacing
                if (hasActiveStream) {
                    val linearLayoutManager = LinearLayoutManager(
                        context,
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )
                    val linearItemDecoration = ParticipantsHorizontalItemDecoration(itemSpacing)
                    participantsRecycler.layoutManager = linearLayoutManager
                    participantsRecycler.addItemDecoration(linearItemDecoration)
                } else {
                    val gridLayoutManager = GridLayoutManager(context, 2)
                    val gridItemDecoration = ParticipantsGridItemDecoration(2, itemSpacing)
                    participantsRecycler.layoutManager = gridLayoutManager
                    participantsRecycler.addItemDecoration(gridItemDecoration)
                }
            }

            activeStreamRenderer.logo.isVisible = !hasTrack
            activeStreamRenderer.nameTv.text = videoElement?.userName
            activeStreamRenderer.zoomRenderer.render(videoElement?.track)
            if (!hasTrack) {
                activeStreamRenderer.frameStuckSpinner.isVisible = false
                activeStreamRenderer.zoomContainer.isVisible = false
            }

            if (videoElement?.isAudioSharing == true) {
                activeStreamRenderer.microButton.setImageResource(R.drawable.mic)
                activeStreamRenderer.microButton.backgroundTintList = null
                activeStreamRenderer.microButton.colorFilter = null
            } else {
                activeStreamRenderer.microButton.setImageResource(R.drawable.mic_off)
                val color = ContextCompat.getColor(context, R.color.bright_red)
                activeStreamRenderer.microButton.setColorFilter(color)
            }
        }
    }

    private fun showControls() {
        binding.buttonsContainer.visibility = View.VISIBLE
        binding.buttonsContainer.alpha = 0.0f

        binding.buttonsContainer.animate()
            .translationY(0f)
            .alpha(1.0f)
            .setListener(null)
    }

    private fun hideControls() {
        binding.buttonsContainer.animate()
            .translationY(binding.buttonsContainer.height.toFloat())
            .alpha(0.0f)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator, isReverse: Boolean) {
                    binding.buttonsContainer.visibility = View.GONE
                }
            })
    }

    private fun playChat() {
        binding.buttonsContainer.visibility = View.VISIBLE
        binding.buttonsContainer.alpha = 0.0f
        binding.hangUp.visibility = View.INVISIBLE
        binding.buttonsContainer.animate()
            .translationY(0f)
            .alpha(1.0f)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.buttonsContainer.animate()
                        .translationY(binding.buttonsContainer.height.toFloat())
                        .alpha(0.0f)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                binding.hangUp.visibility = View.VISIBLE
                                binding.buttonsContainer.visibility = View.GONE
                            }
                        })
                }
            })
    }

    private val eventListener = object : SessionEventListener {

        override fun onConnectionStateChanged(newState: ScreenMeet.ConnectionState) {
            if (newState is ScreenMeet.ConnectionState.Connected) {
                loadState()
            }
        }

        override fun onParticipantJoined(participant: Participant) {
            viewModel.participantJoined(participant)
        }

        override fun onParticipantLeft(participant: Participant) {
            viewModel.participantLeft(participant)
        }

        override fun onParticipantAudioCreated(participant: Participant) {
            viewModel.participantUpdated(participant)
        }

        override fun onParticipantAudioStopped(participant: Participant) {
            viewModel.participantUpdated(participant)
        }

        override fun onParticipantVideoCreated(participant: Participant, video: VideoElement) {
            viewModel.participantUpdated(participant)
        }

        override fun onParticipantVideoStopped(participant: Participant, source: VideoSource) {
            viewModel.participantUpdated(participant)
        }

        override fun onLocalAudioCreated() {
            val localParticipant = ScreenMeet.localParticipant()
            viewModel.participantUpdated(localParticipant)
        }

        override fun onLocalAudioStopped() {
            val localParticipant = ScreenMeet.localParticipant()
            viewModel.participantUpdated(localParticipant)
        }

        override fun onLocalVideoCreated(source: VideoSource, video: VideoElement) {
            val localParticipant = ScreenMeet.localParticipant()
            viewModel.participantUpdated(localParticipant)
        }

        override fun onLocalVideoStopped(source: VideoSource) {
            val localParticipant = ScreenMeet.localParticipant()
            viewModel.participantUpdated(localParticipant)
        }

        override fun onActiveSpeakerChanged(participant: Participant, video: VideoElement) {
            viewModel.markActiveSpeaker(video)
        }

        override fun onChatMessage(chatMessage: ChatMessage) {
            val currentBackStackEntry = tryOrNull { findNavController().currentBackStackEntry }
            if (currentBackStackEntry?.destination?.id != R.id.fragmentChat) {
                viewModel.receivedMessage(chatMessage)
            }
        }

        override fun onFeatureRequest(
            feature: Feature,
            decisionHandler: (granted: Boolean) -> Unit
        ) {
        }
    }

    override fun onResume() {
        super.onResume()
        ScreenMeet.registerEventListener(eventListener)
        loadState()
    }

    override fun onPause() {
        super.onPause()
        viewModel.updateParticipants(listOf())
        viewModel.markActiveSpeaker(null)
        ScreenMeet.unregisterEventListener(eventListener)
    }
}
