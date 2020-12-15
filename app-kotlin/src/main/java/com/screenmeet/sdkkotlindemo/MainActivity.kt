package com.screenmeet.sdkkotlindemo

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
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
import kotlin.random.Random

@Suppress("MoveVariableDeclarationIntoWhen", "UNUSED_ANONYMOUS_PARAMETER")
@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {

    private lateinit var handler: Handler
    private lateinit var lifecycleListener: LifecycleListener
    private lateinit var eventListener: Session.EventListener

    private val viewToObfuscate = ArrayList<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        handler = Handler()
        enableButtons()

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
                    s?.let {
                        updateParticipants(s.participants())
                    }
                }
            }
        }

        handler.post(mockUiUpdate)
    }

    private fun enableButtons() {
        connectBtn.setOnClickListener {
            val code = codeEt.text.toString()
            connectSession(code)
        }
        requestBtn.setOnClickListener {
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

        disconnectBtn.setOnClickListener { ScreenMeet.disconnect(false) }
        terminateBtn.setOnClickListener { ScreenMeet.disconnect(true) }
        pauseBtn.setOnClickListener {
            ScreenMeet.session()?.pause()
        }
        resumeBtn.setOnClickListener {
            ScreenMeet.session()?.resume()
        }
        dialogBtn.setOnClickListener {
            ScreenMeetUI.showSessionCodeDialog(object : StringResponseListener {
                override fun onSuccess(s: String) {
                    codeEt.setText(s)
                }

                override fun onCancel() {}
            })
        }
        obfuscateNewBtn.setOnClickListener {
            val obfuscatedView = constructObfuscatedView()
            val layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            findViewById<ViewGroup>(R.id.obfuscateContainer).addView(obfuscatedView, layoutParams)
            viewToObfuscate.add(obfuscatedView)

            ScreenMeet.appStreamVideoSource().setConfidential(obfuscatedView.findViewWithTag("tv"))
        }
        deObfuscateNewBtn.setOnClickListener {
            if (viewToObfuscate.isNotEmpty()) {
                val view = viewToObfuscate[viewToObfuscate.size - 1]
                viewToObfuscate.remove(view)
                findViewById<ViewGroup>(R.id.obfuscateContainer).removeView(view)

                ScreenMeet.appStreamVideoSource().unsetConfidential(view.findViewWithTag("tv"))
                ScreenMeet.appStreamVideoSource().setConfidential(view)
                ScreenMeet.appStreamVideoSource().unsetConfidential(view)
            }
        }
        navigateToWebView.setOnClickListener { startActivity(Intent(this@MainActivity, WebViewActivity::class.java)) }
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
        ScreenMeet.session()?.let{
            ScreenMeet.unregisterLifecycleListener(lifecycleListener)
            ScreenMeet.unregisterEventListener(eventListener)
        }
    }

    override fun onResume() {
        super.onResume()
        ScreenMeet.session()?.let {
            ScreenMeet.registerLifecycleListener(lifecycleListener)
            ScreenMeet.registerEventListener(eventListener)
            showSessionConnected(it)
        } ?: showSessionFailure()
    }

    private fun showSessionConnected(session: Session) {
        connectBtn.visibility = View.GONE
        disconnectBtn.visibility = View.VISIBLE
        terminateBtn.visibility = View.VISIBLE
        resultTv.visibility = View.GONE
        connectProgress.visibility = View.GONE


        @Session.State val state = session.lifecycleState()
        when (state) {
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
        obfuscateNewBtn.visibility = View.VISIBLE
        deObfuscateNewBtn.visibility = View.VISIBLE
        navigateToWebView.visibility = View.VISIBLE
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
        obfuscateNewBtn.visibility = View.GONE
        deObfuscateNewBtn.visibility = View.GONE
        navigateToWebView.visibility = View.GONE
        mockView.visibility = View.GONE
    }

    private fun showSessionFailure(errorCode: Int, error: String) {
        showSessionFailure()
        resultTv.setTextColor(Color.RED)
        resultTv.text =  "$errorCode $error"
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

        val participantsString = StringBuilder()
        for (p in participants) {
            participantsString.append("Participant ").append(p.name()).append(" ").append(p.id()).append("\n")
        }
        participantsTv.text = participantsString
        participantsLabelTv.visibility = View.VISIBLE
        participantsTv.visibility = View.VISIBLE
    }

    private fun showNetworkDisconnected() {
        connectionTv.visibility = View.VISIBLE
        connectionTv.text = "DISCONNECTED"
        connectionTv.setTextColor(Color.RED)
    }

    private fun showNetworkConnected() {
        connectionTv.visibility = View.VISIBLE
        connectionTv.text = "CONNECTED"
        connectionTv.setTextColor(Color.GREEN)
    }

    private fun showStreamingState() {
        pauseBtn.visibility = View.VISIBLE
        resumeBtn.visibility = View.GONE
        stateLabelTv.visibility = View.VISIBLE
        stateTv.visibility = View.VISIBLE
        stateTv.text = "STREAMING"
        stateTv.setTextColor(Color.GREEN)
    }

    private fun showStreamingState(@Session.State oldState: Int,
                                   @StreamingReason reasonCode: Int) {
        showStreamingState()
        var reason = "Unknown"
        if (reasonCode == StreamingReason.SESSION_RESUMED) {
            reason = "SESSION_RESUMED"
        }
        stateReasonTv.visibility = View.VISIBLE
        val stateChangeMessage = getStateChangeMessage(oldState, Session.State.STREAMING, reason)
        stateReasonTv.text = stateChangeMessage
    }

    private fun showPausedState() {
        pauseBtn.visibility = View.GONE
        resumeBtn.visibility = View.VISIBLE
        stateLabelTv.visibility = View.VISIBLE
        stateTv.visibility = View.VISIBLE
        stateTv.text = "PAUSED"
        stateTv.setTextColor(Color.YELLOW)
    }

    private fun showPausedState(@Session.State oldState: Int,
                                @PauseReason reasonCode: Int) {
        showPausedState()
        var reason = "Unknown"
        if (reasonCode == PauseReason.SESSION_PAUSED) {
            reason = "SESSION_PAUSED"
        }
        stateReasonTv.visibility = View.VISIBLE
        val stateChangeMessage = getStateChangeMessage(oldState, Session.State.PAUSED, reason)
        stateReasonTv.text = stateChangeMessage
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
        stateTv.visibility = View.VISIBLE
        stateTv.text = "INACTIVE"
        stateTv.setTextColor(Color.RED)
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
        stateReasonTv.visibility = View.VISIBLE
        val stateChangeMessage = getStateChangeMessage(oldState, Session.State.INACTIVE, reason)
        stateReasonTv.text = stateChangeMessage
    }

    private var mockUiUpdate: Runnable = object : Runnable {
        override fun run() {
            try {
                val color = (Math.random() * 16777215).toInt() or (0xFF shl 24)
                mockView.setBackgroundColor(color)
            } finally {
                handler.postDelayed(this, 500)
            }
        }
    }

    private fun constructObfuscatedView(): View {
        val context = this@MainActivity
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        val view = TextView(context)
        view.tag = "tv"
        view.text = "Secret text " + viewToObfuscate.size
        val textSize = Random.nextInt(25) + 11
        view.textSize = textSize.toFloat()
        val measuredSize = textSize * (view.text.length + 5)
        val spaceLeft = Space(context)
        val spaceRight = Space(context)
        val container = LinearLayout(context)
        container.addView(spaceLeft, ViewGroup.LayoutParams(displayMetrics.widthPixels - measuredSize, ViewGroup.LayoutParams.MATCH_PARENT))
        container.addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        container.addView(spaceRight, ViewGroup.LayoutParams(displayMetrics.widthPixels - measuredSize, ViewGroup.LayoutParams.MATCH_PARENT))
        val scrollView = HorizontalScrollView(context)
        scrollView.isHorizontalScrollBarEnabled = false
        scrollView.addView(container, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        return scrollView
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