package com.screenmeet.live.feature

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.screenmeet.live.R
import com.screenmeet.live.databinding.FragmentCameraBinding
import com.screenmeet.live.util.viewBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CameraFragment : Fragment(R.layout.fragment_camera) {

    private val binding by viewBinding(FragmentCameraBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fullSize = 100.0f
        binding.camera.zoomFactor = fullSize
        binding.zoomSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.camera.zoomFactor = progress / fullSize
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    override fun onStart() {
        super.onStart()
        binding.camera.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.camera.onResume()
    }

    override fun onPause() {
        binding.camera.onPause()
        super.onPause()
    }

    override fun onStop() {
        binding.camera.onStop()
        super.onStop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        binding.camera.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
