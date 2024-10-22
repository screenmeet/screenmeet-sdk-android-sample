package com.screenmeet.live.feature.call

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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
import com.screenmeet.live.tools.NAVIGATION_DESTINATION
import com.screenmeet.live.tools.ParticipantsGridItemDecoration
import com.screenmeet.live.tools.ParticipantsHorizontalItemDecoration
import com.screenmeet.live.tools.getNavigationResult
import com.screenmeet.live.tools.setButtonBackgroundColor
import com.screenmeet.live.tools.showAlert
import com.screenmeet.live.tools.tryOrNull
import com.screenmeet.live.tools.viewBinding
import com.screenmeet.sdk.Participant
import com.screenmeet.sdk.ScreenMeet
import com.screenmeet.sdk.ScreenMeet.VideoSource
import com.screenmeet.sdk.SessionEventListener
import com.screenmeet.sdk.VideoElement
import com.screenmeet.sdk.domain.entity.ChatMessage
import com.screenmeet.sdk.RemoteControlCommand
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.RendererCommon

@AndroidEntryPoint
class CallFragment : Fragment(R.layout.fragment_call) {

    private val binding by viewBinding(FragmentCallBinding::bind)
    private val viewModel by viewModels<CallViewModel>()

    private var areaNotClickableRemotely: Rect? = null

    private var notificationsPermissionLauncher: ActivityResultLauncher<String>? = null

    private lateinit var eglBase: EglBase.Context
    private lateinit var participantsAdapter: ParticipantsAdapter

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

        if (Build.VERSION.SDK_INT >= 33) {
            if (!hasNotificationPermission(binding.root.context)) {
                val contract = ActivityResultContracts.RequestPermission()
                notificationsPermissionLauncher = registerForActivityResult(contract) {
                    notificationsPermissionLauncher = null
                }
                notificationsPermissionLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    @RequiresApi(33)
    private fun hasNotificationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
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
                        activeStreamRenderer.zoomRenderer.let { renderer ->
                            renderer.post {
                                val layoutParams = renderer.layoutParams
                                layoutParams.width = width
                                layoutParams.height = height
                                activeStreamRenderer.zoomContainer.updateViewLayout(
                                    renderer,
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

            hangUp.setButtonBackgroundColor(R.color.error_red)
            hangUp.setOnClickListener {
                requireActivity().showAlert(
                    dialogTittle = "Disconnect",
                    message = "Are you sure you want to disconnect?",
                    confirmed = ScreenMeet::disconnect
                )
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
                val color = ContextCompat.getColor(context, R.color.error_red)
                activeStreamRenderer.microButton.setColorFilter(color)
            }
        }
    }

    private fun showControls() {
        binding.buttonsContainer.isVisible = true
        binding.buttonsContainer.alpha = 0.0f

        binding.buttonsContainer.animate()
            .translationY(0f)
            .alpha(1.0f)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    val rect = Rect(0, 0, 0, 0)
                    binding.hangUp.getGlobalVisibleRect(rect)
                    areaNotClickableRemotely = rect
                }
            })
    }

    private fun hideControls() {
        binding.buttonsContainer.animate()
            .translationY(binding.buttonsContainer.height.toFloat())
            .alpha(0.0f)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    areaNotClickableRemotely = null
                }
            })
    }

    private fun playChat() {
        binding.buttonsContainer.isVisible = true
        binding.buttonsContainer.alpha = 0.0f
        binding.hangUp.isVisible = false
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
                                binding.hangUp.isVisible = true
                                binding.buttonsContainer.isVisible = false
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

        override fun onScreenShareRequest(participant: Participant): Boolean {
            val context = requireActivity()
            context.showAlert(
                dialogTittle = context.getString(R.string.screenshare_request),
                message = context.getString(
                    R.string.screenshare_request_hint,
                    participant.identity.name
                ),
                confirmed = ScreenMeet::shareScreen
            )
            return true
        }

        override fun onRemoteControlCommand(command: RemoteControlCommand): Boolean {
            if (command is RemoteControlCommand.Mouse) {
                // If the click occurs in a non-clickable area, return true.
                // This indicates that the event has been handled and should not be further dispatched.
                return areaNotClickableRemotely?.contains(command.x, command.y) == true
            }
            return false
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
