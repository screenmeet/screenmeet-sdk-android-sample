package com.screenmeet.live.feature.call

import androidx.annotation.IdRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenmeet.live.R
import com.screenmeet.live.util.NavigationDispatcher
import com.screenmeet.sdk.VideoElement
import com.screenmeet.sdk.domain.entity.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _showControls = MutableStateFlow(false)
    val showControls = _showControls.asStateFlow()

    private val _activeSpeaker = MutableStateFlow<String?>(null)
    val activeSpeaker = _activeSpeaker.asStateFlow()

    private val _participants = MutableStateFlow(listOf<VideoElement>())
    val participants = _participants.asStateFlow()

    fun controlsVisible(visible: Boolean){
        viewModelScope.launch {
            if (visible){
                _showControls.value = true
                delay(controlsDisplayTime)
                _showControls.value = false
            } else _showControls.value = false
        }
    }

    fun updateParticipants(participants: List<VideoElement>){
        _participants.value = participants
    }

    fun pinVideo(uiVideo: VideoElement?){
        _activeSpeaker.value = uiVideo?.id
    }

    fun receivedMessage(chatMessage: ChatMessage){
        viewModelScope.launch {
            if(chatMessage.isOwn) return@launch
            eventChannel.send(Event.NewMessage)
            _hasUnread.value = true
        }
    }

    fun openMore(){
        _hasUnread.value = false
        navigationDispatcher.emit {
            it.navigate(R.id.openMore)
        }
    }

    fun navigate(@IdRes destination: Int){
        navigationDispatcher.emit {
            it.navigate(destination)
        }
    }

    init {
        controlsVisible(true)
    }
}

sealed class Event {
    object NewMessage: Event()
}