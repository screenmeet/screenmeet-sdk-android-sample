package com.screenmeet.live.feature.call

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.screenmeet.live.R
import com.screenmeet.live.databinding.FragmentCallBinding
import com.screenmeet.live.util.NavigationDispatcher
import com.screenmeet.live.util.viewBinding
import com.screenmeet.sdk.Identity
import com.screenmeet.sdk.Participant
import com.screenmeet.sdk.ScreenMeet
import com.screenmeet.sdk.SessionEventListener
import com.screenmeet.sdk.domain.entity.ChatMessage
import dagger.hilt.android.AndroidEntryPoint
import org.webrtc.EglBase
import org.webrtc.RendererCommon.RendererEvents
import org.webrtc.VideoFrame
import org.webrtc.VideoSink
import org.webrtc.VideoTrack
import javax.inject.Inject

@AndroidEntryPoint
class CallFragment : Fragment(R.layout.fragment_call) {

    @Inject
    lateinit var navigationDispatcher: NavigationDispatcher

    private lateinit var participantsAdapter: ParticipantsAdapter
    private lateinit var eglBase: EglBase.Context

    private var localVideoTrack: VideoTrack? = null
    private var activeParticipant: Participant? = null

    private val binding by viewBinding(FragmentCallBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // The same eglContext as a capturing one should be used to preview camera
        eglBase = ScreenMeet.eglContext!!
        participantsAdapter = ParticipantsAdapter(eglBase)

        binding.apply {
            localRenderer.renderer.init(eglBase, null)
            localRenderer.renderer.setZOrderMediaOverlay(true)
            localRenderer.renderer.setZOrderOnTop(true)

            activeSpeakerRenderer.zoomView.setHasClickableChildren(true)
            activeSpeakerRenderer.renderer.setZOrderOnTop(false)
            activeSpeakerRenderer.renderer.init(
                eglBase,
                object : RendererEvents {
                    override fun onFirstFrameRendered() {}

                    override fun onFrameResolutionChanged(width: Int, height: Int, i2: Int) {
                        Handler(Looper.getMainLooper()).post {
                            val layoutParams = activeSpeakerRenderer.renderer.layoutParams
                            layoutParams.width = width
                            layoutParams.height = height
                            activeSpeakerRenderer.zoomView.updateViewLayout(
                                activeSpeakerRenderer.renderer, layoutParams
                            )
                        }
                    }
                }
            )

            participantsRecycler.adapter = participantsAdapter
            participantsRecycler.layoutManager = LinearLayoutManager(root.context)

            chat.setOnClickListener { navigationDispatcher.emit { it.navigate(R.id.goChat) } }

            buttonSidebar.setOnClickListener {
                if (!participantsRecycler.isVisible) {
                    buttonSidebar.setImageResource(R.drawable.ic_arrow_right)
                    participantsRecycler.isVisible = true
                    displayParticipants(ScreenMeet.participants())
                } else {
                    buttonSidebar.setImageResource(R.drawable.ic_arrow_left)
                    participantsRecycler.isVisible = false
                    participantsAdapter.dispose(participantsRecycler)
                    participantsAdapter.submitList(listOf())
                }
            }
            setButtonBackgroundColor(hangUp, R.color.bright_red)
        }
    }

    private val eventListener: SessionEventListener = object : SessionEventListener {
        override fun onParticipantJoined(participant: Participant) {
            displayParticipants(ScreenMeet.participants())
        }

        override fun onParticipantLeft(participant: Participant) {
            displayParticipants(ScreenMeet.participants())
        }

        override fun onLocalVideoCreated(videoTrack: VideoTrack) {
            applyControlsState()
            renderLocalVideoTrack(videoTrack)
        }

        override fun onLocalVideoStopped() {
            applyControlsState()
            renderLocalVideoTrack(null)
        }

        override fun onLocalAudioCreated() {
            applyControlsState()
        }

        override fun onLocalAudioStopped() {
            applyControlsState()
        }

        override fun onParticipantMediaStateChanged(participant: Participant) {
            displayParticipants(ScreenMeet.participants())
        }

        override fun onConnectionStateChanged(newState: ScreenMeet.ConnectionState) {
            when (newState.state) {
                ScreenMeet.SessionState.CONNECTED -> loadState()
                ScreenMeet.SessionState.DISCONNECTED -> sessionEnded()
                else -> {}
            }
        }

        override fun onActiveSpeakerChanged(participant: Participant) {
            displayParticipants(ScreenMeet.participants())
        }

        override fun onChatMessage(chatMessage: ChatMessage) {}
    }

    fun renderLocalVideoTrack(videoTrack: VideoTrack?) {
        if (videoTrack != null) {
            localVideoTrack = videoTrack
            localVideoTrack?.addSink(rotationSink)
        } else {
            binding.localRenderer.renderer.clearImage()
            localVideoTrack = null
        }
    }

    private val rotationSink = VideoSink { videoFrame: VideoFrame ->
        val frame = VideoFrame(videoFrame.buffer, 0, videoFrame.timestampNs)
        binding.localRenderer.renderer.onFrame(frame)
    }

    override fun onResume() {
        super.onResume()

        ScreenMeet.registerEventListener(eventListener)
        binding.localRenderer.renderer.clearImage()
        enableButtons()
        loadState()
    }

    override fun onPause() {
        super.onPause()
        participantsAdapter.dispose(binding.participantsRecycler)
        localVideoTrack?.removeSink(rotationSink)
        ScreenMeet.unregisterEventListener(eventListener)
    }

    private fun displayParticipants(participants: List<Participant>) {
        if (participants.isNotEmpty()) {
            activeParticipant = participants.firstOrNull { it.id == activeParticipant?.id }
            if (activeParticipant == null) {
                activeParticipant = participants.first()
            }
            displayActiveSpeaker(activeParticipant!!)
            participantsAdapter.submitList(participants.filter { it.id != activeParticipant?.id })
        } else {
            activeParticipant = null
            activeSpeakerAbsent()
            participantsAdapter.submitList(listOf())
        }
    }

    private fun loadState() {
        val participants = ScreenMeet.participants()
        displayParticipants(participants)
        val localVideoTrack = ScreenMeet.localVideoTrack()
        renderLocalVideoTrack(localVideoTrack)

        applyControlsState()
    }

    private fun sessionEnded() {
        renderLocalVideoTrack(null)
        binding.localRenderer.renderer.release()
        binding.activeSpeakerRenderer.renderer.release()
        participantsAdapter.dispose(binding.participantsRecycler)
    }

    private fun enableButtons() {
        binding.micro.setOnClickListener {
            switchButton(binding.micro, pending = true, enabled = true)
            if (ScreenMeet.localMediaState().isAudioActive) {
                ScreenMeet.stopAudioSharing()
            } else ScreenMeet.shareAudio()
        }
        binding.camera.setOnClickListener {
            switchButton(binding.camera, pending = true, enabled = true)
            when (ScreenMeet.localMediaState().videoState.source) {
                ScreenMeet.VideoSource.BACK_CAMERA,
                ScreenMeet.VideoSource.FRONT_CAMERA,
                ScreenMeet.VideoSource.CUSTOM_CAMERA -> ScreenMeet.stopVideoSharing()
                ScreenMeet.VideoSource.SCREEN,
                ScreenMeet.VideoSource.NONE -> ScreenMeet.shareCamera(true)
            }
        }
        binding.cameraSwitch.setOnClickListener {
            switchButton(binding.cameraSwitch, pending = true, enabled = true)
            when (ScreenMeet.localMediaState().videoState.source) {
                ScreenMeet.VideoSource.BACK_CAMERA -> ScreenMeet.shareCamera(true)
                ScreenMeet.VideoSource.FRONT_CAMERA -> ScreenMeet.shareCamera(false)
                ScreenMeet.VideoSource.CUSTOM_CAMERA,
                ScreenMeet.VideoSource.SCREEN,
                ScreenMeet.VideoSource.NONE -> {
                }
            }
        }
        binding.screen.setOnClickListener {
            switchButton(binding.screen, pending = true, enabled = true)
            when (ScreenMeet.localMediaState().videoState.source) {
                ScreenMeet.VideoSource.BACK_CAMERA,
                ScreenMeet.VideoSource.FRONT_CAMERA,
                ScreenMeet.VideoSource.CUSTOM_CAMERA,
                ScreenMeet.VideoSource.NONE -> ScreenMeet.shareScreen()
                ScreenMeet.VideoSource.SCREEN -> ScreenMeet.stopVideoSharing()
            }
        }
        binding.hangUp.setOnClickListener { ScreenMeet.disconnect() }
    }

    private fun applyControlsState() {
        when (ScreenMeet.localMediaState().videoState.source) {
            ScreenMeet.VideoSource.SCREEN -> {
                switchButton(binding.screen, pending = false, enabled = true)
                switchButton(binding.camera, pending = false, enabled = false)
                binding.cameraSwitch.isVisible = false
                binding.camera.setImageResource(R.drawable.videocam_off)
                binding.localRenderer.cameraButton.setImageResource(R.drawable.screenshot)
            }
            ScreenMeet.VideoSource.FRONT_CAMERA, ScreenMeet.VideoSource.BACK_CAMERA -> {
                switchButton(binding.screen, pending = false, enabled = false)
                switchButton(binding.camera, pending = false, enabled = true)
                switchButton(binding.cameraSwitch, pending = false, enabled = true)
                binding.cameraSwitch.isVisible = true
                binding.camera.setImageResource(R.drawable.videocam)
                binding.localRenderer.cameraButton.setImageResource(R.drawable.videocam)
            }
            ScreenMeet.VideoSource.CUSTOM_CAMERA -> {
                switchButton(binding.screen, pending = false, enabled = false)
                switchButton(binding.camera, pending = false, enabled = true)
                switchButton(binding.cameraSwitch, pending = false, enabled = true)
                binding.cameraSwitch.isVisible = false
                binding.camera.setImageResource(R.drawable.videocam)
                binding.localRenderer.cameraButton.setImageResource(R.drawable.videocam)
            }
            ScreenMeet.VideoSource.NONE -> {
                switchButton(binding.camera, pending = false, enabled = false)
                switchButton(binding.screen, pending = false, enabled = false)
                binding.cameraSwitch.isVisible = false
                binding.camera.setImageResource(R.drawable.videocam_off)
                binding.localRenderer.cameraButton.setImageResource(R.drawable.videocam_off)
            }
        }
        val audioActive = ScreenMeet.localMediaState().isAudioActive
        switchButton(binding.micro, false, audioActive)
        if (audioActive) {
            binding.micro.setImageResource(R.drawable.mic)
            binding.localRenderer.microButton.setImageResource(R.drawable.mic)
        } else {
            binding.micro.setImageResource(R.drawable.mic_off)
            binding.localRenderer.microButton.setImageResource(R.drawable.mic_off)
        }
    }

    private fun displayActiveSpeaker(participant: Participant) {
        binding.apply {
            activeSpeakerRenderer.nameTv.text = participant.identity.name
            activeSpeakerRenderer.nameTv.isVisible = true
            activeSpeakerRenderer.hostImage.isVisible =
                participant.identity.role == Identity.Role.HOST
            activeSpeakerRenderer.microButton.isVisible = true

            if (participant.mediaState.isAudioActive) {
                activeSpeakerRenderer.microButton.setImageResource(R.drawable.mic)
            } else activeSpeakerRenderer.microButton.setImageResource(R.drawable.mic_off)
            activeSpeakerRenderer.cameraButton.isVisible = true

            when (participant.mediaState.videoState.source) {
                ScreenMeet.VideoSource.BACK_CAMERA,
                ScreenMeet.VideoSource.FRONT_CAMERA,
                ScreenMeet.VideoSource.CUSTOM_CAMERA -> {
                    activeSpeakerRenderer.cameraButton.setImageResource(R.drawable.videocam)
                }
                ScreenMeet.VideoSource.SCREEN -> {
                    activeSpeakerRenderer.cameraButton.setImageResource(R.drawable.screenshot)
                }
                ScreenMeet.VideoSource.NONE -> {
                    activeSpeakerRenderer.cameraButton.setImageResource(R.drawable.videocam_off)
                }
            }

            updateTrack(participant.videoTrack)
        }
    }

    private fun activeSpeakerAbsent() {
        binding.apply {
            activeSpeakerRenderer.nameTv.isVisible = false
            activeSpeakerRenderer.hostImage.isVisible = false
            activeSpeakerRenderer.microButton.isVisible = false
            activeSpeakerRenderer.cameraButton.isVisible = false
            activeSpeakerRenderer.renderer.isVisible = false
            activeSpeakerRenderer.logo.isVisible = true
            activeSpeakerRenderer.renderer.clearImage()
        }
    }

    private fun updateTrack(videoTrack: VideoTrack?) {
        binding.apply {
            if (videoTrack == null) {
                activeSpeakerRenderer.renderer.clearImage()
                activeSpeakerRenderer.renderer.isVisible = false
                activeSpeakerRenderer.logo.isVisible = true
            } else {
                binding.activeSpeakerRenderer.renderer.isVisible = true
                binding.activeSpeakerRenderer.logo.isVisible = false
                videoTrack.addSink(binding.activeSpeakerRenderer.renderer)
            }
        }
    }

    private fun switchButton(button: ImageButton, pending: Boolean, enabled: Boolean) {
        if (pending) {
            button.isEnabled = false
            setButtonBackgroundColor(button, R.color.loading_button)
        } else {
            if (enabled) setButtonBackgroundColor(
                button,
                R.color.enabled_button
            ) else setButtonBackgroundColor(button, R.color.disabled_button)
            button.isEnabled = true
        }
    }

    private fun setButtonBackgroundColor(button: ImageButton, colorRes: Int) {
        val background = button.background as GradientDrawable
        background.setColor(resources.getColor(colorRes))
    }

    override fun onStop() {
        super.onStop()
        binding.localRenderer.renderer.release()
        binding.activeSpeakerRenderer.renderer.release()
    }
}
