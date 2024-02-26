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
import android.content.Context
import android.hardware.camera2.CaptureResult
import android.util.Log
import android.util.Range
import android.util.Rational
import androidx.annotation.RequiresPermission
import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.github.thibaultbee.streampack.app.BR
import io.github.thibaultbee.streampack.app.utils.ObservableViewModel
import io.github.thibaultbee.streampack.app.utils.StreamerManager
import io.github.thibaultbee.streampack.app.utils.isEmpty
import io.github.thibaultbee.streampack.error.StreamPackError
import io.github.thibaultbee.streampack.listeners.OnConnectionListener
import io.github.thibaultbee.streampack.listeners.OnErrorListener
import io.github.thibaultbee.streampack.streamers.StreamerLifeCycleObserver
import io.github.thibaultbee.streampack.utils.isFrameRateSupported
import io.github.thibaultbee.streampack.ui.views.PreviewView
import kotlinx.coroutines.launch

class PreviewViewModel(private val streamerManager: StreamerManager) : ObservableViewModel() {
    val streamerLifeCycleObserver: StreamerLifeCycleObserver
        get() = streamerManager.streamerLifeCycleObserver

    val streamerError = MutableLiveData<String>()

    val requiredPermissions: List<String>
        get() = streamerManager.requiredPermissions

    private val onErrorListener = object : OnErrorListener {
        override fun onError(error: StreamPackError) {
            Log.e(TAG, "onError", error)
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

    fun inflateStreamerView(view: PreviewView) {
        streamerManager.inflateStreamerView(view)
    }

    fun onPreviewStarted() {
        notifyCameraChanged()
    }

    fun onZoomRationOnPinchChanged() {
        notifyPropertyChanged(BR.zoomRatio)
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
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

    fun startStream() {
        viewModelScope.launch {
            try {
                streamerManager.startStream()
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

    fun setMute(isMuted: Boolean) {
        streamerManager.isMuted = isMuted
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
            notifyCameraChanged()
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

    val showZoomSlider = MutableLiveData(false)
    fun toggleZoomSlider() {
        showZoomSlider.postValue(!(showZoomSlider.value)!!)
    }

    val isZoomAvailable = MutableLiveData(false)
    val zoomRatioRange = MutableLiveData<Range<Float>>()
    var zoomRatio: Float
        @Bindable get() = streamerManager.cameraSettings?.zoom?.zoomRatio
            ?: 1f
        set(value) {
            streamerManager.cameraSettings?.zoom?.let {
                it.zoomRatio = value
                notifyPropertyChanged(BR.zoomRatio)
            }
        }

    val isAutoFocusModeAvailable = MutableLiveData(false)
    fun toggleAutoFocusMode() {
        streamerManager.cameraSettings?.let {
            val afModes = it.focus.availableAutoModes
            val index = afModes.indexOf(it.focus.autoMode)
            it.focus.autoMode = afModes[(index + 1) % afModes.size]
            if (it.focus.autoMode == CaptureResult.CONTROL_AF_MODE_OFF) {
                showLensDistanceSlider.postValue(true)
            } else {
                showLensDistanceSlider.postValue(false)
            }
        }
    }

    val showLensDistanceSlider = MutableLiveData(false)
    val lensDistanceRange = MutableLiveData<Range<Float>>()
    var lensDistance: Float
        @Bindable get() = streamerManager.cameraSettings?.focus?.lensDistance
            ?: 0f
        set(value) {
            streamerManager.cameraSettings?.focus?.let {
                it.lensDistance = value
                notifyPropertyChanged(BR.lensDistance)
            }
        }

    private fun notifyCameraChanged() {
        streamerManager.cameraSettings?.let {
            // Set optical stabilization first
            // Do not set both video and optical stabilization at the same time
            if (it.stabilization.availableOptical) {
                it.stabilization.enableOptical = true
            } else {
                it.stabilization.enableVideo = true
            }

            isAutoWhiteBalanceAvailable.postValue(it.whiteBalance.availableAutoModes.size > 1)
            isFlashAvailable.postValue(it.flash.available)

            it.exposure.let { exposure ->
                isExposureCompensationAvailable.postValue(
                    !exposure.availableCompensationRange.isEmpty
                )

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

            it.zoom.let { zoom ->
                isZoomAvailable.postValue(
                    !zoom.availableRatioRange.isEmpty
                )

                zoomRatioRange.postValue(zoom.availableRatioRange)
                zoomRatio = zoom.zoomRatio
            }

            it.focus.let { focus ->
                isAutoFocusModeAvailable.postValue(focus.availableAutoModes.size > 1)

                showLensDistanceSlider.postValue(false)
                lensDistanceRange.postValue(focus.availableLensDistanceRange)
                lensDistance = focus.lensDistance
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

    companion object {
        private const val TAG = "PreviewViewModel"
    }
}
