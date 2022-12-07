package com.screenmeet.live.util

import android.util.Log
import com.screenmeet.sdk.*
import com.screenmeet.sdk.domain.entity.ChatMessage

class LogsDebugListener: SessionEventListener {
    override fun onParticipantJoined(participant: Participant) {
        Log.d("SessionEventDebug", "onParticipantJoined $participant")
    }

    override fun onParticipantLeft(participant: Participant) {
        Log.d("SessionEventDebug", "onParticipantLeft $participant")
    }

    override fun onParticipantAudioCreated(participant: Participant) {
        Log.d("SessionEventDebug", "onParticipantAudioCreated $participant")
    }

    override fun onParticipantAudioStopped(participant: Participant) {
        Log.d("SessionEventDebug", "onParticipantAudioStopped $participant")
    }

    override fun onParticipantVideoCreated(participant: Participant, video: VideoElement) {
        Log.d("SessionEventDebug", "onParticipantVideoCreated $video $participant")
    }

    override fun onParticipantVideoStopped(
        participant: Participant,
        source: ScreenMeet.VideoSource
    ) {
        Log.d("SessionEventDebug", "onParticipantVideoStopped $source $participant")
    }

    override fun onLocalVideoCreated(source: ScreenMeet.VideoSource, video: VideoElement) {
        Log.d("SessionEventDebug", "onLocalVideoCreated $video ${ScreenMeet.localParticipant()}")
    }

    override fun onLocalVideoStopped(source: ScreenMeet.VideoSource) {
        Log.d("SessionEventDebug", "onLocalVideoStopped $source ${ScreenMeet.localParticipant()}")
    }

    override fun onLocalAudioCreated() {
        Log.d("SessionEventDebug", "onLocalAudioCreated ${ScreenMeet.localParticipant()}")
    }

    override fun onLocalAudioStopped() {
        Log.d("SessionEventDebug", "onLocalAudioStopped ${ScreenMeet.localParticipant()}")
    }

    override fun onActiveSpeakerChanged(participant: Participant, video: VideoElement) {
        super.onActiveSpeakerChanged(participant, video)
        Log.d("SessionEventDebug", "onActiveSpeakerChanged $video $participant")
    }

    override fun onConnectionStateChanged(newState: ScreenMeet.ConnectionState) {
        Log.d("SessionEventDebug", "onConnectionStateChanged ${newState.javaClass.simpleName}")
    }

    override fun onChatMessage(chatMessage: ChatMessage) {
        Log.d("SessionEventDebug", "onChatMessage $chatMessage")
    }

    override fun onFeatureRequest(
        feature: Feature,
        decisionHandler: (granted: Boolean) -> Unit
    ) {
        Log.d("SessionEventDebug", "onFeatureRequest $feature")
    }

    override fun onFeatureRequestRejected(entitlement: Entitlement) {
        Log.d("SessionEventDebug", "onFeatureRequestRejected $entitlement")
    }

    override fun onFeatureStarted(feature: Feature) {
        Log.d("SessionEventDebug", "onFeatureStarted $feature")
    }

    override fun onFeatureStopped(feature: Feature) {
        Log.d("SessionEventDebug", "onFeatureStopped $feature")
    }
}