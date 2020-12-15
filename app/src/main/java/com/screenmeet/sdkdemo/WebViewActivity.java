package com.screenmeet.sdkdemo;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import com.screenmeet.sdk.ScreenMeet;

public class WebViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_webview);

        WebView webView = findViewById(R.id.wv);

        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());

        //For confidentiality example - navigate to Login page
        webView.loadUrl("https://cbdemo.screenmeet.com/");

        ScreenMeet.appStreamVideoSource().setConfidential(webView, true);
    }
}