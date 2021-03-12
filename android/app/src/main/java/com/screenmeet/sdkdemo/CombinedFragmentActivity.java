package com.screenmeet.sdkdemo;

import android.os.Bundle;
import android.view.ViewGroup;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.facebook.react.*;
import com.facebook.react.common.LifecycleState;
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler;
import com.facebook.soloader.SoLoader;

import java.util.List;

import io.flutter.embedding.android.FlutterFragment;

public class CombinedFragmentActivity extends FragmentActivity implements DefaultHardwareBackBtnHandler {

    private final String TAG_FLUTTER_FRAGMENT = FlutterFragment.class.getSimpleName();

    private ReactInstanceManager reactInstanceManager;
    private ReactRootView reactRootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ui_flutter);
        FragmentManager fragmentManager = getSupportFragmentManager();

        FlutterFragment flutterFragment = (FlutterFragment)
                fragmentManager.findFragmentByTag(TAG_FLUTTER_FRAGMENT);

        if (flutterFragment == null) {
            flutterFragment = FlutterFragment.createDefault();
            fragmentManager
                    .beginTransaction()
                    .add(R.id.fluterContainer,
                            flutterFragment,
                        TAG_FLUTTER_FRAGMENT
                    ).commit();
        }

        SoLoader.init(this, false);

        reactRootView = new ReactRootView(this);
        List<ReactPackage> packages = new PackageList(getApplication()).getPackages();

        reactInstanceManager = ReactInstanceManager.builder()
                .setApplication(getApplication())
                .setCurrentActivity(this)
                .setBundleAssetName("index.android.bundle")
                .setJSMainModulePath("index")
                .addPackages(packages)
                .setUseDeveloperSupport(BuildConfig.DEBUG)
                .setInitialLifecycleState(LifecycleState.RESUMED)
                .build();

        reactRootView.startReactApplication(reactInstanceManager, "ReactNativeApp", null);

        ViewGroup container = findViewById(R.id.reactContainer);
        container.addView(reactRootView);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (reactInstanceManager != null) {
            reactInstanceManager.onHostPause(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (reactInstanceManager != null) {
            reactInstanceManager.onHostResume(this, this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (reactInstanceManager != null) {
            reactInstanceManager.onHostDestroy(this);
        }
        if (reactRootView != null) {
            reactRootView.unmountReactApplication();
        }
    }

    @Override
    public void invokeDefaultOnBackPressed() {
        super.onBackPressed();
    }
}