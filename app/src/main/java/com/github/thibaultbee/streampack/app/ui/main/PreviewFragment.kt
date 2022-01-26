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
package com.github.thibaultbee.streampack.app.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.thibaultbee.streampack.app.configuration.Configuration
import com.github.thibaultbee.streampack.app.databinding.MainFragmentBinding
import com.github.thibaultbee.streampack.app.utils.DialogUtils
import com.github.thibaultbee.streampack.app.utils.StreamerManager
import com.github.thibaultbee.streampack.utils.getCameraCharacteristics
import com.github.thibaultbee.streampack.views.getPreviewOutputSize
import com.jakewharton.rxbinding4.view.clicks
import com.tbruyelle.rxpermissions3.RxPermissions
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable

class PreviewFragment : Fragment() {
    private val fragmentDisposables = CompositeDisposable()
    private lateinit var binding: MainFragmentBinding

    companion object {
        private const val TAG = "PreviewFragment"
    }

    private val viewModel: PreviewViewModel by lazy {
        ViewModelProvider(
            this,
            PreviewViewModelFactory(
                StreamerManager(
                    requireContext(),
                    Configuration(requireContext())
                )
            )
        )[PreviewViewModel::class.java]
    }

    private val rxPermissions: RxPermissions by lazy { RxPermissions(this) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MainFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.viewmodel = viewModel
        bindProperties()
        return binding.root
    }

    @SuppressLint("MissingPermission")
    private fun bindProperties() {
        binding.liveButton.clicks()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                rxPermissions
                    .requestEachCombined(*viewModel.requiredPermissions.toTypedArray())
                    .subscribe { permission ->
                        if (!permission.granted) {
                            binding.liveButton.isChecked = false
                            showPermissionError()
                        } else {
                            if (binding.liveButton.isChecked) {
                                startStream()
                            } else {
                                stopStream()
                            }
                        }
                    }
            }
            .let(fragmentDisposables::add)

        viewModel.streamerError.observe(viewLifecycleOwner) {
            showError("Oops", it)
        }
    }


    private fun startStream() {
        /**
         * Lock orientation while stream is running to avoid stream interruption if
         * user turns the device.
         * For landscape only mode, set [Activity.requestedOrientation] to
         * [ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE] in [onCreate] or [onResume].
         */
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        viewModel.startStream(requireContext().filesDir)
    }

    private fun unLockScreen() {
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    private fun stopStream() {
        viewModel.stopStream()
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

        rxPermissions
            .requestEachCombined(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            .subscribe { permission ->
                if (!permission.granted) {
                    showPermissionErrorAndFinish()
                } else {
                    viewModel.createStreamer()
                    // Wait till streamer exists to create the SurfaceView (and call startCapture).
                    binding.preview.visibility = View.VISIBLE
                }
            }
        binding.preview.holder.addCallback(surfaceViewCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        fragmentDisposables.clear()
    }

    @SuppressLint("MissingPermission")
    private val surfaceViewCallback = object : SurfaceHolder.Callback {
        override fun surfaceDestroyed(holder: SurfaceHolder) {
            viewModel.stopPreview()
            binding.preview.holder.removeCallback(this)
        }

        override fun surfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) = Unit

        override fun surfaceCreated(holder: SurfaceHolder) {
            // Selects appropriate preview size and configures view finder
            viewModel.cameraId?.let {
                val previewSize = getPreviewOutputSize(
                    binding.preview.display,
                    requireContext().getCameraCharacteristics(it),
                    SurfaceHolder::class.java
                )
                Log.d(
                    TAG,
                    "View finder size: ${binding.preview.width} x ${binding.preview.height}"
                )
                Log.d(TAG, "Selected preview size: $previewSize")
                binding.preview.setAspectRatio(previewSize.width, previewSize.height)

                // To ensure that size is set, initialize camera in the view's thread
                binding.preview.post { viewModel.startPreview(holder.surface) }
            }
        }
    }
}
