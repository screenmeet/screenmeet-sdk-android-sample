package com.screenmeet.sdkdemo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;

import com.screenmeet.sdk.CompletionError;
import com.screenmeet.sdk.CompletionHandler;
import com.screenmeet.sdk.ErrorCode;
import com.screenmeet.sdk.Participant;
import com.screenmeet.sdk.ScreenMeet;
import com.screenmeet.sdk.SessionEventListener;
import com.screenmeet.sdkdemo.databinding.ActivityMainBinding;

import org.jetbrains.annotations.NotNull;
import org.webrtc.VideoTrack;

import io.flutter.embedding.android.FlutterActivity;

@SuppressWarnings("Convert2Lambda")
@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private Handler handler;

    private SessionEventListener eventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        enableButtons();
        handler = new Handler();
        eventListener = new SessionEventListener() {
            @Override
            public void onParticipantJoined(@NotNull Participant participant) {

            }

            @Override
            public void onParticipantLeft(@NotNull Participant participant) {

            }

            @Override
            public void onParticipantMediaStateChanged(@NotNull Participant participant) {

            }

            @Override
            public void onLocalVideoCreated(@NotNull VideoTrack videoTrack) {
            }

            @Override
            public void onLocalVideoStopped() {

            }

            @Override
            public void onLocalAudioCreated() {

            }

            @Override
            public void onLocalAudioStopped() {

            }

            @Override
            public void onActiveSpeakerChanged(@NotNull Participant participant) {

            }

            @Override
            public void onConnectionStateChanged(@NotNull ScreenMeet.ConnectionState connectionState) {
                switch (connectionState.getState()){
                    case CONNECTED:
                        showSessionConnected();
                        break;
                    case CONNECTING:
                    case RECONNECTING:
                        break;
                    case DISCONNECTED:
                        showSessionFailure();
                        break;
                }
            }
        };

        handler.post(mockUiUpdate);
    }

    private void enableButtons(){
        binding.connectBtn.setOnClickListener(v -> {
            String code = binding.codeEt.getText().toString();
            connectSession(code);
        });

        binding.disconnectBtn.setOnClickListener(v -> ScreenMeet.disconnect());
        binding.navigateToWebView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, WebViewActivity.class));
            }
        });
        binding.confidentialityDemoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ConfidentialityActivity.class));
            }
        });
        binding.flutterFragmentDemoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CombinedFragmentActivity.class));
            }
        });
        binding.flutterActivityDemoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(FlutterActivity.createDefaultIntent(MainActivity.this));
            }
        });
        binding.reactActivityDemoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ReactNativeActivity.class));
            }
        });
        binding.callActivityDemoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CallActivity.class));
            }
        });
    }

    private void connectSession(String code){
        showProgress();
        ScreenMeet.connect(code, new CompletionHandler() {
            @Override
            public void onSuccess() {
                ScreenMeet.shareScreen();
                ScreenMeet.shareAudio();
            }

            @Override
            public void onFailure(@NotNull CompletionError completionError) {
                showSessionFailure(completionError.getCode(), completionError.getMessage());
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        ScreenMeet.unregisterEventListener(eventListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ScreenMeet.registerEventListener(eventListener);
        ScreenMeet.SessionState connectionState = ScreenMeet.connectionState().getState();
        switch (connectionState){
            case CONNECTED:
                ScreenMeet.registerEventListener(eventListener);
                showSessionConnected();
                break;
            case CONNECTING:
            case RECONNECTING:
                break;
            case DISCONNECTED:
                showSessionFailure();
                break;
        }
    }

    private void showSessionConnected(){
        binding.connectBtn.setVisibility(View.GONE);
        binding.disconnectBtn.setVisibility(View.VISIBLE);
        binding.resultTv.setVisibility(View.GONE);
        binding.connectProgress.setVisibility(View.GONE);

        binding.mockView.setVisibility(View.VISIBLE);

        binding.navigateToWebView.setVisibility(View.VISIBLE);
        binding.confidentialityDemoBtn.setVisibility(View.VISIBLE);
        binding.flutterFragmentDemoBtn.setVisibility(View.VISIBLE);
        binding.flutterActivityDemoBtn.setVisibility(View.VISIBLE);
        binding.reactActivityDemoBtn.setVisibility(View.VISIBLE);
        binding.callActivityDemoBtn.setVisibility(View.VISIBLE);
    }

    private void showSessionFailure(){
        binding.connectBtn.setVisibility(View.VISIBLE);
        binding.disconnectBtn.setVisibility(View.GONE);

        binding.connectProgress.setVisibility(View.GONE);

        binding.resultTv.setVisibility(View.GONE);
        binding.connectionTv.setVisibility(View.GONE);

        binding.sessionTv.setVisibility(View.GONE);
        binding.sessionFeaturesTv.setVisibility(View.GONE);

        binding.stateLabelTv.setVisibility(View.GONE);
        binding.stateTv.setVisibility(View.GONE);
        binding.stateReasonTv.setVisibility(View.GONE);

        binding.participantsLabelTv.setVisibility(View.GONE);
        binding.participantsTv.setVisibility(View.GONE);

        binding.mockView.setVisibility(View.GONE);

        binding.navigateToWebView.setVisibility(View.GONE);
        binding.confidentialityDemoBtn.setVisibility(View.GONE);
        binding.flutterFragmentDemoBtn.setVisibility(View.GONE);
        binding.flutterActivityDemoBtn.setVisibility(View.GONE);
        binding.reactActivityDemoBtn.setVisibility(View.GONE);
        binding.callActivityDemoBtn.setVisibility(View.GONE);
    }

    private void showSessionFailure(ErrorCode errorCode, String error){
        showSessionFailure();

        binding.resultTv.setTextColor(Color.RED);
        String errorText = errorCode + " " + error;
        binding.resultTv.setText(errorText);
        binding.resultTv.setVisibility(View.VISIBLE);
    }

    private void showProgress(){
        binding.resultTv.setVisibility(View.GONE);
        binding.connectProgress.setVisibility(View.VISIBLE);
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private Runnable mockUiUpdate = new Runnable() {
        @Override
        public void run() {
            try {
                int color = ((int)(Math.random()*16777215)) | (0xFF << 24);
                binding.mockView.setBackgroundColor(color);
            } finally {
                handler.postDelayed(mockUiUpdate, 500);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        eventListener = null;
        mockUiUpdate = null;
    }
}