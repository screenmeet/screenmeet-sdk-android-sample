package com.screenmeet.sdkdemo.recycler;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.screenmeet.sdk.Identity;
import com.screenmeet.sdk.Participant;
import com.screenmeet.sdkdemo.R;
import com.screenmeet.sdkdemo.databinding.PartcipantLayoutBinding;

import org.webrtc.EglBase;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.ArrayList;

public class ParticipantsAdapter extends RecyclerView.Adapter<ParticipantsAdapter.ViewHolder> {

    private final ArrayList<Participant> participants;
    private final EglBase eglBase;

    public ParticipantsAdapter(ArrayList<Participant> participants, EglBase eglBase) {
        this.participants = participants;
        this.eglBase = eglBase;
    }

    @Override
    public ParticipantsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        PartcipantLayoutBinding binding = PartcipantLayoutBinding.inflate(inflater);
        binding.surfaceViewRenderer.init(eglBase.getEglBaseContext(), null);
        binding.surfaceViewRenderer.setZOrderMediaOverlay(true);
        binding.surfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        return new ViewHolder(binding.getRoot());
    }

    public void add(Participant participant) {
        participants.add(participant);
        notifyItemInserted(participants.size() - 1);
    }

    public void remove(Participant participant) {
        int index = participants.indexOf(participant);
        if(index != -1){
            participants.remove(index);
            notifyItemRemoved(index);
        }
    }

    public void update(Participant participant) {
        int index = participants.indexOf(participant);
        if(index != -1){
            participants.set(index, participant);
            notifyItemChanged(index);
        }
    }

    @Override
    public void onBindViewHolder(ParticipantsAdapter.ViewHolder holder, int position) {
        Participant participant = participants.get(position);

        holder.nameTv.setText(participant.getIdentity().getName());

        if(participant.getIdentity().getRole() == Identity.Role.HOST){
            holder.hostImage.setVisibility(View.VISIBLE);
        } else holder.hostImage.setVisibility(View.GONE);

        if(participant.getCallerState().getAudioEnabled()){
            holder.micButton.setImageResource(R.drawable.mic);
        } else holder.micButton.setImageResource(R.drawable.mic_off);

        boolean videoEnabled = participant.getCallerState().getVideoEnabled();
        boolean screenEnabled = participant.getCallerState().getScreenEnabled();
        if(videoEnabled || screenEnabled){
            if(videoEnabled) holder.cameraButton.setImageResource(R.drawable.videocam);
            else holder.cameraButton.setImageResource(R.drawable.screenshot);
        } else {
            holder.cameraButton.setImageResource(R.drawable.videocam_off);
        }

        VideoTrack videoTrack = participant.getVideoTrack();
        if (videoTrack != null) {
            videoTrack.setEnabled(true);
            videoTrack.addSink(holder.renderer);
        }
    }

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return participants.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView nameTv;
        public ImageView micButton;
        public ImageView cameraButton;
        public ImageView hostImage;
        public SurfaceViewRenderer renderer;

        public ViewHolder(View itemView) {
            super(itemView);

            nameTv = (TextView) itemView.findViewById(R.id.nameTv);
            micButton = (ImageView) itemView.findViewById(R.id.microButton);
            cameraButton = (ImageView) itemView.findViewById(R.id.cameraButton);
            hostImage = (ImageView) itemView.findViewById(R.id.hostImage);
            renderer = (SurfaceViewRenderer) itemView.findViewById(R.id.surfaceViewRenderer);
        }
    }
}