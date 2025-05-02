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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ToggleButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import io.github.thibaultbee.streampack.app.ApplicationConstants
import io.github.thibaultbee.streampack.app.R
import io.github.thibaultbee.streampack.app.databinding.MainFragmentBinding
import io.github.thibaultbee.streampack.app.utils.DialogUtils
import io.github.thibaultbee.streampack.app.utils.PermissionManager
import io.github.thibaultbee.streampack.core.interfaces.IStreamer
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoSource
import io.github.thibaultbee.streampack.core.streamers.lifecycle.StreamerViewModelLifeCycleObserver
import io.github.thibaultbee.streampack.core.streamers.single.SingleStreamer
import io.github.thibaultbee.streampack.ui.views.PreviewView
import kotlinx.coroutines.launch

class PreviewFragment : Fragment(R.layout.main_fragment) {
    private lateinit var binding: MainFragmentBinding

    private val previewViewModel: PreviewViewModel by viewModels {
        PreviewViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = MainFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.viewmodel = previewViewModel

        bindProperties()
        return binding.root
    }

    @SuppressLint("MissingPermission")
    private fun bindProperties() {
        binding.liveButton.setOnClickListener { view ->
            view as ToggleButton
            if (view.isPressed) {
                if (view.isChecked) {
                    startStreamIfPermissions(previewViewModel.requiredPermissions)
                } else {
                    stopStream()
                }
            }
        }

        previewViewModel.streamerErrorLiveData.observe(viewLifecycleOwner) {
            showError("Oops", it)
        }

        previewViewModel.endpointErrorLiveData.observe(viewLifecycleOwner) {
            showError("Endpoint error", it)
        }

        previewViewModel.isStreamingLiveData.observe(viewLifecycleOwner) { isStreaming ->
            if (isStreaming) {
                lockOrientation()
            } else {
                unlockOrientation()
            }
            if (isStreaming) {
                binding.liveButton.isChecked = true
            } else if (previewViewModel.isTryingConnectionLiveData.value == true) {
                binding.liveButton.isChecked = true
            } else {
                binding.liveButton.isChecked = false
            }
        }

        previewViewModel.isTryingConnectionLiveData.observe(viewLifecycleOwner) { isWaitingForConnection ->
            if (isWaitingForConnection) {
                binding.liveButton.isChecked = true
            } else if (previewViewModel.isStreamingLiveData.value == true) {
                binding.liveButton.isChecked = true
            } else {
                binding.liveButton.isChecked = false
            }
        }

        previewViewModel.streamerLiveData.observe(viewLifecycleOwner) { streamer ->
            if (streamer is IStreamer) {
                // TODO: Remove this observer when streamer is released
                lifecycle.addObserver(StreamerViewModelLifeCycleObserver(streamer))
            } else {
                Log.e(TAG, "Streamer is not a ICoroutineStreamer")
            }
            if (streamer is IWithVideoSource) {
                inflateStreamerPreview(streamer)
            } else {
                Log.e(TAG, "Can't start preview, streamer is not a IVideoStreamer")
            }
        }
    }

    private fun lockOrientation() {
        /**
         * Lock orientation while stream is running to avoid stream interruption if
         * user turns the device.
         * For landscape only mode, set [requireActivity().requestedOrientation] to
         * [ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE].
         */
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
    }

    private fun unlockOrientation() {
        requireActivity().requestedOrientation = ApplicationConstants.supportedOrientation
    }

    private fun startStream() {
        previewViewModel.startStream()
    }

    private fun stopStream() {
        previewViewModel.stopStream()
    }

    private fun showPermissionError(vararg permissions: String) {
        Log.e(TAG, "Permission not granted: ${permissions.joinToString { ", " }}")
        DialogUtils.showPermissionAlertDialog(requireContext())
    }

    private fun showError(title: String, message: String) {
        Log.e(TAG, "Error: $title, $message")
        DialogUtils.showAlertDialog(requireContext(), "Error: $title", message)
    }

    @SuppressLint("MissingPermission")
    override fun onStart() {
        super.onStart()
        requestCameraAndMicrophonePermissions()
    }

    override fun onPause() {
        super.onPause()
        stopStream()
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private fun inflateStreamerPreview() {
        val streamer = previewViewModel.streamerLiveData.value
        if (streamer is SingleStreamer) {
            inflateStreamerPreview(streamer)
        } else {
            Log.e(TAG, "Can't start preview, streamer is not a SingleStreamer")
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private fun inflateStreamerPreview(streamer: SingleStreamer) {
        val preview = binding.preview
        // Set camera settings button when camera is started
        preview.listener = object : PreviewView.Listener {
            override fun onPreviewStarted() {
                Log.i(TAG, "Preview started")
            }

            override fun onZoomRationOnPinchChanged(zoomRatio: Float) {
                previewViewModel.onZoomRationOnPinchChanged()
            }
        }

        // Wait till streamer exists to set it to the SurfaceView.
        preview.streamer = streamer
        if (PermissionManager.hasPermissions(requireContext(), Manifest.permission.CAMERA)) {
            lifecycleScope.launch {
                try {
                    preview.startPreview()
                } catch (t: Throwable) {
                    Log.e(TAG, "Error starting preview", t)
                }
            }
        } else {
            Log.e(TAG, "Camera permission not granted. Preview will not start.")
        }
    }

    private fun startStreamIfPermissions(permissions: List<String>) {
        when {
            PermissionManager.hasPermissions(
                requireContext(), *permissions.toTypedArray()
            ) -> {
                startStream()
            }

            else -> {
                requestLiveStreamPermissionsLauncher.launch(
                    permissions.toTypedArray()
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestCameraAndMicrophonePermissions() {
        when {
            PermissionManager.hasPermissions(
                requireContext(), Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
            ) -> {
                inflateStreamerPreview()
                previewViewModel.configureAudio()
                previewViewModel.initializeVideoSource()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                showPermissionError(Manifest.permission.RECORD_AUDIO)
                requestCameraAndMicrophonePermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO
                    )
                )
            }

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showPermissionError(Manifest.permission.CAMERA)
                requestCameraAndMicrophonePermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.CAMERA
                    )
                )
            }

            else -> {
                requestCameraAndMicrophonePermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA
                    )
                )
            }
        }
    }

    private val requestLiveStreamPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val missingPermissions = permissions.toList().filter {
            !it.second
        }.map { it.first }

        if (missingPermissions.isEmpty()) {
            startStream()
        } else {
            showPermissionError(*missingPermissions.toTypedArray())
        }
    }

    @SuppressLint("MissingPermission")
    private val requestCameraAndMicrophonePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val missingPermissions = permissions.toList().filter {
            !it.second
        }.map { it.first }

        if (permissions[Manifest.permission.CAMERA] == true) {
            inflateStreamerPreview()
            previewViewModel.initializeVideoSource()
        } else if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
            previewViewModel.configureAudio()
        }
        if (missingPermissions.isNotEmpty()) {
            showPermissionError(*missingPermissions.toTypedArray())
        }
    }

    companion object {
        private const val TAG = "PreviewFragment"
    }
}
