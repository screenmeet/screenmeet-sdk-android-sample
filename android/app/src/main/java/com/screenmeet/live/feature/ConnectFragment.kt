package com.screenmeet.live.feature

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.mukesh.OtpView
import com.screenmeet.live.R
import com.screenmeet.live.databinding.DialogSettingsBinding
import com.screenmeet.live.databinding.FragmentConnectBinding
import com.screenmeet.live.tools.DataStoreManager
import com.screenmeet.live.tools.NavigationDispatcher
import com.screenmeet.live.tools.viewBinding
import com.screenmeet.sdk.CompletionError
import com.screenmeet.sdk.CompletionHandler
import com.screenmeet.sdk.ScreenMeet
import com.screenmeet.sdk.ScreenMeet.ConnectionState.Connected
import com.screenmeet.sdk.SessionEventListener
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ConnectFragment : Fragment(R.layout.fragment_connect) {

    @Inject
    lateinit var dataStore: DataStoreManager

    @Inject
    lateinit var navigationDispatcher: NavigationDispatcher

    private val binding by viewBinding(FragmentConnectBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyInsets()

        if (ScreenMeet.connectionState() is Connected) {
            navigationDispatcher.emit { it.navigate(R.id.goVideoCall) }
        } else {
            binding.apply {
                codeEt.doAfterTextChanged { text ->
                    text ?: return@doAfterTextChanged
                    val isEnabled = text.length == 6 || text.length == 9 || text.length == 12
                    binding.connectBtn.isEnabled = isEnabled
                    binding.connectBtn.alpha = if (isEnabled) 0.7f else 0.3f
                }
                connectBtn.setOnClickListener {
                    val code = binding.codeEt.text.toString()
                    connect(code)
                }
                settingsBtn.setOnClickListener {
                    showSettingsDialog()
                }
                clearBtn.setOnClickListener {
                    dataStore.sessionId = null
                    codeEt.text.clear()
                }
                codeEt.setText(dataStore.sessionId)
            }
        }
    }

    private fun connect(code: String) {
        loading(true)
        val completion = object : CompletionHandler {
            override fun onSuccess() {
                dataStore.sessionId = code
                connectionSuccess()
            }

            override fun onFailure(error: CompletionError) {
                connectionError(error)
            }
        }

        ScreenMeet.connect(
            code = code,
            localUserName = "John Doe",
            completion = completion
        )
    }

    private fun loading(show: Boolean) {
        if (show) {
            binding.resultTv.isVisible = false
        }
        binding.loadingIndicator.isVisible = show
        binding.codeEt.isEnabled = !show
        binding.hintTv.isVisible = !show
        binding.connectBtn.isEnabled = !show
    }

    private fun connectionSuccess() {
        loading(false)
        binding.hintTv.isVisible = false
        binding.resultTv.isVisible = true
        binding.resultTv.setTextColor(Color.WHITE)
        binding.resultTv.text = getString(R.string.connected)
        navigationDispatcher.emit { it.navigate(R.id.goVideoCall) }
    }

    private fun connectionError(error: CompletionError) {
        when (error) {
            is CompletionError.RequestedCaptcha -> showCaptchaDialog(error.challenge)
            is CompletionError.WaitingForKnock -> {
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
        binding.settingsBtn.applyInsetter { type(statusBars = true) { margin() } }
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

    private fun showSettingsDialog() {
        AlertDialog.Builder(requireContext(), R.style.RoundedDialog).apply {
            val dialogBinding = DialogSettingsBinding.inflate(layoutInflater)
            setView(dialogBinding.root)
            setTitle(R.string.settings_tittle)
            setCancelable(true)
            setPositiveButton(R.string.save_label) { _, _ ->
                lifecycleScope.launch {
                    val endpoint = dialogBinding.urlEt.editText?.text.toString().trim()
                    val apiKey = dialogBinding.apiKeyEt.editText?.text.toString().trim()
                    val serverTag = dialogBinding.serverTagEt.editText?.text.toString().trim()
                    dataStore.setConnectionPrefs(endpoint, serverTag, apiKey)
                }
            }
            setNegativeButton(R.string.cancel_label) { _, _ -> }
            lifecycleScope.launch {
                val (endpoint, tag, apiKey) = dataStore.getConnectionPrefs()
                dialogBinding.urlEt.editText?.setText(endpoint)
                dialogBinding.apiKeyEt.editText?.setText(apiKey)
                dialogBinding.serverTagEt.editText?.setText(tag)
            }
            show()
        }
    }

    private fun showError(message: String, clear: Boolean = true) {
        if (clear) {
            binding.codeEt.editableText.clear()
        }
        binding.resultTv.isVisible = true
        val color = ContextCompat.getColor(binding.root.context, R.color.error_red)
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
            if (newState is ScreenMeet.ConnectionState.Disconnected) {
                loading(false)
                if (newState.reason != ScreenMeet.ConnectionState.Disconnected.Reason.KnockDenied) {
                    showError(getString(R.string.connection_error))
                }
            }
        }
    }
}
