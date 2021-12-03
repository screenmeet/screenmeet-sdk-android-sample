package com.screenmeet.sdkdemo.feature.call

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.screenmeet.sdk.Identity
import com.screenmeet.sdk.Participant
import com.screenmeet.sdk.ScreenMeet
import com.screenmeet.sdk.SessionEventListener
import com.screenmeet.sdk.domain.entity.ChatMessage
import com.screenmeet.sdkdemo.R
import com.screenmeet.sdkdemo.databinding.FragmentCallBinding
import com.screenmeet.sdkdemo.util.NavigationDispatcher
import com.screenmeet.sdkdemo.util.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import org.webrtc.EglBase
import org.webrtc.RendererCommon.RendererEvents
import org.webrtc.VideoFrame
import org.webrtc.VideoSink
import org.webrtc.VideoTrack
import javax.inject.Inject

@AndroidEntryPoint
class CallFragment: Fragment(R.layout.fragment_call) {

    @Inject
    lateinit var navigationDispatcher: NavigationDispatcher

    private var participantsAdapter: ParticipantsAdapter? = null
    private var eglBase: EglBase.Context? = null
    private var localVideoTrack: VideoTrack? = null

    private val binding by viewBinding(FragmentCallBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //The same eglContext as a capturing one should be used to preview camera
        eglBase = ScreenMeet.eglContext

        binding.apply {
            localRenderer.surfaceViewRenderer.init(eglBase, null)
            activeSpeakerRenderer.renderer.setZOrderOnTop(false)
            activeSpeakerRenderer.renderer.init(eglBase, object : RendererEvents {
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
            })
            localRenderer.surfaceViewRenderer.setZOrderMediaOverlay(true)
            localRenderer.surfaceViewRenderer.setZOrderOnTop(true)

            chat.setOnClickListener { navigationDispatcher.emit { it.navigate(R.id.goChat) } }

            buttonSidebar.setOnClickListener {
                if (!participantsRecycler.isVisible) {
                    buttonSidebar.setImageResource(R.drawable.ic_arrow_right)
                    participantsRecycler.isVisible = true
                } else {
                    buttonSidebar.setImageResource(R.drawable.ic_arrow_left)
                    participantsRecycler.isVisible = false
                }
            }
        }
    }

    private val eventListener: SessionEventListener = object : SessionEventListener {
        override fun onParticipantJoined(participant: Participant) {
            participantsAdapter?.let {
                it.add(participant)
                if (!it.activeSpeakerPresent()) {
                    switchActiveSpeaker(participant)
                }
            }
        }

        override fun onParticipantLeft(participant: Participant) {
            participantsAdapter?.let {
                val wasActiveSpeaker = it.isActiveSpeaker(participant)
                it.remove(participant)
                if (wasActiveSpeaker) {
                    if (ScreenMeet.participants().isNotEmpty()) {
                        val p = ScreenMeet.participants()[0]
                        switchActiveSpeaker(p)
                    } else activeSpeakerAbsent()
                }
            }
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
            participantsAdapter?.let {
                if (it.isActiveSpeaker(participant)) displayActiveSpeaker(participant)
                else it.update(participant)
            }
        }

        override fun onConnectionStateChanged(newState: ScreenMeet.ConnectionState) {
            when (newState.state) {
                ScreenMeet.SessionState.CONNECTED -> loadState()
                ScreenMeet.SessionState.DISCONNECTED -> sessionEnded()
                else -> {}
            }
        }

        override fun onActiveSpeakerChanged(participant: Participant) {
            switchActiveSpeaker(participant)
        }

        override fun onChatMessage(chatMessage: ChatMessage) {}
    }

    fun renderLocalVideoTrack(videoTrack: VideoTrack?) {
        if (videoTrack != null) {
            localVideoTrack?.removeSink(binding.localRenderer.surfaceViewRenderer)
            localVideoTrack?.removeSink(rotationSink)

            localVideoTrack = videoTrack
            localVideoTrack?.setEnabled(true)
            localVideoTrack?.addSink(rotationSink)
        } else {
            localVideoTrack = null
            binding.localRenderer.surfaceViewRenderer.clearImage()
        }
    }

    private val rotationSink = VideoSink { videoFrame: VideoFrame ->
        val frame = VideoFrame(videoFrame.buffer, 0, videoFrame.timestampNs)
        binding.localRenderer.surfaceViewRenderer.onFrame(frame)
    }

    override fun onResume() {
        super.onResume()

        ScreenMeet.registerEventListener(eventListener)
        binding.localRenderer.surfaceViewRenderer.clearImage()
        enableButtons()
        loadState()
    }

    override fun onPause() {
        super.onPause()
        participantsAdapter?.dispose()
        localVideoTrack?.removeSink(rotationSink)
        localVideoTrack?.removeSink(binding.localRenderer.surfaceViewRenderer)
        ScreenMeet.unregisterEventListener(eventListener)
    }

    private fun loadState() {
        val participants = ScreenMeet.participants()
        participantsAdapter?.dispose()
        participantsAdapter =
            ParticipantsAdapter(
                participants,
                eglBase
            )
        binding.participantsRecycler.adapter = participantsAdapter
        binding.participantsRecycler.layoutManager = LinearLayoutManager(binding.root.context)
        if (participants.isNotEmpty()) {
            participantsAdapter?.activeSpeaker?.let { activeSpeaker ->
                switchActiveSpeaker(activeSpeaker)
            } ?:  switchActiveSpeaker(participants[0])
        } else activeSpeakerAbsent()
        renderLocalVideoTrack(ScreenMeet.localVideoTrack())
        applyControlsState()
        setButtonBackgroundColor(binding.hangUp, R.color.bright_red)
    }

    private fun sessionEnded() {
        binding.activeSpeakerRenderer.renderer.release()
        binding.localRenderer.surfaceViewRenderer.release()
        participantsAdapter?.dispose()
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
                ScreenMeet.VideoSource.NONE -> { }
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

    private fun switchActiveSpeaker(participant: Participant) {
        val activeSpeakerCurrent = participantsAdapter!!.activeSpeaker
        activeSpeakerCurrent?.clearSinks()
        val activeSpeaker = participantsAdapter!!.updateActiveSpeaker(participant)
        activeSpeaker?.let { displayActiveSpeaker(it) }
    }

    private fun displayActiveSpeaker(participant: Participant) {
        participant.clearSinks()
        binding.activeSpeakerRenderer.nameTv.text = participant.identity.name
        binding.activeSpeakerRenderer.nameTv.isVisible = true
        binding.activeSpeakerRenderer.hostImage.isVisible = participant.identity.role == Identity.Role.HOST
        binding.activeSpeakerRenderer.microButton.isVisible = true
        if (participant.mediaState.isAudioActive) {
            binding.activeSpeakerRenderer.microButton.setImageResource(R.drawable.mic)
        } else binding.activeSpeakerRenderer.microButton.setImageResource(R.drawable.mic_off)
        binding.activeSpeakerRenderer.cameraButton.isVisible = true
        when (participant.mediaState.videoState.source) {
            ScreenMeet.VideoSource.BACK_CAMERA,
            ScreenMeet.VideoSource.FRONT_CAMERA,
            ScreenMeet.VideoSource.CUSTOM_CAMERA -> {
                binding.activeSpeakerRenderer.cameraButton.setImageResource(R.drawable.videocam)
            }
            ScreenMeet.VideoSource.SCREEN -> {
                binding.activeSpeakerRenderer.cameraButton.setImageResource(R.drawable.screenshot)
            }
            ScreenMeet.VideoSource.NONE -> {
                binding.activeSpeakerRenderer.cameraButton.setImageResource(R.drawable.videocam_off)
            }
        }
        val videoTrack = participant.videoTrack
        if (videoTrack == null) {
            participant.clearSinks()
            binding.activeSpeakerRenderer.renderer.isVisible = false
            binding.activeSpeakerRenderer.logo.isVisible = true
            binding.activeSpeakerRenderer.renderer.clearImage()
        } else updateTrack(videoTrack)
    }

    private fun activeSpeakerAbsent() {
        binding.activeSpeakerRenderer.nameTv.isVisible = false
        binding.activeSpeakerRenderer.hostImage.isVisible = false
        binding.activeSpeakerRenderer.microButton.isVisible = false
        binding.activeSpeakerRenderer.cameraButton.isVisible = false
        binding.activeSpeakerRenderer.renderer.isVisible = false
        binding.activeSpeakerRenderer.logo.isVisible = true
        binding.activeSpeakerRenderer.renderer.clearImage()
    }

    fun updateTrack(videoTrackNew: VideoTrack) {
        videoTrackNew.setEnabled(true)
        videoTrackNew.addSink(binding.activeSpeakerRenderer.renderer)
        binding.activeSpeakerRenderer.renderer.isVisible = true
        binding.activeSpeakerRenderer.logo.isVisible = false
    }
}