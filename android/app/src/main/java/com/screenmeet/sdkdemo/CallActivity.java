package com.screenmeet.sdkdemo;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.screenmeet.sdk.Participant;
import com.screenmeet.sdk.ScreenMeet;
import com.screenmeet.sdk.SessionEventListener;
import com.screenmeet.sdkdemo.databinding.ActivityCallBinding;
import com.screenmeet.sdkdemo.recycler.ParticipantsAdapter;

import org.jetbrains.annotations.NotNull;
import org.webrtc.EglBase;
import org.webrtc.RendererCommon;
import org.webrtc.VideoTrack;

import java.util.ArrayList;

public class CallActivity extends AppCompatActivity {

    private ActivityCallBinding binding;
    private ParticipantsAdapter participantsAdapter;
    private EglBase eglBase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        eglBase = EglBase.create();

        binding = ActivityCallBinding.inflate(getLayoutInflater());

        binding.activeSpeakerRenderer.init(EglBase.create().getEglBaseContext(), null);

        setContentView(binding.getRoot());
    }

    private final SessionEventListener eventListener = new SessionEventListener() {
        @Override
        public void onParticipantJoined(@NotNull Participant participant) {
            participantsAdapter.add(participant);
        }

        @Override
        public void onParticipantLeft(@NotNull Participant participant) {
            participantsAdapter.remove(participant);
        }

        @Override
        public void onParticipantMediaStateChanged(@NotNull Participant participant) {
            participantsAdapter.update(participant);
        }

        @Override
        public void onLocalVideoCreated(@NotNull VideoTrack videoTrack) {
            applyControlsState();
        }

        @Override
        public void onLocalVideoStopped() {
            applyControlsState();
        }

        @Override
        public void onLocalAudioCreated() {
            applyControlsState();
        }

        @Override
        public void onLocalAudioStopped() {
            applyControlsState();
        }

        @Override
        public void onParticipantVideoTrackCreated(@NotNull Participant participant) {
            participantsAdapter.update(participant);
        }

        @Override
        public void onParticipantAudioTrackCreated(@NotNull Participant participant) {
            participantsAdapter.update(participant);
        }

        @Override
        public void onConnectionStateChanged(@NotNull ScreenMeet.ConnectionState connectionState) {
            if(connectionState == ScreenMeet.ConnectionState.CONNECTED){
                binding.connectionLoss.setVisibility(View.GONE);

                //TODO TBD. Currently session state is restored after some delay. Would be fixed
                new Handler().postDelayed(() -> loadState(), 1000);

            } else  binding.connectionLoss.setVisibility(View.VISIBLE);
        }

        @Override
        public void onActiveSpeakerChanged(@NotNull Participant participant) {

        }

        @Override
        public void onSessionEnded(@NotNull String s) {
            sessionEnded();
            finish();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        ScreenMeet.registerEventListener(eventListener);

        enableButtons();
        loadState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ScreenMeet.unregisterEventListener(eventListener);
    }

    private void loadState(){
        ArrayList<Participant> participants = ScreenMeet.participants();
        if(!participants.isEmpty()){

            participantsAdapter = new ParticipantsAdapter(participants, eglBase);
            binding.participantsRecycler.setAdapter(participantsAdapter);
            binding.participantsRecycler.setLayoutManager(new LinearLayoutManager(this));
        }

        applyControlsState();
        setButtonBackgroundColor(binding.hangUp, R.color.bright_red);
    }

    private void sessionEnded(){

    }

    private void enableButtons(){
        binding.micro.setOnClickListener(v -> {
            switchButton(binding.micro, true, true);
            ScreenMeet.toggleLocalAudio();
        });

        binding.camera.setOnClickListener(v -> {
            switchButton(binding.camera, true, true);
            ScreenMeet.VideoSourceType sourceType = ScreenMeet.currentVideoSource();
            if(sourceType != null){
                switch (sourceType){
                    case BACK_CAMERA:
                    case FRONT_CAMERA:
                        ScreenMeet.toggleLocalVideo();
                        break;
                    case SCREEN:
                        ScreenMeet.changeVideoSource(ScreenMeet.VideoSourceType.FRONT_CAMERA);
                        break;
                }
            } else ScreenMeet.changeVideoSource(ScreenMeet.VideoSourceType.SCREEN);
        });

        binding.screen.setOnClickListener(v -> {
            switchButton(binding.screen, true, true);
            ScreenMeet.VideoSourceType sourceType = ScreenMeet.currentVideoSource();
            if(sourceType != null){
                switch (sourceType){
                    case BACK_CAMERA:
                    case FRONT_CAMERA:
                        ScreenMeet.changeVideoSource(ScreenMeet.VideoSourceType.SCREEN);
                        break;
                    case SCREEN:
                        ScreenMeet.toggleLocalVideo();
                        break;
                }
            } else ScreenMeet.changeVideoSource(ScreenMeet.VideoSourceType.SCREEN);
        });

        binding.hangUp.setOnClickListener(v -> {
            ScreenMeet.disconnect();
        });
    }

    private void applyControlsState(){
        boolean videoActive = ScreenMeet.isVideoActive();
        Log.d("applyControlsState", "video active " + videoActive);

        ScreenMeet.VideoSourceType sourceType = ScreenMeet.currentVideoSource();
        if (sourceType != null) {
            switch (sourceType) {
                case SCREEN:
                    switchButton(binding.screen, false, videoActive);
                    switchButton(binding.camera, false, false);
                    break;
                case FRONT_CAMERA:
                case BACK_CAMERA:
                    switchButton(binding.camera, false, videoActive);
                    switchButton(binding.screen, false, false);
                    break;
            }
        } else {
            switchButton(binding.camera, false, false);
            switchButton(binding.screen, false, false);
        }

        ScreenMeet.ConnectionState connectionState = ScreenMeet.connectionState();
        if(connectionState == ScreenMeet.ConnectionState.CONNECTED){
            binding.connectionLoss.setVisibility(View.GONE);
        } else  binding.connectionLoss.setVisibility(View.VISIBLE);

        if(videoActive) {
            binding.camera.setImageResource(R.drawable.videocam);
            if(sourceType == ScreenMeet.VideoSourceType.SCREEN) {
                binding.localRenderer.cameraButton.setImageResource(R.drawable.screenshot);
            } else binding.localRenderer.cameraButton.setImageResource(R.drawable.videocam);
        } else {
            binding.camera.setImageResource(R.drawable.videocam_off);
            binding.localRenderer.cameraButton.setImageResource(R.drawable.videocam_off);
        }

        boolean audioActive = ScreenMeet.isAudioActive();
        switchButton(binding.micro, false, audioActive);
        if(audioActive) {
            binding.micro.setImageResource(R.drawable.mic);
            binding.localRenderer.microButton.setImageResource(R.drawable.mic);
        } else {
            binding.micro.setImageResource(R.drawable.mic_off);
            binding.localRenderer.microButton.setImageResource(R.drawable.mic_off);
        }
    }

    private void switchButton(ImageButton button, boolean pending, boolean enabled){
        if(pending){
            button.setEnabled(false);
            setButtonBackgroundColor(button, R.color.loading_button);
        } else {
            if(enabled) setButtonBackgroundColor(button, R.color.enabled_button);
            else setButtonBackgroundColor(button, R.color.disabled_button);
            button.setEnabled(true);
        }
    }

    private void setButtonBackgroundColor(ImageButton button, int colorRes){
        GradientDrawable background = (GradientDrawable) button.getBackground();
        background.setColor(getResources().getColor(colorRes));
    }
}