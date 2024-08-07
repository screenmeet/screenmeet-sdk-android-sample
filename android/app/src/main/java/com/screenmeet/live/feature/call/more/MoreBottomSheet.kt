package com.screenmeet.live.feature.call.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.screenmeet.live.R
import com.screenmeet.live.databinding.DialogMoreBinding
import com.screenmeet.live.tools.NAVIGATION_DESTINATION
import com.screenmeet.live.tools.NavigationDispatcher
import com.screenmeet.sdk.ScreenMeet
import com.screenmeet.sdk.ScreenMeet.VideoSource
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MoreBottomSheet : BottomSheetDialogFragment() {

    @Inject
    lateinit var navigationDispatcher: NavigationDispatcher

    private lateinit var binding: DialogMoreBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogMoreBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = binding.root.context
        binding.actionItemsRv.layoutManager = GridLayoutManager(context, 3)
        val adapter = MoreDialogAdapter(::handleActionClick)
        binding.actionItemsRv.adapter = adapter
        adapter.submitList(provideActions())
    }

    private fun provideActions(): List<MoreActionItem> {
        val items = mutableListOf<MoreActionItem>()
        val mediaState = ScreenMeet.localParticipant().mediaState

        val frontCamItem = if (mediaState.isFrontCameraSharing) {
            MoreActionItem(R.string.share_cam_front_stop, R.drawable.ic_video_camera_off)
        } else {
            MoreActionItem(R.string.share_cam_front, R.drawable.ic_video_camera_front)
        }
        items.add(frontCamItem)

        val backCamItem = if (mediaState.isBackCameraSharing) {
            MoreActionItem(R.string.share_cam_back_stop, R.drawable.ic_video_camera_off)
        } else {
            MoreActionItem(R.string.share_cam_back, R.drawable.ic_video_camera_back)
        }
        items.add(backCamItem)

        val fullScreenItem = if (mediaState.isFullScreenSharing) {
            MoreActionItem(R.string.share_screen_stop, R.drawable.ic_video_camera_off)
        } else {
            MoreActionItem(R.string.share_screen, R.drawable.screen_share)
        }
        items.add(fullScreenItem)

        val screenItem = if (mediaState.isScreenSharing) {
            MoreActionItem(R.string.share_app_stop, R.drawable.ic_video_camera_off)
        } else {
            MoreActionItem(R.string.share_app, R.drawable.screen_share)
        }
        items.add(screenItem)

        val audioItem = if (mediaState.isAudioSharing) {
            MoreActionItem(R.string.share_audio_stop, R.drawable.mic_off)
        } else {
            MoreActionItem(R.string.share_audio, R.drawable.mic)
        }
        items.add(audioItem)

        val chatItem = MoreActionItem(R.string.chat, R.drawable.ic_chat)
        items.add(chatItem)

        val peopleItem = MoreActionItem(R.string.people, R.drawable.ic_people)
        items.add(peopleItem)

        val viewConfidentiality = MoreActionItem(
            R.string.view_confidentiality,
            R.drawable.ic_security
        )
        items.add(viewConfidentiality)

        val webConfidentiality = MoreActionItem(
            R.string.web_confidentiality,
            R.drawable.ic_web_stories
        )
        items.add(webConfidentiality)

        return items.toList()
    }

    private fun handleActionClick(item: MoreActionItem) {
        when (item.text) {
            R.string.share_app -> ScreenMeet.shareScreen()
            R.string.share_app_stop -> ScreenMeet.stopVideoSharing(VideoSource.Screen)
            R.string.share_screen -> ScreenMeet.shareFullScreen()
            R.string.share_screen_stop -> ScreenMeet.stopVideoSharing(VideoSource.FullScreen())
            R.string.share_cam_front -> ScreenMeet.shareCamera(true)
            R.string.share_cam_front_stop -> ScreenMeet.stopVideoSharing(VideoSource.FrontCamera)
            R.string.share_cam_back -> ScreenMeet.shareCamera(false)
            R.string.share_cam_back_stop -> ScreenMeet.stopVideoSharing(VideoSource.BackCamera)
            R.string.share_audio -> ScreenMeet.shareAudio()
            R.string.share_audio_stop -> ScreenMeet.stopAudioSharing()
            R.string.chat -> navigate(R.id.goChat)
            R.string.people -> navigate(R.id.goPeople)
            R.string.web_confidentiality -> navigate(R.id.goWebView)
            R.string.view_confidentiality -> navigate(R.id.goConfidentiality)
        }
        back()
    }

    private fun navigate(@IdRes destination: Int) {
        navigationDispatcher.emit {
            it.previousBackStackEntry?.savedStateHandle?.set(NAVIGATION_DESTINATION, destination)
        }
    }

    private fun back() = navigationDispatcher.back()
}
