package com.screenmeet.live

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
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
import com.screenmeet.live.util.LogsDebugListener
import com.screenmeet.live.util.NavigationDispatcher
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

    private val logsListener = LogsDebugListener()

    @Inject
    lateinit var navigationDispatcher: NavigationDispatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.disconnect.setOnClickListener {
            showAlert(
                dialogTittle = "Disconnect",
                message = "Are you sure you want to disconnect?",
                confirmed = ScreenMeet::disconnect
            )
        }

        applyEdgeToEdge()
        applyInsets()
        initNavigation()

        ScreenMeet.registerEventListener(eventListener)
        ScreenMeet.registerEventListener(logsListener)

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                observeNavigationCommands()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateHeader()
        displayWidgetIfNeeded()
    }

    override fun onPause() {
        super.onPause()
        displayWidgetIfNeeded(true)
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
        (supportFragmentManager.findFragmentById(R.id.fragmentHost) as NavHostFragment).also { navHost ->
            val navInflater = navHost.navController.navInflater
            val navGraph = navInflater.inflate(R.navigation.main_graph)
            navHost.navController.graph = navGraph
            navController = navHost.navController

            navHost.navController.addOnDestinationChangedListener { _, _, _ ->
                displayWidgetIfNeeded()
            }
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
                Entitlement.LASER_POINTER -> binding.stopRemoteAssist.setImageResource(R.drawable.ic_pointer)
                Entitlement.REMOTE_CONTROL -> binding.stopRemoteAssist.setImageResource(R.drawable.ic_remote_control)
            }
        }

        override fun onFeatureStopped(feature: Feature) {
            binding.stopRemoteAssist.isVisible = false
        }
    }

    private fun showAlert(dialogTittle: String, message: String, confirmed: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(dialogTittle)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ -> confirmed() }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun displayWidgetIfNeeded(background: Boolean = false) {
        val widgetManager = SupportApplication.widgetManager
        val currentDestinationId = navController.currentDestination?.id
        val shouldNotShow = currentDestinationId == R.id.fragmentConnect
                || currentDestinationId ==R.id.fragmentVideoCall
                || currentDestinationId == R.id.fragmentChat
                || currentDestinationId == R.id.fragmentCallMore
                || currentDestinationId == R.id.fragmentPeople
        if(!shouldNotShow || background){
            val activeSpeaker = ScreenMeet.currentActiveSpeaker() ?: run {
                ScreenMeet.uiVideos(false).firstOrNull { it.track != null }
            }
            if (activeSpeaker != null) {
                widgetManager.showFloatingWidget(this@MainActivity, activeSpeaker)
            } else widgetManager.hideFloatingWidget()
        } else widgetManager.hideFloatingWidget()
    }

    private fun updateHeader() {
        when (ScreenMeet.connectionState()) {
            is ScreenMeet.ConnectionState.Connecting,
            is ScreenMeet.ConnectionState.Reconnecting -> {
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
            is ScreenMeet.ConnectionState.Disconnected  -> {
                stopListeningForeground()
                widgetManager.hideFloatingWidget()
                binding.statusView.isVisible = false
                binding.stopRemoteAssist.isVisible = false
                navigationDispatcher.emit {
                    if(it.currentBackStackEntry?.destination?.id != R.id.fragmentConnect) {
                        it.navigate(R.id.goConnect)
                    }
                }
            }
        }
    }
}
