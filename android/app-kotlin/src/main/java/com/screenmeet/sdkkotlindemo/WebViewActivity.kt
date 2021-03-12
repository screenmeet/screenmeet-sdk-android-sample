package com.screenmeet.sdkkotlindemo

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.screenmeet.sdk.ScreenMeet

class WebViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_webview)

        val webView = findViewById<WebView>(R.id.wv)
        webView.settings.setSupportZoom(true)
        webView.settings.builtInZoomControls = true
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()

        //For confidentiality example - navigate to Login page
        webView.loadUrl("https://cbdemo.screenmeet.com/")
        ScreenMeet.appStreamVideoSource().setConfidential(webView, true)
    }
}