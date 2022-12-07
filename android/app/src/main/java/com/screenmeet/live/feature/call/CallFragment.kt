package com.screenmeet.live.feature.call

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import com.screenmeet.live.util.*
import com.screenmeet.sdk.Participant
import com.screenmeet.sdk.ScreenMeet
import com.screenmeet.sdk.SessionEventListener
import com.screenmeet.sdk.VideoElement
import com.screenmeet.sdk.domain.entity.ChatMessage
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.launch
import org.webrtc.EglBase

@AndroidEntryPoint
class CallFragment : Fragment(R.layout.fragment_call) {

    private val binding by viewBinding(FragmentCallBinding::bind)
    private val viewModel by viewModels<CallViewModel>()

    private lateinit var participantsAdapter: ParticipantsAdapter
    private lateinit var eglBase: EglBase.Context

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // The same eglContext as a capturing one should be used to preview camera
        eglBase = ScreenMeet.eglContext!!
        participantsAdapter = ParticipantsAdapter(lifecycleScope, eglBase){ video ->
            viewModel.pinVideo(video)
        }

        binding.apply {
            root.applyInsetter {
                type(statusBars = true, navigationBars = true, displayCutout = true) { padding() }
            }

            more.setOnClickListener {
                viewModel.openMore()
            }

            activeStreamRenderer.renderer.init(eglBase, null)
            activeStreamRenderer.renderer.listenFramesStuck(lifecycleScope) { stuck ->
                binding.activeStreamRenderer.frameStuckSpinner.isVisible = stuck
            }
            participantsRecycler.adapter = participantsAdapter

            setButtonBackgroundColor(hangUp, R.color.bright_red)
            container.setOnClickListener {
                viewModel.controlsVisible(true)
            }
            hangUp.setOnClickListener { ScreenMeet.disconnect() }
        }

        getNavigationResult<Int?>(R.id.fragmentVideoCall, NAVIGATION_DESTINATION){ dest ->
            dest?.let {
                viewModel.navigate(it)
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.participants.collect { participants ->
                    val hasActiveStream = viewModel.activeSpeaker.value != null
                    binding.activeStreamRenderer.root.isVisible = hasActiveStream
                    if (hasActiveStream) {
                        displayActiveSpeakerLayout(participants)
                    } else displayGridLayout(participants)
                    loadState()
                }
            }
        }
        lifecycleScope.launchWhenResumed {
            viewModel.activeSpeaker.collect { activeTrackId ->
                val context = binding.root.context
                val hasActiveStream = activeTrackId != null
                binding.activeStreamRenderer.root.isVisible = hasActiveStream
                repeat(binding.participantsRecycler.itemDecorationCount){
                    binding.participantsRecycler.removeItemDecorationAt(it)
                }
                if (hasActiveStream){
                    binding.participantsRecycler.layoutManager = LinearLayoutManager(
                        context,
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )
                    val linearItemDecoration = ParticipantsHorizontalItemDecoration(30)
                    binding.participantsRecycler.addItemDecoration(linearItemDecoration)
                } else {
                    binding.participantsRecycler.layoutManager = GridLayoutManager(context, 2)
                    val gridItemDecoration = ParticipantsGridItemDecoration(2, 30)
                    binding.participantsRecycler.addItemDecoration(gridItemDecoration)
                }
                loadState()
            }
        }
        lifecycleScope.launchWhenResumed {
            viewModel.showControls.collect { show ->
                activity?.let { act ->
                    val insetsController = WindowInsetsControllerCompat(
                        act.window,
                        act.findViewById(R.id.root)
                    )
                    if(show){
                        insetsController.show(WindowInsetsCompat.Type.systemBars())
                    } else {
                        insetsController.hide(WindowInsetsCompat.Type.systemBars())
                    }
                }

                if(show) {
                    showControls()
                } else hideControls()
            }
        }
        lifecycleScope.launchWhenResumed {
            viewModel.hasUnread.collect { hasUnread ->
                binding.unreadMark.isVisible = hasUnread
                if(hasUnread && !viewModel.showControls.value){
                    playChat()
                }
            }
        }
        lifecycleScope.launchWhenResumed {
            for (event in viewModel.eventChannel) {
                if(!viewModel.showControls.value){
                    playChat()
                }
            }
        }
    }

    private fun displayActiveSpeakerLayout(participants: List<VideoElement>){
        dispatchParticipants(participants)
    }

    private fun displayGridLayout(participants: List<VideoElement>){
        dispatchParticipants(participants)
    }

    private fun dispatchParticipants(participants: List<VideoElement>){
        participantsAdapter.submitList(participants)
    }

    private fun loadState() {
        val uiVideos = ScreenMeet.uiVideos(includeLocal = true)
        val activeSpeakerId = viewModel.activeSpeaker.value
        if (activeSpeakerId != null) {
            val activeVideo = uiVideos.firstOrNull { it.id == activeSpeakerId }
            val participantsVideos = uiVideos.filter { it.id != activeSpeakerId }
            binding.apply {
                activeStreamRenderer.nameTv.text = activeVideo?.userName
                val context = binding.root.context
                val pinColor = ContextCompat.getColor(context, R.color.enabled_button)
                activeStreamRenderer.pinButton.setColorFilter(pinColor)
                activeStreamRenderer.pinButton.setOnClickListener {
                    viewModel.pinVideo(null)
                }

                if (activeVideo?.isAudioSharing == true) {
                    activeStreamRenderer.microButton.setImageResource(R.drawable.mic)
                    activeStreamRenderer.microButton.backgroundTintList = null
                    activeStreamRenderer.microButton.colorFilter = null
                } else {
                    activeStreamRenderer.microButton.setImageResource(R.drawable.mic_off)
                    val color = ContextCompat.getColor(context, R.color.bright_red)
                    activeStreamRenderer.microButton.setColorFilter(color)
                }

                val videoTrack = activeVideo?.track
                if (videoTrack != null) {
                    activeStreamRenderer.renderer.render(videoTrack)
                } else activeStreamRenderer.renderer.clear()

                val hasTrack = videoTrack != null
                binding.activeStreamRenderer.renderer.isVisible = hasTrack
                binding.activeStreamRenderer.logo.isVisible = !hasTrack
            }
            viewModel.updateParticipants(participantsVideos)
        } else {
            viewModel.updateParticipants(uiVideos)
            binding.activeStreamRenderer.renderer.clear()
        }
    }

    private fun setButtonBackgroundColor(button: ImageButton, colorRes: Int) {
        val background = button.background as GradientDrawable
        background.setColor(ContextCompat.getColor(button.context, colorRes))
    }

    private fun showControls(){
        binding.buttonsContainer.visibility = View.VISIBLE
        binding.buttonsContainer.alpha = 0.0f

        binding.buttonsContainer.animate()
            .translationY(0f)
            .alpha(1.0f)
            .setListener(null)
    }


    private fun playChat(){
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



    private fun hideControls(){
        binding.buttonsContainer.animate()
            .translationY(binding.buttonsContainer.height.toFloat())
            .alpha(0.0f)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator, isReverse: Boolean) {
                    binding.buttonsContainer.visibility = View.GONE
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
        participantsAdapter.dispose(binding.participantsRecycler)
        participantsAdapter.submitList(listOf())
        binding.activeStreamRenderer.renderer.clear()
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
                ScreenMeet.ConnectionState.Connected -> loadState()
                ScreenMeet.ConnectionState.Reconnecting -> viewModel.pinVideo(null)
                else -> {}
            }
        }

        override fun onActiveSpeakerChanged(participant: Participant, video: VideoElement) {
            super.onActiveSpeakerChanged(participant, video)
            viewModel.pinVideo(video)
        }

        override fun onChatMessage(chatMessage: ChatMessage) {
            viewModel.receivedMessage()
        }
    }
}
