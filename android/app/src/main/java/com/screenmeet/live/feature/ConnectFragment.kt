package com.screenmeet.live.feature

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.mukesh.OtpView
import com.screenmeet.live.R
import com.screenmeet.live.databinding.FragmentConnectBinding
import com.screenmeet.live.util.NavigationDispatcher
import com.screenmeet.live.util.viewBinding
import com.screenmeet.sdk.CompletionError
import com.screenmeet.sdk.CompletionHandler
import com.screenmeet.sdk.ScreenMeet
import com.screenmeet.sdk.SessionEventListener
import com.screenmeet.sdk.util.Log
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import javax.inject.Inject

@AndroidEntryPoint
class ConnectFragment : Fragment(R.layout.fragment_connect) {

    @Inject
    lateinit var navigationDispatcher: NavigationDispatcher

    private val binding by viewBinding(FragmentConnectBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyInsets()

        if (ScreenMeet.connectionState() is ScreenMeet.ConnectionState.Connected) {
            navigationDispatcher.emit { it.navigate(R.id.goMain) }
        } else {
            binding.otpView.setOtpCompletionListener { code -> connect(code) }
        }
    }

    private fun connect(code: String) {
        loading(true)
        ScreenMeet.connect(
            code,
            object : CompletionHandler {
                override fun onSuccess() {
                    connectionSuccess()
                }

                override fun onFailure(error: CompletionError) {
                    connectionError(error)
                }
            }
        )
    }

    private fun loading(show: Boolean) {
        if (show) binding.resultTv.isVisible = false
        binding.loadingIndicator.isVisible = show
        binding.otpView.isEnabled = !show
        binding.hintTv.isVisible = !show
    }

    private fun connectionSuccess() {
        loading(false)
        binding.hintTv.isVisible = false
        binding.resultTv.isVisible = true
        binding.resultTv.setTextColor(Color.WHITE)
        binding.resultTv.text = "Connected!"
        navigationDispatcher.emit { it.navigate(R.id.goVideoCall) }
    }

    private fun connectionError(error: CompletionError) {
        when (error) {
            is CompletionError.RequestedCaptcha -> showCaptchaDialog(error.challenge)
            is CompletionError.WaitingForKnock  -> {
                showError(getString(R.string.waiting_for_knock), false)
            }
            is CompletionError.KnockDenied -> {
                loading(false)
                showError(getString(R.string.knock_denied))
            }
            else -> {
                loading(false)
                showError(error.message)
            }
        }
    }

    private fun applyInsets() {
        binding.otpView.applyInsetter { type(statusBars = true) { margin() } }
        binding.hintTv.applyInsetter { type(ime = true) { margin() } }
        binding.loadingIndicator.applyInsetter { type(ime = true) { margin() } }
    }

    private fun showCaptchaDialog(challenge: CompletionError.RequestedCaptcha.Challenge) {
        val context = context ?: return
        AlertDialog.Builder(context).apply {
            val inflater = layoutInflater
            val view = inflater.inflate(R.layout.dialog_captcha, null)
            view.findViewById<ImageView>(R.id.captchaImage).setImageBitmap(challenge.bitmap)
            val optView = view.findViewById<OtpView>(R.id.otpView)
            setTitle(R.string.captcha_not_robot)
            setCancelable(true).setView(view)
            setOnCancelListener { challenge.solve("") }
            val dialog = create()
            optView.setOtpCompletionListener { answer ->
                dialog.dismiss()
                loading(true)
                challenge.solve(answer)
            }
            dialog.show()
        }
    }

    private fun showError(message: String, clear: Boolean = true) {
        if(clear) {
            binding.otpView.editableText.clear()
        }
        binding.resultTv.isVisible = true
        val color = ContextCompat.getColor(binding.root.context, R.color.bright_red)
        binding.resultTv.setTextColor(color)
        binding.resultTv.text = message
    }

    override fun onResume() {
        super.onResume()
        ScreenMeet.registerEventListener(sessionEventListener)
    }

    override fun onPause() {
        super.onPause()
        ScreenMeet.unregisterEventListener(sessionEventListener)
    }

    private val sessionEventListener = object : SessionEventListener {

        override fun onConnectionStateChanged(newState: ScreenMeet.ConnectionState) {
            if(newState is ScreenMeet.ConnectionState.Disconnected){
                loading(false)
                if (newState.reason != ScreenMeet.ConnectionState.Disconnected.Reason.KnockDenied) {
                    showError(getString(R.string.connection_error))
                }
            }
        }

    }
}
