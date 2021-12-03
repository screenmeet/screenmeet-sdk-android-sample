package com.screenmeet.sdkdemo.feature

import android.os.Bundle
import android.view.View
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.screenmeet.sdk.ScreenMeet
import com.screenmeet.sdkdemo.R
import com.screenmeet.sdkdemo.databinding.FragmentWebviewConfidentialityBinding
import com.screenmeet.sdkdemo.util.viewBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WebViewFragment: Fragment(R.layout.fragment_webview_confidentiality) {

    private val binding by viewBinding(FragmentWebviewConfidentialityBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.webView.apply {
            ScreenMeet.setConfidential(this, true)

            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.javaScriptEnabled = true
            webViewClient = WebViewClient()

            //For confidentiality example - navigate to Login page
            loadUrl("https://cbdemo.screenmeet.com/")
        }
    }
}