package com.screenmeet.sdkdemo;

import android.app.Application;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;

import com.screenmeet.sdk.ScreenMeet;

public class SupportApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        //TODO Provide your API token below
        ScreenMeet.Configuration configuration = new ScreenMeet.Configuration(null);

        ScreenMeet.init(this, configuration);
        registerActivityLifecycleCallbacks(ScreenMeet.activityLifecycleCallback());
    }
}