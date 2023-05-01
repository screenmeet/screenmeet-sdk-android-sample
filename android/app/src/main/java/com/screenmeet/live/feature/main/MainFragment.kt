package com.screenmeet.live.feature.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.screenmeet.live.R
import com.screenmeet.live.databinding.FragmentMainBinding
import com.screenmeet.live.util.NavigationDispatcher
import com.screenmeet.live.util.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import javax.inject.Inject

@AndroidEntryPoint
class MainFragment : Fragment(R.layout.fragment_main) {

    @Inject
    lateinit var navigationDispatcher: NavigationDispatcher

    private val binding by viewBinding(FragmentMainBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyInsets()

        val featureAdapter = FeatureAdapter { feature ->
            when (feature.first) {
                "React Native Activity Demo" -> startActivity(Intent(context, react()))
                "Flutter Activity Demo" -> {
                    flutter()?.let {
                        try {
                            startActivity(
                                it.getMethod("provideFlutter", Context::class.java)
                                    .invoke(it, context) as Intent
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                else -> navigationDispatcher.emit { it.navigate(feature.second) }
            }
        }
        binding.featureRecycler.adapter = featureAdapter
        binding.featureRecycler.layoutManager = StaggeredGridLayoutManager(
            2,
            StaggeredGridLayoutManager.VERTICAL
        )
        featureAdapter.submitList(features())
    }

    private fun applyInsets() {
        binding.featureRecycler.applyInsetter { type(navigationBars = true) { margin() } }
    }

    private fun features(): List<Pair<String, Int>> {
        val features = mutableListOf(
            Pair("Camera Control Demo", R.id.goCamera),
            Pair("Confidential WebView Demo", R.id.goWebView),
            Pair("Confidentiality Demo", R.id.goConfidentiality)
        )
        if (react() != null) {
            features.add(Pair("React Native Activity Demo", -1))
        }
        if (flutter() != null) {
            features.add(Pair("Flutter Activity Demo", -1))
        }

        return features.toList()
    }

    private fun react(): Class<*>? {
        return try {
            Class.forName("com.xps.react.demo.ReactNativeActivity")
        } catch (e: ClassNotFoundException) {
            null
        }
    }

    private fun flutter(): Class<*>? {
        return try {
            Class.forName("com.xps.flutter.demo.FlutterProvider")
        } catch (e: ClassNotFoundException) {
            null
        }
    }
}
