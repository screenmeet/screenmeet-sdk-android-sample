package com.screenmeet.live.feature.call

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.screenmeet.live.R
import com.screenmeet.live.databinding.FragmentCallBinding
import com.screenmeet.live.util.NAVIGATION_DESTINATION
import com.screenmeet.live.util.ParticipantsGridItemDecoration
import com.screenmeet.live.util.ParticipantsHorizontalItemDecoration
import com.screenmeet.live.util.getNavigationResult
import com.screenmeet.live.util.viewBinding
import com.screenmeet.sdk.Participant
import com.screenmeet.sdk.ScreenMeet
import com.screenmeet.sdk.SessionEventListener
import com.screenmeet.sdk.VideoElement
import com.screenmeet.sdk.domain.entity.ChatMessage
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.RendererCommon

@AndroidEntryPoint
class CallFragment : Fragment(R.layout.fragment_call) {

    private val binding by viewBinding(FragmentCallBinding::bind)
    private val viewModel by viewModels<CallViewModel>()

    private var permissionLauncher: ActivityResultLauncher<String>? = null
    private lateinit var participantsAdapter: ParticipantsAdapter
    private lateinit var eglBase: EglBase.Context

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpView()
        observeState()

        getNavigationResult<Int?>(R.id.fragmentVideoCall, NAVIGATION_DESTINATION) { dest ->
            dest?.let {
                viewModel.navigate(it)
            }
        }
    }

    private fun setUpView() {
        // The same eglContext as a capturing one should be used to preview camera
        eglBase = ScreenMeet.eglContext!!
        participantsAdapter = ParticipantsAdapter(
            scope = lifecycleScope,
            eglBase = eglBase,
            onClick = { video ->
                viewModel.pinVideo(video)
            }
        )

        binding.apply {
            root.applyInsetter {
                type(statusBars = true, navigationBars = true, displayCutout = true) { padding() }
            }

            more.setOnClickListener {
                viewModel.openMore()
            }

            activeStreamRenderer.zoomRenderer.init(
                eglBase,
                object : RendererCommon.RendererEvents {
                    override fun onFirstFrameRendered() {}

                    override fun onFrameResolutionChanged(width: Int, height: Int, i2: Int) {
                        Handler(Looper.getMainLooper()).post {
                            binding.activeStreamRenderer.zoomRenderer.let {
                                val layoutParams = it.layoutParams
                                layoutParams.width = width
                                layoutParams.height = height
                                binding.activeStreamRenderer.zoomContainer.updateViewLayout(
                                    it,
                                    layoutParams
                                )
                                if (width > 0 && height > 0) {
                                    binding.activeStreamRenderer.zoomContainer.isVisible = true
                                }
                            }
                        }
                    }
                }
            )

            activeStreamRenderer.zoomRenderer.listenFramesStuck(lifecycleScope) { stuck ->
                binding.activeStreamRenderer.frameStuckSpinner.isVisible = stuck
            }
            participantsRecycler.adapter = participantsAdapter

            setButtonBackgroundColor(hangUp, R.color.bright_red)

            val openControlsListener: (v: View) -> Unit = {
                viewModel.controlsVisible(true)
            }
            container.setOnClickListener(openControlsListener)
            participantsRecycler.setOnClickListener(openControlsListener)
            activeStreamRenderer.root.setOnClickListener(openControlsListener)
            activeStreamRenderer.zoomRenderer.setOnClickListener(openControlsListener)
            hangUp.setOnClickListener {
                participantsAdapter.dispose(binding.participantsRecycler)
                binding.activeStreamRenderer.zoomRenderer.clear()
                ScreenMeet.disconnect()
            }
        }
    }

    private fun observeState() {
        observeParticipants()

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.activeSpeaker.collect { activeTrackId ->
                    switchActiveSpeakerTrack(activeTrackId)
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
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    delay(2000)
                    checkPermission(requireContext())
                }
            }
        }
    }

    private fun observeParticipants() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.participants.collect { participants ->
                    val hasActiveStream = viewModel.activeSpeaker.value != null
                    binding.activeStreamRenderer.root.isVisible = hasActiveStream
                    if (hasActiveStream) {
                        displayActiveSpeakerLayout(participants)
                    } else {
                        displayGridLayout(participants)
                    }
                    loadState()
                }
            }
        }
    }

    private fun switchActiveSpeakerTrack(activeTrackId: String?) {
        val context = binding.root.context
        val hasActiveStream = activeTrackId != null
        binding.activeStreamRenderer.root.isVisible = hasActiveStream
        repeat(binding.participantsRecycler.itemDecorationCount) {
            binding.participantsRecycler.removeItemDecorationAt(it)
        }
        if (hasActiveStream) {
            val linearLayoutManager = LinearLayoutManager(
                context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            binding.participantsRecycler.layoutManager = linearLayoutManager
            val linearItemDecoration = ParticipantsHorizontalItemDecoration(30)
            binding.participantsRecycler.addItemDecoration(linearItemDecoration)
        } else {
            binding.participantsRecycler.layoutManager = GridLayoutManager(context, 2)
            val gridItemDecoration = ParticipantsGridItemDecoration(2, 30)
            binding.participantsRecycler.addItemDecoration(gridItemDecoration)
        }
        loadState()
    }

    private fun displayActiveSpeakerLayout(participants: List<VideoElement>) {
        dispatchParticipants(participants)
    }

    private fun displayGridLayout(participants: List<VideoElement>) {
        dispatchParticipants(participants)
    }

    private fun dispatchParticipants(participants: List<VideoElement>) {
        participantsAdapter.submitList(participants)
    }

    private fun loadState() {
        val uiVideos = ScreenMeet.uiVideos(includeLocal = true)
        val activeSpeakerId = viewModel.activeSpeaker.value
        val activeVideo = uiVideos.firstOrNull { it.id == activeSpeakerId }
        if (activeVideo != null) {
            val participantsVideos = uiVideos.filter { it.id != activeSpeakerId }
            binding.apply {
                activeStreamRenderer.nameTv.text = activeVideo.userName
                val context = binding.root.context
                val pinColor = ContextCompat.getColor(context, R.color.enabled_button)
                activeStreamRenderer.pinButton.setColorFilter(pinColor)
                activeStreamRenderer.pinButton.setOnClickListener {
                    viewModel.pinVideo(null)
                }

                if (activeVideo.isAudioSharing) {
                    activeStreamRenderer.microButton.setImageResource(R.drawable.mic)
                    activeStreamRenderer.microButton.backgroundTintList = null
                    activeStreamRenderer.microButton.colorFilter = null
                } else {
                    activeStreamRenderer.microButton.setImageResource(R.drawable.mic_off)
                    val color = ContextCompat.getColor(context, R.color.bright_red)
                    activeStreamRenderer.microButton.setColorFilter(color)
                }

                val videoTrack = activeVideo.track
                val hasTrack = videoTrack != null
                activeStreamRenderer.zoomRenderer.render(videoTrack)
                binding.activeStreamRenderer.logo.isVisible = !hasTrack
                if (!hasTrack) {
                    binding.activeStreamRenderer.zoomContainer.isVisible = false
                }
            }
            viewModel.updateParticipants(participantsVideos)
        } else {
            viewModel.updateParticipants(uiVideos)
            viewModel.pinVideo(null)
            binding.activeStreamRenderer.zoomRenderer.render(null)
            binding.activeStreamRenderer.zoomContainer.isVisible = false
        }
    }

    private fun setButtonBackgroundColor(button: ImageButton, colorRes: Int) {
        val background = button.background as GradientDrawable
        background.setColor(ContextCompat.getColor(button.context, colorRes))
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

    override fun onResume() {
        super.onResume()
        ScreenMeet.registerEventListener(eventListener)
        loadState()
    }

    override fun onPause() {
        super.onPause()
        ScreenMeet.unregisterEventListener(eventListener)
        if (ScreenMeet.connectionState() !is ScreenMeet.ConnectionState.Disconnected) {
            participantsAdapter.dispose(binding.participantsRecycler)
            binding.activeStreamRenderer.zoomRenderer.clear()
        }
        participantsAdapter.submitList(listOf())
    }

    private val eventListener: SessionEventListener = object : SessionEventListener {
        override fun onParticipantJoined(participant: Participant) {
            loadState()
        }

        override fun onParticipantLeft(participant: Participant) {
            loadState()
        }

        override fun onLocalVideoCreated(source: ScreenMeet.VideoSource, video: VideoElement) {
            loadState()
        }

        override fun onLocalVideoStopped(source: ScreenMeet.VideoSource) {
            loadState()
        }

        override fun onLocalAudioCreated() {
            loadState()
        }

        override fun onLocalAudioStopped() {
            loadState()
        }

        override fun onParticipantAudioCreated(participant: Participant) {
            loadState()
        }

        override fun onParticipantAudioStopped(participant: Participant) {
            loadState()
        }

        override fun onParticipantVideoCreated(participant: Participant, video: VideoElement) {
            loadState()
        }

        override fun onParticipantVideoStopped(
            participant: Participant,
            source: ScreenMeet.VideoSource
        ) {
            loadState()
        }

        override fun onConnectionStateChanged(newState: ScreenMeet.ConnectionState) {
            when (newState) {
                is ScreenMeet.ConnectionState.Connected -> loadState()
                is ScreenMeet.ConnectionState.Reconnecting -> viewModel.pinVideo(null)
                is ScreenMeet.ConnectionState.Disconnected -> {
                    participantsAdapter.dispose(binding.participantsRecycler)
                    binding.activeStreamRenderer.zoomRenderer.clear()
                }
                else -> {}
            }
        }

        override fun onActiveSpeakerChanged(participant: Participant, video: VideoElement) {
            super.onActiveSpeakerChanged(participant, video)
            viewModel.pinVideo(video)
        }

        override fun onChatMessage(chatMessage: ChatMessage) {
            viewModel.receivedMessage(chatMessage)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkPermission(
        context: Context,
        permission: String = Manifest.permission.POST_NOTIFICATIONS
    ) {
        val selfPermission = ContextCompat.checkSelfPermission(context, permission)
        when {
            selfPermission == PackageManager.PERMISSION_GRANTED -> return
            shouldShowRequestPermissionRationale(permission) -> {
                showNotificationsRationale(permission)
            }
            else -> permissionLauncher?.launch(permission)
        }
    }

    private fun registerForPermissionResult() {
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            val atLeastTiramisu = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            if (!isGranted && atLeastTiramisu) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    showNotificationsRationale(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun showNotificationsRationale(permission: String) {
        AlertDialog.Builder(requireContext(), R.style.RoundedDialog).apply {
            setTitle(R.string.app_name)
            setMessage(R.string.notifications_rationale)
            setCancelable(true)
            setPositiveButton(R.string.allow) { _, _ ->
                permissionLauncher?.launch(permission)
            }
            setNegativeButton(R.string.deny) { _, _ ->
            }
            show()
        }
    }

    init {
        registerForPermissionResult()
    }
}
