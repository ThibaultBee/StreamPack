/*
 * Copyright (C) 2021 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.app.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import io.github.thibaultbee.streampack.app.databinding.MainFragmentBinding
import io.github.thibaultbee.streampack.app.utils.DialogUtils
import io.github.thibaultbee.streampack.app.utils.PermissionManager
import io.github.thibaultbee.streampack.ui.views.PreviewView


class PreviewFragment : Fragment() {
    private lateinit var binding: MainFragmentBinding

    private val previewViewModel: PreviewViewModel by viewModels {
        PreviewViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MainFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.viewmodel = previewViewModel
        bindProperties()
        return binding.root
    }

    @SuppressLint("MissingPermission")
    private fun bindProperties() {
        binding.liveButton.setOnClickListener {
            requestStreamerPermissions(previewViewModel.requiredPermissions)
        }

        previewViewModel.streamerError.observe(viewLifecycleOwner) {
            showError("Oops", it)
        }
    }

    private fun startStopLive() {
        if (binding.liveButton.isChecked) {
            startStream()
        } else {
            stopStream()
        }
    }

    private fun requestStreamerPermissions(permissions: List<String>) {
        when {
            PermissionManager.hasPermissions(
                requireContext(),
                *permissions.toTypedArray()
            ) -> {
                startStopLive()
            }

            else -> {
                requestStreamerPermissionsLauncher.launch(
                    permissions.toTypedArray()
                )
            }
        }
    }

    private fun startStream() {
        /**
         * Lock orientation while stream is running to avoid stream interruption if
         * user turns the device.
         * For landscape only mode, set [requireActivity().requestedOrientation] to
         * [ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE] in [onCreate] or [onResume].
         */
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        previewViewModel.startStream()
    }

    private fun unLockScreen() {
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    private fun stopStream() {
        previewViewModel.stopStream()
        unLockScreen()
    }

    private fun showPermissionError() {
        binding.liveButton.isChecked = false
        unLockScreen()
        DialogUtils.showPermissionAlertDialog(requireContext())
    }

    private fun showPermissionErrorAndFinish() {
        binding.liveButton.isChecked = false
        DialogUtils.showPermissionAlertDialog(requireContext()) { requireActivity().finish() }
    }

    private fun showError(title: String, message: String) {
        binding.liveButton.isChecked = false
        unLockScreen()
        DialogUtils.showAlertDialog(requireContext(), "Error: $title", message)
    }

    @SuppressLint("MissingPermission")
    override fun onStart() {
        super.onStart()
        requestCameraAndMicrophonePermissions()
    }

    override fun onPause() {
        super.onPause()
        binding.liveButton.isChecked = false
    }

    @SuppressLint("MissingPermission")
    private fun requestCameraAndMicrophonePermissions() {
        when {
            PermissionManager.hasPermissions(
                requireContext(),
                Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
            ) -> {
                configureStreamer()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                showPermissionError()
                requestCameraAndMicrophonePermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO
                    )
                )
            }

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showPermissionError()
                requestCameraAndMicrophonePermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.CAMERA
                    )
                )
            }

            else -> {
                requestCameraAndMicrophonePermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA
                    )
                )
            }
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun configureStreamer() {
        previewViewModel.configureStreamer()

        // Set camera settings button when camera is started
        binding.preview.listener = object : PreviewView.Listener {
            override fun onPreviewStarted() {
                previewViewModel.onPreviewStarted()
            }

            override fun onZoomRationOnPinchChanged(zoomRatio: Float) {
                previewViewModel.onZoomRationOnPinchChanged()
            }
        }

        // Wait till streamer exists to set it to the SurfaceView.
        previewViewModel.inflateStreamerView(binding.preview)

        // Wait till streamer exists
        lifecycle.addObserver(previewViewModel.streamerLifeCycleObserver)
    }

    @SuppressLint("MissingPermission")
    private val requestCameraAndMicrophonePermissionsLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.toList().all {
                    it.second
                }) {
                configureStreamer()
            } else {
                showPermissionErrorAndFinish()
            }
        }

    private val requestStreamerPermissionsLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.toList().all {
                    it.second
                }) {
                startStopLive()
            } else {
                showPermissionError()
            }
        }
}
