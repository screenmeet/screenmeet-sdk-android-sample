package com.screenmeet.sdkdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.screenmeet.sdk.CompletionHandler;
import com.screenmeet.sdk.LocalVideoSource;
import com.screenmeet.sdk.Participant;
import com.screenmeet.sdk.ScreenMeet;
import com.screenmeet.sdk.ScreenMeetUI;
import com.screenmeet.sdk.Session;

import java.util.Set;

import static com.screenmeet.sdk.Session.LifecycleListener.InactiveReason.DISCONNECT_LOCAL;
import static com.screenmeet.sdk.Session.LifecycleListener.InactiveReason.TERMINATED_LOCAL;
import static com.screenmeet.sdk.Session.LifecycleListener.InactiveReason.TERMINATED_SERVER;
import static com.screenmeet.sdk.Session.LifecycleListener.PauseReason.SESSION_PAUSED;
import static com.screenmeet.sdk.Session.LifecycleListener.StreamingReason.SESSION_RESUMED;
import static com.screenmeet.sdk.Session.State.INACTIVE;
import static com.screenmeet.sdk.Session.State.PAUSED;
import static com.screenmeet.sdk.Session.State.STREAMING;

@SuppressWarnings("Convert2Lambda")
@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity {

    private EditText codeEt;
    private Button connectBtn, disconnectBtn, terminateBtn, pauseBtn, resumeBtn, dialogBtn, requestBtn;

    private TextView resultTv, sessionTv,
            featuresTittleTv, sessionFeaturesTv,
            stateLabelTv, stateTv, stateReasonTv,
            connectionTv, participantsLabelTv, participantsTv;

    private ProgressBar progressBar;
    private View mockView;

    private Handler handler;

    private Session.LifecycleListener lifecycleListener;
    private Session.EventListener eventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        enableButtons();
        handler = new Handler();

        LocalVideoSource.FrameProcessor frameProcessor = ScreenMeet.localVideoSource().frameProcessor();
        ScreenMeet.localVideoSource().frameProcessor(frameProcessor);

        lifecycleListener = new Session.LifecycleListener() {
            @Override
            public void onStreaming(int i, int i1) {
                showStreamingState(i, i1);
            }

            @Override
            public void onInactive(int i, int i1) {
                showInactiveState(i, i1);
            }

            @Override
            public void onPause(int i, int i1) {
                showPausedState(i, i1);
            }

            @Override
            public void onNetworkDisconnect() {
                showNetworkDisconnected();
            }

            @Override
            public void onNetworkReconnect() {
                showNetworkConnected();
            }
        };

        eventListener = new Session.EventListener() {
            @Override
            public void onParticipantAction(Participant participant, int i) {
                switch (i){
                    case ParticipantAction.ADDED:
                    case ParticipantAction.REMOVED:
                        Session s = ScreenMeet.session();
                        if (s != null) updateParticipants(s.participants());
                        break;
                }
            }
        };

        handler.post(mockUiUpdate);
    }

    private void enableButtons(){
        connectBtn.setOnClickListener(v -> {
            String code = codeEt.getText().toString();
            connectSession(code);
        });
        requestBtn.setOnClickListener(v -> ScreenMeetUI.showAppMirrorPermissionDialog(new ScreenMeetUI.PermissionResponseListener() {
            @Override
            public void onAllow() {
                Toast.makeText(MainActivity.this, "App Mirroring allowed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeny() {
                Toast.makeText(MainActivity.this, "App Mirroring denied", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancel() {
                Toast.makeText(MainActivity.this, "App Mirroring canceled", Toast.LENGTH_SHORT).show();
            }
        }));


       disconnectBtn.setOnClickListener(v -> ScreenMeet.disconnect(false));
       terminateBtn.setOnClickListener(v -> ScreenMeet.disconnect(true));

        pauseBtn.setOnClickListener(v -> {
            if (ScreenMeet.session() != null) {
                ScreenMeet.session().pause();
            }
        });
        resumeBtn.setOnClickListener(v -> {
            if (ScreenMeet.session() != null) {
                ScreenMeet.session().resume();
            }
        });

       dialogBtn.setOnClickListener(v -> ScreenMeetUI.showSessionCodeDialog(new ScreenMeetUI.StringResponseListener() {
           @Override
           public void onSuccess(String s) {
               codeEt.setText(s);
           }

           @Override
           public void onCancel() {
           }
       }));
    }

    private void connectSession(String code){
        showProgress();
        ScreenMeet.connect(code, new CompletionHandler<Session>() {
            @Override
            public void onSuccess(Session session) {
                ScreenMeet.registerLifecycleListener(lifecycleListener);
                ScreenMeet.registerEventListener(eventListener);

                showSessionConnected(session);
            }

            @Override
            public void onFailure(int i, String s) {
                showSessionFailure(i, s);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(ScreenMeet.session() != null) {
            ScreenMeet.unregisterLifecycleListener(lifecycleListener);
            ScreenMeet.unregisterEventListener(eventListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Session session = ScreenMeet.session();
        if(session != null) {
            ScreenMeet.registerLifecycleListener(lifecycleListener);
            ScreenMeet.registerEventListener(eventListener);

            showSessionConnected(session);
        } else showSessionFailure();
    }

    private void showSessionConnected(@NonNull Session session){
        connectBtn.setVisibility(View.GONE);
        disconnectBtn.setVisibility(View.VISIBLE);
        terminateBtn.setVisibility(View.VISIBLE);
        resultTv.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        @Session.State int state = session.lifecycleState();
        switch (state){
            case STREAMING:
                showStreamingState();
                break;
            case Session.State.PAUSED:
                showPausedState();
                break;
            case Session.State.INACTIVE:
                showInactiveState();
                break;
        }

        String sessionText = "Session with " + session.owner();
        sessionTv.setText(sessionText);
        sessionTv.setVisibility(View.VISIBLE);

        featuresTittleTv.setVisibility(View.VISIBLE);
        sessionFeaturesTv.setText(session.features().toString());
        sessionFeaturesTv.setVisibility(View.VISIBLE);

        if(session.isConnected()){
            showNetworkConnected();
        } else showNetworkDisconnected();

        updateParticipants(session.participants());

        mockView.setVisibility(View.VISIBLE);
    }

    private void initView(){
        codeEt = findViewById(R.id.codeEt);
        resultTv = findViewById(R.id.resultTv);
        connectionTv = findViewById(R.id.connectionTv);

        connectBtn = findViewById(R.id.connectBtn);
        disconnectBtn = findViewById(R.id.disconnectBtn);
        terminateBtn = findViewById(R.id.terminateBtn);
        progressBar = findViewById(R.id.connectProgress);

        pauseBtn = findViewById(R.id.pauseBtn);
        resumeBtn = findViewById(R.id.resumeBtn);

        sessionTv = findViewById(R.id.sessionTv);
        featuresTittleTv = findViewById(R.id.featuresTittleTv);
        sessionFeaturesTv = findViewById(R.id.sessionFeaturesTv);

        stateLabelTv = findViewById(R.id.stateLabelTv);
        stateTv = findViewById(R.id.stateTv);
        stateReasonTv = findViewById(R.id.stateReasonTv);

        participantsLabelTv = findViewById(R.id.participantsLabelTv);
        participantsTv = findViewById(R.id.participantsTv);

        dialogBtn = findViewById(R.id.dialogBtn);
        requestBtn = findViewById(R.id.requestBtn);

        mockView = findViewById(R.id.mockView);
    }

    private void showSessionFailure(){
        connectBtn.setVisibility(View.VISIBLE);
        disconnectBtn.setVisibility(View.GONE);
        terminateBtn.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        resultTv.setVisibility(View.GONE);
        connectionTv.setVisibility(View.GONE);

        pauseBtn.setVisibility(View.GONE);
        resumeBtn.setVisibility(View.GONE);

        sessionTv.setVisibility(View.GONE);
        sessionFeaturesTv.setVisibility(View.GONE);
        featuresTittleTv.setVisibility(View.GONE);

        stateLabelTv.setVisibility(View.GONE);
        stateTv.setVisibility(View.GONE);
        stateReasonTv.setVisibility(View.GONE);

        participantsLabelTv.setVisibility(View.GONE);
        participantsTv.setVisibility(View.GONE);

        mockView.setVisibility(View.GONE);
    }

    private void showSessionFailure(int errorCode, String error){
        showSessionFailure();

        resultTv.setTextColor(Color.RED);
        String errorText = errorCode + " " + error;
        resultTv.setText(errorText);
        resultTv.setVisibility(View.VISIBLE);
    }

    private void showProgress(){
        resultTv.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void updateParticipants(Set<Participant> participants){
        String participantsText = "Participants active: " + participants.size();
        participantsLabelTv.setText(participantsText);
        StringBuilder participantsString = new StringBuilder();
        for (Participant p : participants){
            participantsString.append("Participant ").append(p.name()).append(" ").append(p.id()).append("\n");
        }
        participantsTv.setText(participantsString);

        participantsLabelTv.setVisibility(View.VISIBLE);
        participantsTv.setVisibility(View.VISIBLE);
    }

    private void showNetworkDisconnected(){
        connectionTv.setVisibility(View.VISIBLE);
        connectionTv.setText("DISCONNECTED");
        connectionTv.setTextColor(Color.RED);
    }

    private void showNetworkConnected(){
        connectionTv.setVisibility(View.VISIBLE);
        connectionTv.setText("CONNECTED");
        connectionTv.setTextColor(Color.GREEN);
    }

    private void showStreamingState(){
        pauseBtn.setVisibility(View.VISIBLE);
        resumeBtn.setVisibility(View.GONE);

        stateLabelTv.setVisibility(View.VISIBLE);
        stateTv.setVisibility(View.VISIBLE);
        stateTv.setText("STREAMING");
        stateTv.setTextColor(Color.GREEN);
    }

    private void showStreamingState(@Session.State int oldState,
                                    @Session.LifecycleListener.StreamingReason int reasonCode){
        showStreamingState();

        String reason = "Unknown";
        if (reasonCode == SESSION_RESUMED) {
            reason = "SESSION_RESUMED";
        }

        stateReasonTv.setVisibility(View.VISIBLE);
        String stateChangeMessage = getStateChangeMessage(oldState, STREAMING, reason);
        stateReasonTv.setText(stateChangeMessage);
    }

    private void showPausedState(){
        pauseBtn.setVisibility(View.GONE);
        resumeBtn.setVisibility(View.VISIBLE);

        stateLabelTv.setVisibility(View.VISIBLE);
        stateTv.setVisibility(View.VISIBLE);
        stateTv.setText("PAUSED");
        stateTv.setTextColor(Color.YELLOW);
    }

    private void showPausedState(@Session.State int oldState,
                                 @Session.LifecycleListener.PauseReason int reasonCode){
        showPausedState();

        String reason = "Unknown";
        if (reasonCode == SESSION_PAUSED) {
            reason = "SESSION_PAUSED";
        }

        stateReasonTv.setVisibility(View.VISIBLE);
        String stateChangeMessage = getStateChangeMessage(oldState, PAUSED, reason);
        stateReasonTv.setText(stateChangeMessage);
    }

    private void showInactiveState(){
        connectBtn.setVisibility(View.VISIBLE);
        disconnectBtn.setVisibility(View.GONE);
        terminateBtn.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        resultTv.setVisibility(View.GONE);
        connectionTv.setVisibility(View.GONE);

        pauseBtn.setVisibility(View.GONE);
        resumeBtn.setVisibility(View.GONE);

        sessionFeaturesTv.setVisibility(View.GONE);
        featuresTittleTv.setVisibility(View.GONE);

        stateLabelTv.setVisibility(View.VISIBLE);
        stateTv.setVisibility(View.VISIBLE);
        stateTv.setText("INACTIVE");
        stateTv.setTextColor(Color.RED);

        participantsLabelTv.setVisibility(View.GONE);
        participantsTv.setVisibility(View.GONE);

        mockView.setVisibility(View.GONE);
    }

    private void showInactiveState(@Session.State int oldState,
                                   @Session.LifecycleListener.InactiveReason int reasonCode){
        showInactiveState();

        String reason = "Unknown";
        switch (reasonCode) {
            case DISCONNECT_LOCAL :
                reason = "DISCONNECT_LOCAL";
                break;

            case TERMINATED_LOCAL :
                reason = "TERMINATED_LOCAL";
                break;

            case TERMINATED_SERVER :
                reason = "TERMINATED_SERVER";
                break;
        }
        stateReasonTv.setVisibility(View.VISIBLE);
        String stateChangeMessage = getStateChangeMessage(oldState, INACTIVE, reason);
        stateReasonTv.setText(stateChangeMessage);
    }

    private Runnable mockUiUpdate = new Runnable() {
        @Override
        public void run() {
            try {
                int color = ((int)(Math.random()*16777215)) | (0xFF << 24);
                mockView.setBackgroundColor(color);
            } finally {
                handler.postDelayed(mockUiUpdate, 1000);
            }
        }
    };

    private String getStateChangeMessage(@Session.State int oldState, @Session.State int newState, String reason){
        return "Session state changed from " +
                stateToString(oldState) +
                " to " +
                stateToString(newState) +
                ". Reason " +
                reason;
    }

    private String stateToString(@Session.State int state){
        String s = "Unknown";
        switch (state){
            case STREAMING:
                s = "STREAMING";
                break;
            case PAUSED:
                s = "PAUSED";
                break;
            case INACTIVE:
                s = "INACTIVE";
                break;
        }

        return s;
    }
}