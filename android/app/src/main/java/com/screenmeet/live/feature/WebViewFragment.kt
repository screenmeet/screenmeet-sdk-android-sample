package com.screenmeet.live.feature

import android.os.Bundle
import android.view.View
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.screenmeet.live.R
import com.screenmeet.live.databinding.FragmentWebviewConfidentialityBinding
import com.screenmeet.live.tools.viewBinding
import com.screenmeet.sdk.ScreenMeet
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter

@AndroidEntryPoint
class WebViewFragment : Fragment(R.layout.fragment_webview_confidentiality) {

    private val binding by viewBinding(FragmentWebviewConfidentialityBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.webView.apply {
            applyInsetter { type(statusBars = true, ime = true) { margin() } }
            ScreenMeet.setConfidential(this, true)
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.javaScriptEnabled = true
            webViewClient = WebViewClient()

            // For confidentiality example - navigate to Login page
            loadUrl("https://cbdemo.screenmeet.com/")
        }
    }
}
