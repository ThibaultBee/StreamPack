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
import android.content.Context
import android.util.Log
import android.util.Range
import android.util.Rational
import android.view.Surface
import androidx.annotation.RequiresPermission
import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.thibaultbee.streampack.app.BR
import com.github.thibaultbee.streampack.app.utils.ObservableViewModel
import com.github.thibaultbee.streampack.app.utils.StreamerManager
import com.github.thibaultbee.streampack.error.StreamPackError
import com.github.thibaultbee.streampack.listeners.OnConnectionListener
import com.github.thibaultbee.streampack.listeners.OnErrorListener
import com.github.thibaultbee.streampack.utils.isFrameRateSupported
import kotlinx.coroutines.launch
import java.io.File

class PreviewViewModel(private val streamerManager: StreamerManager) : ObservableViewModel() {
    companion object {
        private const val TAG = "PreviewViewModel"
    }

    val cameraId: String
        get() = streamerManager.cameraId

    val streamerError = MutableLiveData<String>()

    val requiredPermissions: List<String>
        get() = streamerManager.requiredPermissions

    private val onErrorListener = object : OnErrorListener {
        override fun onError(error: StreamPackError) {
            streamerError.postValue("${error.javaClass.simpleName}: ${error.message}")
        }
    }

    private val onConnectionListener = object : OnConnectionListener {
        override fun onLost(message: String) {
            streamerError.postValue("Connection lost: $message")
        }

        override fun onFailed(message: String) {
            // Not needed as we catch startStream
        }

        override fun onSuccess() {
            Log.i(TAG, "Connection succeeded")
        }
    }

    fun createStreamer() {
        viewModelScope.launch {
            try {
                streamerManager.rebuildStreamer()
                streamerManager.onErrorListener = onErrorListener
                streamerManager.onConnectionListener = onConnectionListener
                Log.d(TAG, "Streamer is created")
            } catch (e: Throwable) {
                Log.e(TAG, "createStreamer failed", e)
                streamerError.postValue("createStreamer: ${e.message ?: "Unknown error"}")
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
    fun startPreview(previewSurface: Surface) {
        viewModelScope.launch {
            try {
                streamerManager.startPreview(previewSurface)
                notifyCameraChange()
            } catch (e: Throwable) {
                Log.e(TAG, "startPreview failed", e)
                streamerError.postValue("startPreview: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun stopPreview() {
        viewModelScope.launch {
            try {
                streamerManager.stopPreview()
            } catch (e: Throwable) {
                Log.e(TAG, "stopPreview failed", e)
            }
        }
    }

    fun startStream(filesDir: File) {
        viewModelScope.launch {
            try {
                streamerManager.startStream(filesDir)
            } catch (e: Throwable) {
                Log.e(TAG, "startStream failed", e)
                streamerError.postValue("startStream: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun stopStream() {
        viewModelScope.launch {
            try {
                streamerManager.stopStream()
            } catch (e: Throwable) {
                Log.e(TAG, "stopStream failed", e)
            }
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun toggleCamera() {
        /**
         * If video frame rate is not supported by the new camera, streamer will throw an
         * exception instead of crashing. You can either catch the exception or check if the
         * configuration is valid for the new camera with [Context.isFrameRateSupported].
         */
        try {
            streamerManager.toggleCamera()
            notifyCameraChange()
        } catch (e: Exception) {
            Log.e(TAG, "toggleCamera failed", e)
            streamerError.postValue("toggleCamera: ${e.message ?: "Unknown error"}")
        }
    }

    val isFlashAvailable = MutableLiveData(false)
    fun toggleFlash() {
        streamerManager.cameraSettings?.let {
            it.flash.enable = !it.flash.enable
        }
    }

    val isAutoWhiteBalanceAvailable = MutableLiveData(false)
    fun toggleAutoWhiteBalanceMode() {
        streamerManager.cameraSettings?.let {
            val awbModes = it.whiteBalance.availableAutoModes
            val index = awbModes.indexOf(it.whiteBalance.autoMode)
            it.whiteBalance.autoMode = awbModes[(index + 1) % awbModes.size]
        }
    }

    val showExposureSlider = MutableLiveData(false)
    fun toggleExposureSlider() {
        showExposureSlider.postValue(!(showExposureSlider.value)!!)
    }

    val isExposureCompensationAvailable = MutableLiveData(false)
    val exposureCompensationRange = MutableLiveData<Range<Int>>()
    val exposureCompensationStep = MutableLiveData<Rational>()
    var exposureCompensation: Float
        @Bindable get() = streamerManager.cameraSettings?.exposure?.let { it.compensation * it.availableCompensationStep.toFloat() }
            ?: 0f
        set(value) {
            streamerManager.cameraSettings?.exposure?.let {
                it.compensation = (value / it.availableCompensationStep.toFloat()).toInt()
                notifyPropertyChanged(BR.exposureCompensation)
            }
        }

    private fun notifyCameraChange() {
        streamerManager.cameraSettings?.let {
            isAutoWhiteBalanceAvailable.postValue(it.whiteBalance.availableAutoModes.size > 1)
            isFlashAvailable.postValue(it.flash.available)
            isExposureCompensationAvailable.postValue(
                it.exposure.availableCompensationRange != Range(
                    0,
                    0
                )
            )
            it.exposure.let { exposure ->
                exposureCompensationRange.postValue(
                    Range(
                        (exposure.availableCompensationRange.lower * exposure.availableCompensationStep.toFloat()).toInt(),
                        (exposure.availableCompensationRange.upper * exposure.availableCompensationStep.toFloat()).toInt()
                    )
                )
                exposureCompensationStep.postValue(exposure.availableCompensationStep)
                exposureCompensation =
                    exposure.compensation * exposure.availableCompensationStep.toFloat()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            streamerManager.release()
        } catch (e: Exception) {
            Log.e(TAG, "streamer.release failed", e)
        }
    }
}
