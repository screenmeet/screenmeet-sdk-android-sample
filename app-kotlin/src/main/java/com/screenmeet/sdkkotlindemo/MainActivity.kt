package com.screenmeet.sdkkotlindemo

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.screenmeet.sdk.*
import com.screenmeet.sdk.ScreenMeetUI.PermissionResponseListener
import com.screenmeet.sdk.ScreenMeetUI.StringResponseListener
import com.screenmeet.sdk.Session.EventListener.ParticipantAction
import com.screenmeet.sdk.Session.LifecycleListener
import com.screenmeet.sdk.Session.LifecycleListener.*
import kotlinx.android.synthetic.main.activity_main.*

@SuppressLint("SetTextI18n")
@Suppress("UNUSED_ANONYMOUS_PARAMETER")
class MainActivity : AppCompatActivity() {

    private var handler: Handler? = null

    private var lifecycleListener: LifecycleListener? = null
    private var eventListener: Session.EventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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
                    ScreenMeet.session()?.let {
                        updateParticipants(it.participants())
                    }
                }
            }
        }
        handler!!.post(mockUiUpdate)
    }

    private fun enableButtons() {
        connectBtn.setOnClickListener { v: View? ->
            val code = codeEt.text.toString()
            connectSession(code)
        }
        requestBtn.setOnClickListener { v: View? ->
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
        disconnectBtn.setOnClickListener { v: View? -> ScreenMeet.disconnect(false) }
        terminateBtn.setOnClickListener { v: View? -> ScreenMeet.disconnect(true) }
        pauseBtn.setOnClickListener { v: View? ->
            if (ScreenMeet.session() != null) {
                ScreenMeet.session()!!.pause()
            }
        }
        resumeBtn.setOnClickListener { v: View? ->
            ScreenMeet.session()?.resume()
        }
        dialogBtn.setOnClickListener { v: View? ->
            ScreenMeetUI.showSessionCodeDialog(object : StringResponseListener {
                override fun onSuccess(s: String) {
                    codeEt.setText(s)
                }

                override fun onCancel() {}
            })
        }
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
        ScreenMeet.session()?.let {
            ScreenMeet.unregisterLifecycleListener(lifecycleListener)
            ScreenMeet.unregisterEventListener(eventListener)
        }
    }

    override fun onResume() {
        super.onResume()

        ScreenMeet.session()?.let {
            ScreenMeet.unregisterLifecycleListener(lifecycleListener)
            ScreenMeet.unregisterEventListener(eventListener)
            showSessionConnected(it)
        } ?: run {
            showSessionFailure()
        }
    }

    private fun showSessionConnected(session: Session) {
        connectBtn.visibility = View.GONE
        disconnectBtn.visibility = View.VISIBLE
        terminateBtn.visibility = View.VISIBLE
        resultTv.visibility = View.GONE
        connectProgress.visibility = View.GONE

        when (session.lifecycleState()) {
            Session.State.STREAMING -> showStreamingState()
            Session.State.PAUSED -> showPausedState()
            Session.State.INACTIVE -> showInactiveState()
        }

        val sessionText = "Session with " + session.owner()
        sessionTv.text = sessionText
        sessionTv.visibility = View.VISIBLE
        featuresTittleTv.visibility = View.VISIBLE
        sessionFeaturesTv.text = session.features().toString()
        sessionFeaturesTv.visibility = View.VISIBLE

        if (session.isConnected) {
            showNetworkConnected()
        } else showNetworkDisconnected()

        updateParticipants(session.participants())

        mockView.visibility = View.VISIBLE
    }

    private fun showSessionFailure() {
        connectBtn.visibility = View.VISIBLE
        disconnectBtn.visibility = View.GONE
        terminateBtn.visibility = View.GONE
        connectProgress.visibility = View.GONE
        resultTv.visibility = View.GONE
        connectionTv.visibility = View.GONE
        pauseBtn.visibility = View.GONE
        resumeBtn.visibility = View.GONE
        sessionTv.visibility = View.GONE
        sessionFeaturesTv.visibility = View.GONE
        featuresTittleTv.visibility = View.GONE
        stateLabelTv.visibility = View.GONE
        stateTv.visibility = View.GONE
        stateReasonTv.visibility = View.GONE
        participantsLabelTv.visibility = View.GONE
        participantsTv.visibility = View.GONE
        mockView.visibility = View.GONE
    }

    private fun showSessionFailure(errorCode: Int, error: String) {
        showSessionFailure()
        resultTv.setTextColor(Color.RED)
        val errorText = "$errorCode $error"
        resultTv.text = errorText
        resultTv.visibility = View.VISIBLE
    }

    private fun showProgress() {
        resultTv.visibility = View.GONE
        connectProgress.visibility = View.VISIBLE

        this.currentFocus?.let {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    private fun updateParticipants(participants: Set<Participant>) {
        val participantsText = "Participants active: " + participants.size
        participantsLabelTv.text = participantsText
        participantsLabelTv.visibility = View.VISIBLE

        val participantsString = StringBuilder()
        for (p in participants) {
            participantsString.append("Participant ").append(p.name()).append(" ").append(p.id()).append("\n")
        }
        participantsTv.text = participantsString
        participantsTv.visibility = View.VISIBLE
    }

    private fun showNetworkDisconnected() {
        connectionTv.text = "DISCONNECTED"
        connectionTv.setTextColor(Color.RED)
        connectionTv.visibility = View.VISIBLE
    }

    private fun showNetworkConnected() {
        connectionTv.text = "CONNECTED"
        connectionTv.setTextColor(Color.GREEN)
        connectionTv.visibility = View.VISIBLE
    }

    private fun showStreamingState() {
        pauseBtn.visibility = View.VISIBLE
        resumeBtn.visibility = View.GONE
        stateLabelTv.visibility = View.VISIBLE
        stateTv.text = "STREAMING"
        stateTv.setTextColor(Color.GREEN)
        stateTv.visibility = View.VISIBLE
    }

    private fun showStreamingState(@Session.State oldState: Int,
                                   @StreamingReason reasonCode: Int) {
        showStreamingState()

        var reason = "Unknown"
        if (reasonCode == StreamingReason.SESSION_RESUMED) {
            reason = "SESSION_RESUMED"
        }

        val stateChangeMessage = getStateChangeMessage(oldState, Session.State.STREAMING, reason)
        stateReasonTv.text = stateChangeMessage
        stateReasonTv.visibility = View.VISIBLE
    }

    private fun showPausedState() {
        pauseBtn.visibility = View.GONE
        resumeBtn.visibility = View.VISIBLE
        stateLabelTv.visibility = View.VISIBLE
        stateTv.text = "PAUSED"
        stateTv.setTextColor(Color.YELLOW)
        stateTv.visibility = View.VISIBLE
    }

    private fun showPausedState(@Session.State oldState: Int,
                                @PauseReason reasonCode: Int) {
        showPausedState()

        var reason = "Unknown"
        if (reasonCode == PauseReason.SESSION_PAUSED) {
            reason = "SESSION_PAUSED"
        }

        val stateChangeMessage = getStateChangeMessage(oldState, Session.State.PAUSED, reason)
        stateReasonTv.text = stateChangeMessage
        stateReasonTv.visibility = View.VISIBLE
    }

    private fun showInactiveState() {
        connectBtn.visibility = View.VISIBLE
        disconnectBtn.visibility = View.GONE
        terminateBtn.visibility = View.GONE
        connectProgress.visibility = View.GONE
        resultTv.visibility = View.GONE
        connectionTv.visibility = View.GONE
        pauseBtn.visibility = View.GONE
        resumeBtn.visibility = View.GONE
        sessionFeaturesTv.visibility = View.GONE
        featuresTittleTv.visibility = View.GONE
        stateLabelTv.visibility = View.VISIBLE
        stateTv.text = "INACTIVE"
        stateTv.setTextColor(Color.RED)
        stateTv.visibility = View.VISIBLE
        participantsLabelTv.visibility = View.GONE
        participantsTv.visibility = View.GONE
        mockView.visibility = View.GONE
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

        val stateChangeMessage = getStateChangeMessage(oldState, Session.State.INACTIVE, reason)
        stateReasonTv.text = stateChangeMessage
        stateReasonTv.visibility = View.VISIBLE
    }

    private val mockUiUpdate: Runnable = object : Runnable {
        override fun run() {
            try {
                val color = (Math.random() * 16777215).toInt() or (0xFF shl 24)
                mockView.setBackgroundColor(color)
            } finally {
                handler!!.postDelayed(this, 1000)
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
}