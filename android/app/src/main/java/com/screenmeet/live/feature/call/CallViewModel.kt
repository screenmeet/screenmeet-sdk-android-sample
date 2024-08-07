package com.screenmeet.live.feature.call

import androidx.annotation.IdRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenmeet.live.R
import com.screenmeet.live.tools.NavigationDispatcher
import com.screenmeet.sdk.Participant
import com.screenmeet.sdk.VideoElement
import com.screenmeet.sdk.domain.entity.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val navigationDispatcher: NavigationDispatcher
) : ViewModel() {

    private val controlsDisplayTime = 5000L

    val eventChannel: Channel<Event> = Channel(Channel.UNLIMITED)

    private val _hasUnread = MutableStateFlow(false)
    val hasUnread = _hasUnread.asStateFlow()

    private val _showControls = MutableStateFlow(true)
    val showControls = _showControls.asStateFlow()

    private val _activeSpeaker = MutableStateFlow<String?>(null)
    private val _participants = MutableStateFlow(listOf<VideoElement>())

    val state = combine(_activeSpeaker, _participants) { activeSpeaker, participants ->
        val speakerLatest = participants.firstOrNull { it.id == activeSpeaker }
        val participantLatest = participants.filter { it.id != activeSpeaker }
        Pair(speakerLatest, participantLatest)
    }.stateIn(viewModelScope, WhileSubscribed(5000L), Pair(null, listOf()))

    fun updateParticipants(participants: List<VideoElement>) {
        viewModelScope.launch(Dispatchers.IO) {
            _participants.update { participants }
        }
    }

    fun markActiveSpeaker(activeSpeaker: VideoElement?) {
        viewModelScope.launch(Dispatchers.IO) {
            _activeSpeaker.update { activeSpeaker?.id }
        }
    }

    fun participantJoined(participant: Participant) {
        viewModelScope.launch(Dispatchers.IO) {
            _participants.update {
                val videos = it.toMutableList()
                videos.addAll(participant.mediaState.videoState.sources.values)
                videos
            }
        }
    }

    fun participantLeft(participant: Participant) {
        viewModelScope.launch(Dispatchers.IO) {
            _participants.update { it.filter { video -> video.participantId != participant.id } }
            if (_activeSpeaker.value == participant.id) {
                _activeSpeaker.update { null }
            }
        }
    }

    fun participantUpdated(participant: Participant) {
        viewModelScope.launch(Dispatchers.IO) {
            _participants.update {
                val elements = it.filter { video ->
                    video.participantId != participant.id
                }.toMutableList()
                elements.addAll(participant.mediaState.videoState.sources.values)
                elements.toList()
            }
        }
    }

    fun controlsVisible(visible: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (visible) {
                _showControls.value = true
                delay(controlsDisplayTime)
                _showControls.value = false
            } else {
                _showControls.value = false
            }
        }
    }

    fun receivedMessage(chatMessage: ChatMessage) {
        if (chatMessage.isOwn) return
        viewModelScope.launch(Dispatchers.IO) {
            eventChannel.send(Event.NewMessage)
            _hasUnread.update { true }
        }
    }

    fun pinVideo(uiVideo: VideoElement?) {
        viewModelScope.launch(Dispatchers.IO) {
            _activeSpeaker.update { uiVideo?.id }
        }
    }

    fun navigate(@IdRes destination: Int) {
        if (destination == R.id.goChat) {
            _hasUnread.update { false }
        }

        navigationDispatcher.emit {
            it.navigate(destination)
        }
    }

    fun openMore() {
        navigationDispatcher.emit {
            it.navigate(R.id.openMore)
        }
    }

    init {
        controlsVisible(true)
    }
}

sealed class Event {
    object NewMessage : Event()
}
