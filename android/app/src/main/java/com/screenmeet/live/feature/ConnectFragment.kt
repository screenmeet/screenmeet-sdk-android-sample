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
import com.screenmeet.sdk.*
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

        if (ScreenMeet.connectionState().state == ScreenMeet.SessionState.CONNECTED) {
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
        ScreenMeet.shareScreen()
        binding.hintTv.isVisible = false
        binding.resultTv.isVisible = true
        binding.resultTv.setTextColor(Color.WHITE)
        binding.resultTv.text = "Connected!"
        navigationDispatcher.emit { it.navigate(R.id.goMain) }
    }

    private fun connectionError(error: CompletionError) {
        loading(false)
        when (error.code) {
            ErrorCode.CAPTCHA_ERROR -> {
                error.challenge?.let {
                    showCaptchaDialog(it)
                } ?: showError(error.message)
            }
            ErrorCode.WAITING_FOR_KNOCK_PERMISSION -> showError(getString(R.string.waiting_for_knock))
            else -> showError(error.message)
        }
    }

    private fun applyInsets() {
        binding.otpView.applyInsetter { type(statusBars = true) { margin() } }
        binding.hintTv.applyInsetter { type(ime = true) { margin() } }
        binding.loadingIndicator.applyInsetter { type(ime = true) { margin() } }
    }

    private fun showCaptchaDialog(challenge: Challenge) {
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

    private fun showError(message: String) {
        binding.otpView.editableText.clear()
        binding.resultTv.isVisible = true
        binding.resultTv.setTextColor(
            ContextCompat.getColor(
                binding.root.context,
                R.color.bright_red
            )
        )
        binding.resultTv.text = message
    }
}
