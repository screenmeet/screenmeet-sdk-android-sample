package com.screenmeet.live

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import com.screenmeet.live.SupportApplication.Companion.startListeningForeground
import com.screenmeet.live.SupportApplication.Companion.stopListeningForeground
import com.screenmeet.live.SupportApplication.Companion.widgetManager
import com.screenmeet.live.databinding.ActivityMainBinding
import com.screenmeet.live.tools.NavigationDispatcher
import com.screenmeet.live.tools.showAlert
import com.screenmeet.sdk.Entitlement
import com.screenmeet.sdk.Feature
import com.screenmeet.sdk.Participant
import com.screenmeet.sdk.ScreenMeet
import com.screenmeet.sdk.SessionEventListener
import com.screenmeet.sdk.VideoElement
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.launch
import javax.inject.Inject

const val TRANSLUCENT_STATUS = 67108864

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    @Inject
    lateinit var navigationDispatcher: NavigationDispatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyEdgeToEdge()
        applyInsets()
        initNavigation()

        ScreenMeet.registerEventListener(eventListener)

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                observeNavigationCommands()
            }
        }
    }

    override fun onResume() {
        SupportApplication.inBackground = false
        super.onResume()
        updateHeader()
        displayWidgetIfNeeded()
    }

    override fun onPause() {
        SupportApplication.inBackground = true
        super.onPause()
        displayWidgetIfNeeded()
    }

    private fun applyInsets() {
        binding.statusView.applyInsetter { type(statusBars = true) { padding() } }
    }

    private fun applyEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.clearFlags(TRANSLUCENT_STATUS)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }

    private fun initNavigation() {
        val navHost = supportFragmentManager.findFragmentById(R.id.fragmentHost) as NavHostFragment
        val navInflater = navHost.navController.navInflater
        val navGraph = navInflater.inflate(R.navigation.main_graph)
        navHost.navController.graph = navGraph
        navController = navHost.navController

        navHost.navController.addOnDestinationChangedListener { _, _, _ ->
            displayWidgetIfNeeded()
        }
    }

    private suspend fun observeNavigationCommands() {
        for (command in navigationDispatcher.navigationEmitter) {
            try {
                command.invoke(Navigation.findNavController(this@MainActivity, R.id.fragmentHost))
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }
    }

    private val eventListener = object : SessionEventListener {
        override fun onConnectionStateChanged(newState: ScreenMeet.ConnectionState) {
            updateHeader()
        }

        override fun onParticipantJoined(participant: Participant) {
            updateHeader()
        }

        override fun onParticipantLeft(participant: Participant) {
            updateHeader()
            displayWidgetIfNeeded()
        }

        override fun onParticipantAudioCreated(participant: Participant) {
            displayWidgetIfNeeded()
        }

        override fun onParticipantAudioStopped(participant: Participant) {
            displayWidgetIfNeeded()
        }

        override fun onParticipantVideoCreated(participant: Participant, video: VideoElement) {
            displayWidgetIfNeeded()
        }

        override fun onParticipantVideoStopped(
            participant: Participant,
            source: ScreenMeet.VideoSource
        ) = displayWidgetIfNeeded()

        override fun onFeatureStarted(feature: Feature) {
            binding.stopRemoteAssist.isVisible = true
            binding.stopRemoteAssist.setOnClickListener {
                showAlert(
                    "Stop Feature",
                    "Are you sure you want to stop ${feature.entitlement} Feature?"
                ) {
                    ScreenMeet.stopFeature(feature)
                }
            }
            when (feature.entitlement) {
                Entitlement.LASER_POINTER -> binding.stopRemoteAssist.setImageResource(
                    R.drawable.ic_pointer
                )

                Entitlement.REMOTE_CONTROL -> binding.stopRemoteAssist.setImageResource(
                    R.drawable.ic_remote_control
                )
            }
        }

        override fun onFeatureStopped(feature: Feature) {
            binding.stopRemoteAssist.isVisible = false
        }

        override fun onLocalVideoStopped(source: ScreenMeet.VideoSource) {
            displayWidgetIfNeeded()
        }

        override fun onLocalVideoCreated(source: ScreenMeet.VideoSource, video: VideoElement) {
            displayWidgetIfNeeded()
        }


        override fun onScreenShareRequest(participant: Participant) {
            SupportApplication.instance?.let { app ->
                if (SupportApplication.inBackground) {
                    Toast.makeText(app,
                        getString(
                            R.string.background_screenshare_request,
                            participant.identity.name,
                            app.getString(R.string.app_name)
                        ), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun displayWidgetIfNeeded() {
        val widgetManager = SupportApplication.widgetManager

        val currentDestinationId = navController.currentDestination?.id
        val shouldHideWidget = currentDestinationId == R.id.fragmentConnect ||
                currentDestinationId == R.id.fragmentVideoCall ||
                currentDestinationId == R.id.fragmentChat ||
                currentDestinationId == R.id.fragmentCallMore ||
                currentDestinationId == R.id.fragmentPeople

        if (!shouldHideWidget || SupportApplication.inBackground) {
            var activeSpeaker = ScreenMeet.currentActiveSpeaker()
            if (activeSpeaker?.track == null || isOwnScreenShare(activeSpeaker)) {
                activeSpeaker = ScreenMeet.uiVideos(true).firstOrNull {
                    it.track != null && !isOwnScreenShare(it)
                }
            }

            if (activeSpeaker != null) {
                widgetManager.showFloatingWidget(this@MainActivity, activeSpeaker)
                return
            }
        }

        widgetManager.hideFloatingWidget()
    }

    private fun isOwnScreenShare(videoElement: VideoElement): Boolean {
        val localParticipant = ScreenMeet.localParticipant()
        return videoElement.participantId == localParticipant.id &&
                videoElement.sourceType == ScreenMeet.VideoSource.Screen
    }

    private fun updateHeader() {
        when (ScreenMeet.connectionState()) {
            is ScreenMeet.ConnectionState.Connecting,
            is ScreenMeet.ConnectionState.Reconnecting -> {
                widgetManager.hideFloatingWidget()
                binding.statusView.isVisible = true
                binding.statusView.setBackgroundColor(ContextCompat.getColor(this, R.color.yellow))
                binding.connectionTv.text = getString(R.string.session_connecting)
            }

            is ScreenMeet.ConnectionState.Connected -> {
                startListeningForeground()
                binding.statusView.isVisible = true
                binding.statusView.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
                binding.connectionTv.text = getString(R.string.session_connected)
            }

            is ScreenMeet.ConnectionState.Disconnected -> {
                stopListeningForeground()
                widgetManager.hideFloatingWidget()
                binding.statusView.isVisible = false
                binding.stopRemoteAssist.isVisible = false
                navigationDispatcher.emit {
                    if (it.currentBackStackEntry?.destination?.id != R.id.fragmentConnect) {
                        it.navigate(R.id.goConnect)
                    }
                }
            }
        }
    }
}
