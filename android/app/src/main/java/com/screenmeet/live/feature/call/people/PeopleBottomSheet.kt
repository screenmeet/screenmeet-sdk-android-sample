package com.screenmeet.live.feature.call.people

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.screenmeet.live.R
import com.screenmeet.live.databinding.DialogPeopleBinding
import com.screenmeet.live.tools.NavigationDispatcher
import com.screenmeet.sdk.Participant
import com.screenmeet.sdk.ScreenMeet
import com.screenmeet.sdk.SessionEventListener
import com.screenmeet.sdk.VideoElement
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PeopleBottomSheet : BottomSheetDialogFragment() {

    @Inject
    lateinit var navigationDispatcher: NavigationDispatcher

    private lateinit var binding: DialogPeopleBinding

    private lateinit var adapter: PeopleAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogPeopleBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prepare()
        updateParticipants()
        ScreenMeet.registerEventListener(screenMeetListener)
    }

    private fun prepare() {
        val context = binding.root.context
        val layoutManager = LinearLayoutManager(context)
        val drawable = ContextCompat.getDrawable(context, R.drawable.divider)!!
        drawable.alpha = 60
        val decoration = DividerDecoration(drawable)
        adapter = PeopleAdapter {}

        binding.apply {
            binding.actionItemsRv.addItemDecoration(decoration)
            actionItemsRv.layoutManager = layoutManager
            binding.actionItemsRv.adapter = adapter
        }
    }

    private fun updateParticipants() {
        val participant = ScreenMeet.localParticipant()
        val participants = ScreenMeet.participants()
        adapter.submitList(participants.plus(participant))
    }

    private val screenMeetListener = object : SessionEventListener {
        override fun onLocalAudioCreated() {
            updateParticipants()
        }

        override fun onLocalAudioStopped() {
            updateParticipants()
        }

        override fun onParticipantJoined(participant: Participant) {
            updateParticipants()
        }

        override fun onParticipantLeft(participant: Participant) {
            updateParticipants()
        }

        override fun onParticipantAudioCreated(participant: Participant) {
            updateParticipants()
        }

        override fun onParticipantAudioStopped(participant: Participant) {
            updateParticipants()
        }

        override fun onParticipantVideoCreated(participant: Participant, video: VideoElement) {
            updateParticipants()
        }

        override fun onParticipantVideoStopped(
            participant: Participant,
            source: ScreenMeet.VideoSource
        ) = updateParticipants()
    }
}
