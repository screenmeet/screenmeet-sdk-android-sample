package com.screenmeet.sdkdemo

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
import com.screenmeet.sdk.*
import com.screenmeet.sdk.domain.entity.ChatMessage
import com.screenmeet.sdkdemo.SupportApplication.Companion.startListeningForeground
import com.screenmeet.sdkdemo.SupportApplication.Companion.stopListeningForeground
import com.screenmeet.sdkdemo.databinding.ActivityMainBinding
import com.screenmeet.sdkdemo.overlay.WidgetManager
import com.screenmeet.sdkdemo.util.NavigationDispatcher
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import org.webrtc.VideoTrack
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity: AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private var widgetManager: WidgetManager? = null

    @Inject
    lateinit var navigationDispatcher: NavigationDispatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.disconnect.setOnClickListener {
            showAlert(
                "Disconnect",
                "Are you sure you want to disconnect?") {
                ScreenMeet.disconnect()
            }
        }

        applyEdgeToEdge()
        applyInsets()
        initNavigation()

        setWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false)
        window.statusBarColor = Color.TRANSPARENT

        ScreenMeet.registerEventListener(eventListener)
        lifecycleScope.launchWhenResumed { observeNavigationCommands() }
        binding.videoCall.setOnClickListener {
            navigationDispatcher.emit { it.navigate(R.id.goVideoCall) }
        }
        widgetManager = WidgetManager(this)
    }

    private fun setWindowFlag(bits: Int, on: Boolean) {
        val win = window
        val winParams = win.attributes
        if (on) winParams.flags = winParams.flags or bits
        else winParams.flags = winParams.flags and bits.inv()
        win.attributes = winParams
    }

    private fun applyInsets() {
        binding.statusView.applyInsetter { type(statusBars = true) { padding() } }
    }

    private fun applyEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    private fun initNavigation() {
        (supportFragmentManager.findFragmentById(R.id.fragmentHost) as NavHostFragment).also { navHost ->
            val navInflater = navHost.navController.navInflater
            val navGraph = navInflater.inflate(R.navigation.main_graph).apply {
                startDestination = R.id.fragmentConnect
            }
            navHost.navController.graph = navGraph
            navController = navHost.navController

            navHost.navController.addOnDestinationChangedListener { _, destination, _ ->
                val showWidgetManager = destination.id != R.id.fragmentConnect
                        && destination.id != R.id.fragmentVideoCall
                        && destination.id != R.id.fragmentChat
                if (showWidgetManager) displayWidget()
                else widgetManager?.hideFloatingWidget()
                binding.videoCall.isVisible = destination.id == R.id.fragmentMain
            }
        }
    }

    private suspend fun observeNavigationCommands() {
        for (command in navigationDispatcher.navigationEmitter) {
            try {
                command.invoke(Navigation.findNavController(this@MainActivity, R.id.fragmentHost))
            } catch (e: IllegalArgumentException) { e.printStackTrace() }
        }
    }

    private val eventListener = object : SessionEventListener {
        override fun onConnectionStateChanged(newState: ScreenMeet.ConnectionState) { updateHeader() }

        override fun onParticipantJoined(participant: Participant) { updateHeader() }

        override fun onParticipantLeft(participant: Participant) { updateHeader() }

        override fun onActiveSpeakerChanged(participant: Participant) {}

        override fun onChatMessage(chatMessage: ChatMessage) {}

        override fun onLocalAudioCreated() {}

        override fun onLocalAudioStopped() {}

        override fun onLocalVideoCreated(videoTrack: VideoTrack) {}

        override fun onLocalVideoStopped() {}

        override fun onParticipantMediaStateChanged(participant: Participant) { displayWidget() }

        override fun onFeatureRequest(
            feature: Feature,
            decisionHandler: (granted: Boolean) -> Unit
        ) { }

        override fun onFeatureRequestRejected(entitlement: Entitlement) { }

        override fun onFeatureStarted(feature: Feature) {
            binding.stopRemoteAssist.isVisible = true
            binding.stopRemoteAssist.setOnClickListener {
                showAlert(
                    "Stop Feature",
                    "Are you sure you want to stop ${feature.entitlement} Feature?") {
                    ScreenMeet.stopFeature(feature)
                }
             }
            when(feature.entitlement){
                Entitlement.LASER_POINTER -> binding.stopRemoteAssist.setImageResource(R.drawable.ic_pointer)
                Entitlement.REMOTE_CONTROL -> binding.stopRemoteAssist.setImageResource(R.drawable.ic_remote_control)
            }
        }

        override fun onFeatureStopped(feature: Feature) {
            binding.stopRemoteAssist.isVisible = false
        }
    }

    private fun showAlert(dialogTittle: String, message: String, confirmed: () -> Unit ){
        AlertDialog.Builder(this)
            .setTitle(dialogTittle)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ -> confirmed() }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun displayWidget(){
        val shouldNotShow = navController.currentDestination?.id == R.id.fragmentConnect ||
                    navController.currentDestination?.id == R.id.fragmentVideoCall ||
                    navController.currentDestination?.id == R.id.fragmentChat
        if (shouldNotShow) return

        val participant = ScreenMeet.participants().firstOrNull { it.videoTrack != null }
        if ((participant != null)) widgetManager?.showFloatingWidget(this@MainActivity, participant.videoTrack!!)
        else widgetManager?.hideFloatingWidget()
    }

    private fun updateHeader(){
        when (ScreenMeet.connectionState().state) {
            ScreenMeet.SessionState.CONNECTING,
            ScreenMeet.SessionState.RECONNECTING -> {
                binding.statusView.isVisible = true
                binding.statusView.setBackgroundColor(ContextCompat.getColor(this, R.color.yellow))
                binding.connectionTv.text = "Session connecting"
            }
            ScreenMeet.SessionState.CONNECTED -> {
                startListeningForeground()
                binding.statusView.isVisible = true
                binding.statusView.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
                binding.connectionTv.text = "Session connected"
            }
            ScreenMeet.SessionState.DISCONNECTED -> {
                stopListeningForeground()
                binding.statusView.isVisible = false
                binding.stopRemoteAssist.isVisible = false
                navigationDispatcher.emit { it.navigate(R.id.goConnect) }
            }
        }

        val activeParticipant = ScreenMeet.participants().size
        binding.participantsTv.text = "Participants: $activeParticipant"
    }
}