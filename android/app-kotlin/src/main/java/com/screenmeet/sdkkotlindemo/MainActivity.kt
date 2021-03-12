package com.screenmeet.sdkkotlindemo

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.screenmeet.sdk.*
import com.screenmeet.sdk.ScreenMeetUI.PermissionResponseListener
import com.screenmeet.sdk.ScreenMeetUI.StringResponseListener
import com.screenmeet.sdk.Session.EventListener.ParticipantAction
import com.screenmeet.sdk.Session.LifecycleListener
import com.screenmeet.sdk.Session.LifecycleListener.*
import com.screenmeet.sdkkotlindemo.databinding.ActivityMainBinding
import io.flutter.embedding.android.FlutterActivity

@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var handler: Handler

    private var lifecycleListener: LifecycleListener? = null
    private var eventListener: Session.EventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view: View = binding.root
        setContentView(view)

        enableButtons()
        handler = Handler()
        val frameProcessor = ScreenMeet.localVideoSource().frameProcessor()
        ScreenMeet.localVideoSource().frameProcessor(frameProcessor)

        lifecycleListener = object : LifecycleListener {
            override fun onStreaming(i: Int, i1: Int) {
                showStreamingState(i, i1)
            }

            override fun onInactive(i: Int, i1: Int) {
                showInactiveState(i, i1)
            }

            override fun onPause(i: Int, i1: Int) {
                showPausedState(i, i1)
            }

            override fun onNetworkDisconnect() {
                showNetworkDisconnected()
            }

            override fun onNetworkReconnect() {
                showNetworkConnected()
            }
        }
        eventListener = Session.EventListener { participant, i ->
            when (i) {
                ParticipantAction.ADDED, ParticipantAction.REMOVED -> {
                    val s = ScreenMeet.session()
                    if (s != null) updateParticipants(s.participants())
                }
            }
        }
        handler.post(mockUiUpdate)
    }

    private fun enableButtons() {
        binding.connectBtn.setOnClickListener {
            val code = binding.codeEt.text.toString()
            connectSession(code)
        }
        binding.requestBtn.setOnClickListener {
            ScreenMeetUI.showAppMirrorPermissionDialog(object : PermissionResponseListener {
                override fun onAllow() {
                    Toast.makeText(this@MainActivity, "App Mirroring allowed", Toast.LENGTH_SHORT).show()
                }

                override fun onDeny() {
                    Toast.makeText(this@MainActivity, "App Mirroring denied", Toast.LENGTH_SHORT).show()
                }

                override fun onCancel() {
                    Toast.makeText(this@MainActivity, "App Mirroring canceled", Toast.LENGTH_SHORT).show()
                }
            })
        }
        binding.disconnectBtn.setOnClickListener { ScreenMeet.disconnect(false) }
        binding.terminateBtn.setOnClickListener { ScreenMeet.disconnect(true) }
        binding.pauseBtn.setOnClickListener {
                ScreenMeet.session()?.pause()
        }
        binding.resumeBtn.setOnClickListener {
            ScreenMeet.session()?.resume()
        }
        binding.dialogBtn.setOnClickListener {
            ScreenMeetUI.showSessionCodeDialog(object : StringResponseListener {
                override fun onSuccess(s: String) {
                    binding.codeEt.setText(s)
                }

                override fun onCancel() {}
            })
        }
        binding.navigateToWebView.setOnClickListener { startActivity(Intent(this@MainActivity, WebViewActivity::class.java)) }
        binding.confidentialityDemoBtn.setOnClickListener { startActivity(Intent(this@MainActivity, ConfidentialityActivity::class.java)) }
        binding.flutterFragmentDemoBtn.setOnClickListener { startActivity(Intent(this@MainActivity, CombinedFragmentActivity::class.java)) }
        binding.flutterActivityDemoBtn.setOnClickListener { startActivity(FlutterActivity.createDefaultIntent(this@MainActivity)) }
        binding.reactActivityDemoBtn.setOnClickListener { startActivity(Intent(this@MainActivity, ReactNativeActivity::class.java)) }
    }

    private fun connectSession(code: String) {
        showProgress()
        ScreenMeet.connect(code, object : CompletionHandler<Session> {
            override fun onSuccess(session: Session) {
                ScreenMeet.registerLifecycleListener(lifecycleListener)
                ScreenMeet.registerEventListener(eventListener)
                showSessionConnected(session)
            }

            override fun onFailure(i: Int, s: String) {
                showSessionFailure(i, s)
            }
        })
    }

    override fun onPause() {
        super.onPause()
        if (ScreenMeet.session() != null) {
            ScreenMeet.unregisterLifecycleListener(lifecycleListener)
            ScreenMeet.unregisterEventListener(eventListener)
        }
    }

    override fun onResume() {
        super.onResume()
        val session = ScreenMeet.session()
        if (session != null) {
            ScreenMeet.registerLifecycleListener(lifecycleListener)
            ScreenMeet.registerEventListener(eventListener)
            showSessionConnected(session)
        } else showSessionFailure()
    }

    private fun showSessionConnected(session: Session) {
        binding.connectBtn.visibility = View.GONE
        binding.disconnectBtn.visibility = View.VISIBLE
        binding.terminateBtn.visibility = View.VISIBLE
        binding.resultTv.visibility = View.GONE
        binding.connectProgress.visibility = View.GONE

        when (session.lifecycleState()) {
            Session.State.STREAMING -> showStreamingState()
            Session.State.PAUSED -> showPausedState()
            Session.State.INACTIVE -> showInactiveState()
        }

        val sessionText = "Session with ${session.owner()}"
        binding.sessionTv.text = sessionText
        binding.sessionTv.visibility = View.VISIBLE
        binding.featuresTittleTv.visibility = View.VISIBLE
        binding.sessionFeaturesTv.text = session.features().toString()
        binding.sessionFeaturesTv.visibility = View.VISIBLE

        if (session.isConnected) {
            showNetworkConnected()
        } else showNetworkDisconnected()
        updateParticipants(session.participants())

        binding.mockView.visibility = View.VISIBLE
        binding.navigateToWebView.visibility = View.VISIBLE
        binding.confidentialityDemoBtn.visibility = View.VISIBLE
        binding.flutterFragmentDemoBtn.visibility = View.VISIBLE
        binding.flutterActivityDemoBtn.visibility = View.VISIBLE
        binding.reactActivityDemoBtn.visibility = View.VISIBLE
    }

    private fun showSessionFailure() {
        binding.connectBtn.visibility = View.VISIBLE
        binding.disconnectBtn.visibility = View.GONE
        binding.terminateBtn.visibility = View.GONE
        binding.connectProgress.visibility = View.GONE
        binding.resultTv.visibility = View.GONE
        binding.connectionTv.visibility = View.GONE
        binding.pauseBtn.visibility = View.GONE
        binding.resumeBtn.visibility = View.GONE
        binding.sessionTv.visibility = View.GONE
        binding.sessionFeaturesTv.visibility = View.GONE
        binding.featuresTittleTv.visibility = View.GONE
        binding.stateLabelTv.visibility = View.GONE
        binding.stateTv.visibility = View.GONE
        binding.stateReasonTv.visibility = View.GONE
        binding.participantsLabelTv.visibility = View.GONE
        binding.participantsTv.visibility = View.GONE
        binding.mockView.visibility = View.GONE
        binding.navigateToWebView.visibility = View.GONE
        binding.confidentialityDemoBtn.visibility = View.GONE
        binding.flutterFragmentDemoBtn.visibility = View.GONE
        binding.flutterActivityDemoBtn.visibility = View.GONE
        binding.reactActivityDemoBtn.visibility = View.GONE
    }

    private fun showSessionFailure(errorCode: Int, error: String) {
        showSessionFailure()
        binding.resultTv.setTextColor(Color.RED)
        val errorText = "$errorCode $error"
        binding.resultTv.text = errorText
        binding.resultTv.visibility = View.VISIBLE
    }

    private fun showProgress() {
        binding.resultTv.visibility = View.GONE
        binding.connectProgress.visibility = View.VISIBLE
        this.currentFocus?.let {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    private fun updateParticipants(participants: Set<Participant>) {
        val participantsText = "Participants active: " + participants.size
        binding.participantsLabelTv.text = participantsText
        val participantsString = StringBuilder()
        for (p in participants) {
            participantsString.append("Participant ").append(p.name()).append(" ").append(p.id()).append("\n")
        }
        binding.participantsTv.text = participantsString
        binding.participantsLabelTv.visibility = View.VISIBLE
        binding.participantsTv.visibility = View.VISIBLE
    }

    private fun showNetworkDisconnected() {
        binding.connectionTv.visibility = View.VISIBLE
        binding.connectionTv.text = "DISCONNECTED"
        binding.connectionTv.setTextColor(Color.RED)
    }

    private fun showNetworkConnected() {
        binding.connectionTv.visibility = View.VISIBLE
        binding.connectionTv.text = "CONNECTED"
        binding.connectionTv.setTextColor(Color.GREEN)
    }

    private fun showStreamingState() {
        binding.pauseBtn.visibility = View.VISIBLE
        binding.resumeBtn.visibility = View.GONE
        binding.stateLabelTv.visibility = View.VISIBLE
        binding.stateTv.visibility = View.VISIBLE
        binding.stateTv.text = "STREAMING"
        binding.stateTv.setTextColor(Color.GREEN)
    }

    private fun showStreamingState(@Session.State oldState: Int,
                                   @StreamingReason reasonCode: Int) {
        showStreamingState()
        var reason = "Unknown"
        if (reasonCode == StreamingReason.SESSION_RESUMED) {
            reason = "SESSION_RESUMED"
        }
        binding.stateReasonTv.visibility = View.VISIBLE
        val stateChangeMessage = getStateChangeMessage(oldState, Session.State.STREAMING, reason)
        binding.stateReasonTv.text = stateChangeMessage
    }

    private fun showPausedState() {
        binding.pauseBtn.visibility = View.GONE
        binding.resumeBtn.visibility = View.VISIBLE
        binding.stateLabelTv.visibility = View.VISIBLE
        binding.stateTv.visibility = View.VISIBLE
        binding.stateTv.text = "PAUSED"
        binding.stateTv.setTextColor(Color.YELLOW)
    }

    private fun showPausedState(@Session.State oldState: Int,
                                @PauseReason reasonCode: Int) {
        showPausedState()
        var reason = "Unknown"
        if (reasonCode == PauseReason.SESSION_PAUSED) {
            reason = "SESSION_PAUSED"
        }
        binding.stateReasonTv.visibility = View.VISIBLE
        val stateChangeMessage = getStateChangeMessage(oldState, Session.State.PAUSED, reason)
        binding.stateReasonTv.text = stateChangeMessage
    }

    private fun showInactiveState() {
        binding.connectBtn.visibility = View.VISIBLE
        binding.disconnectBtn.visibility = View.GONE
        binding.terminateBtn.visibility = View.GONE
        binding.connectProgress.visibility = View.GONE
        binding.resultTv.visibility = View.GONE
        binding.connectionTv.visibility = View.GONE
        binding.pauseBtn.visibility = View.GONE
        binding.resumeBtn.visibility = View.GONE
        binding.sessionFeaturesTv.visibility = View.GONE
        binding.featuresTittleTv.visibility = View.GONE
        binding.stateLabelTv.visibility = View.VISIBLE
        binding.stateTv.visibility = View.VISIBLE
        binding.stateTv.text = "INACTIVE"
        binding.stateTv.setTextColor(Color.RED)
        binding.participantsLabelTv.visibility = View.GONE
        binding.participantsTv.visibility = View.GONE
        binding.mockView.visibility = View.GONE
        binding.navigateToWebView.visibility = View.GONE
        binding.confidentialityDemoBtn.visibility = View.GONE
        binding.flutterActivityDemoBtn.visibility = View.GONE
        binding.flutterFragmentDemoBtn.visibility = View.GONE
        binding.reactActivityDemoBtn.visibility = View.GONE
    }

    private fun showInactiveState(@Session.State oldState: Int,
                                  @InactiveReason reasonCode: Int) {
        showInactiveState()
        var reason = "Unknown"
        when (reasonCode) {
            InactiveReason.DISCONNECT_LOCAL -> reason = "DISCONNECT_LOCAL"
            InactiveReason.TERMINATED_LOCAL -> reason = "TERMINATED_LOCAL"
            InactiveReason.TERMINATED_SERVER -> reason = "TERMINATED_SERVER"
        }
        binding.stateReasonTv.visibility = View.VISIBLE
        val stateChangeMessage = getStateChangeMessage(oldState, Session.State.INACTIVE, reason)
        binding.stateReasonTv.text = stateChangeMessage
    }

    private var mockUiUpdate: Runnable = object : Runnable {
        override fun run() {
            try {
                val color = (Math.random() * 16777215).toInt() or (0xFF shl 24)
                binding.mockView.setBackgroundColor(color)
            } finally {
                handler.postDelayed(this, 500)
            }
        }
    }

    private fun getStateChangeMessage(@Session.State oldState: Int, @Session.State newState: Int, reason: String): String {
        return "Session state changed from " +
                stateToString(oldState) +
                " to " +
                stateToString(newState) +
                ". Reason " +
                reason
    }

    private fun stateToString(@Session.State state: Int): String {
        var s = "Unknown"
        when (state) {
            Session.State.STREAMING -> s = "STREAMING"
            Session.State.PAUSED -> s = "PAUSED"
            Session.State.INACTIVE -> s = "INACTIVE"
        }
        return s
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleListener = null
        eventListener = null
    }
}