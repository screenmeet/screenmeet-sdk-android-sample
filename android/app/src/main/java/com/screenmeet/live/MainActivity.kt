package com.screenmeet.live

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import com.screenmeet.live.SupportApplication.Companion.startListeningForeground
import com.screenmeet.live.SupportApplication.Companion.stopListeningForeground
import com.screenmeet.live.databinding.ActivityMainBinding
import com.screenmeet.live.overlay.WidgetManager
import com.screenmeet.live.util.LogsDebugListener
import com.screenmeet.live.util.NavigationDispatcher
import com.screenmeet.sdk.*
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private var widgetManager: WidgetManager? = null

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

        lifecycleScope.launchWhenResumed { observeNavigationCommands() }
        binding.videoCall.setOnClickListener {
            navigationDispatcher.emit { it.navigate(R.id.goVideoCall) }
        }
        widgetManager = WidgetManager(this)
    }

    override fun onResume() {
        super.onResume()
        updateHeader()
    }

    private fun applyInsets() {
        binding.statusView.applyInsetter { type(statusBars = true) { padding() } }
    }

    private fun applyEdgeToEdge() {
        @Suppress("Deprecation")
        setWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false)
        window.statusBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    private fun setWindowFlag(bits: Int, on: Boolean) {
        val win = window
        val winParams = win.attributes
        if (on) winParams.flags = winParams.flags or bits
        else winParams.flags = winParams.flags and bits.inv()
        win.attributes = winParams
    }

    private fun initNavigation() {
        (supportFragmentManager.findFragmentById(R.id.fragmentHost) as NavHostFragment).also { navHost ->
            val navInflater = navHost.navController.navInflater
            val navGraph = navInflater.inflate(R.navigation.main_graph)
            navHost.navController.graph = navGraph
            navController = navHost.navController

            navHost.navController.addOnDestinationChangedListener { _, destination, _ ->
                binding.videoCall.isVisible = destination.id == R.id.fragmentMain
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

    private fun displayWidgetIfNeeded() {
        val currentDestinationId = navController.currentDestination?.id
        val shouldNotShow = currentDestinationId == R.id.fragmentConnect
                || currentDestinationId == R.id.fragmentVideoCall
                || currentDestinationId == R.id.fragmentChat
                || currentDestinationId == R.id.fragmentCallMore
                || currentDestinationId == R.id.fragmentPeople
        if(!shouldNotShow){
            val activeSpeaker = ScreenMeet.currentActiveSpeaker()
            if (activeSpeaker != null) {
                widgetManager?.showFloatingWidget(this@MainActivity, activeSpeaker)
            } else widgetManager?.hideFloatingWidget()
        } else widgetManager?.hideFloatingWidget()
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
                binding.statusView.isVisible = false
                binding.stopRemoteAssist.isVisible = false
                navigationDispatcher.emit {
                    if(it.currentBackStackEntry?.destination?.id != R.id.fragmentConnect) {
                        it.navigate(R.id.goConnect)
                    }
                }
            }
        }

        val participantsCount = ScreenMeet.participants().size
        binding.participantsTv.text = getString(R.string.session_participants, participantsCount)
    }
}
