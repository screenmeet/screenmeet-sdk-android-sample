package com.screenmeet.sdkkotlindemo

import android.os.Bundle
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import com.facebook.react.PackageList
import com.facebook.react.ReactInstanceManager
import com.facebook.react.ReactPackage
import com.facebook.react.ReactRootView
import com.facebook.react.common.LifecycleState
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler
import com.facebook.soloader.SoLoader
import io.flutter.embedding.android.FlutterFragment

class CombinedFragmentActivity : FragmentActivity(), DefaultHardwareBackBtnHandler {

    private val flutterFragmentTag = FlutterFragment::class.java.simpleName

    private var reactInstanceManager: ReactInstanceManager? = null
    private var reactRootView: ReactRootView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ui_flutter)
        val fragmentManager = supportFragmentManager
        var flutterFragment = fragmentManager.findFragmentByTag(flutterFragmentTag) as FlutterFragment?
        if (flutterFragment == null) {
            flutterFragment = FlutterFragment.createDefault()
            fragmentManager
                    .beginTransaction()
                    .add(R.id.fluterContainer,
                            flutterFragment,
                            flutterFragmentTag
                    ).commit()
        }
        SoLoader.init(this, false)
        reactRootView = ReactRootView(this)
        val packages: List<ReactPackage> = PackageList(application).packages
        reactInstanceManager = ReactInstanceManager.builder()
                .setApplication(application)
                .setCurrentActivity(this)
                .setBundleAssetName("index.android.bundle")
                .setJSMainModulePath("index")
                .addPackages(packages)
                .setUseDeveloperSupport(BuildConfig.DEBUG)
                .setInitialLifecycleState(LifecycleState.RESUMED)
                .build()
        reactRootView!!.startReactApplication(reactInstanceManager, "ReactNativeApp", null)
        val container = findViewById<ViewGroup>(R.id.reactContainer)
        container.addView(reactRootView)
    }

    override fun onPause() {
        super.onPause()
        reactInstanceManager?.onHostPause(this)
    }

    override fun onResume() {
        super.onResume()
        reactInstanceManager?.onHostResume(this, this)
    }

    override fun onDestroy() {
        super.onDestroy()
        reactInstanceManager?.onHostDestroy(this)
        reactRootView?.unmountReactApplication()
    }

    override fun invokeDefaultOnBackPressed() {
        super.onBackPressed()
    }
}