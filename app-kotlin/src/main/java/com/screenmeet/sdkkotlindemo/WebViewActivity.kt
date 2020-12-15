package com.screenmeet.sdkkotlindemo

import android.os.Bundle
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.screenmeet.sdk.ScreenMeet
import kotlinx.android.synthetic.main.activity_single_webview.*

class WebViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_single_webview)

        webView.settings.setSupportZoom(true)
        webView.settings.builtInZoomControls = true
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()

        //For confidentiality example - navigate to Login page
        webView.loadUrl("https://cbdemo.screenmeet.com/")

        ScreenMeet.appStreamVideoSource().setConfidential(webView, true)
    }
}