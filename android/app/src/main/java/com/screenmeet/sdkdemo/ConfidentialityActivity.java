package com.screenmeet.sdkdemo;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.screenmeet.sdk.ScreenMeet;

import java.util.ArrayList;
import java.util.Random;

public class ConfidentialityActivity extends AppCompatActivity {

    private final ArrayList<View> viewToObfuscate = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ui_confidentiality);

        findViewById(R.id.obfuscateNew).setOnClickListener(v -> {
            View obfuscatedView = constructConfidentialView(ConfidentialityActivity.this, viewToObfuscate.size());

            ((ViewGroup)findViewById(R.id.obfuscateContainer)).addView(obfuscatedView,
                    new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            viewToObfuscate.add(obfuscatedView);
            ScreenMeet.setConfidential(obfuscatedView.findViewWithTag("tv"));
        });

        findViewById(R.id.deobfuscateNew).setOnClickListener(v -> {
            if(!viewToObfuscate.isEmpty()){
                View view = viewToObfuscate.get(viewToObfuscate.size() - 1);
                ScreenMeet.unsetConfidential(view.findViewWithTag("tv"));
                viewToObfuscate.remove(view);
                ((ViewGroup)findViewById(R.id.obfuscateContainer)).removeView(view);
            }
        });
    }

    public static View constructConfidentialView(Activity context, int id){
        DisplayMetrics displayMetrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;

        TextView view = new TextView(context);
        view.setTag("tv");
        String text = "Secret text " + id;
        view.setText(text);
        int textSize = new Random().nextInt(25) + 11;
        view.setTextSize(textSize);
        int measuredSize = textSize * (view.getText().length() + 5);

        Space spaceLeft = new Space(context);
        Space spaceRight = new Space(context);

        LinearLayout container = new LinearLayout(context);
        container.addView(spaceLeft, new ViewGroup.LayoutParams(width - measuredSize, ViewGroup.LayoutParams.MATCH_PARENT));
        container.addView(view, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        container.addView(spaceRight, new ViewGroup.LayoutParams(width - measuredSize, ViewGroup.LayoutParams.MATCH_PARENT));

        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        scrollView.setHorizontalScrollBarEnabled(false);
        scrollView.addView(container, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        return scrollView;
    }
}