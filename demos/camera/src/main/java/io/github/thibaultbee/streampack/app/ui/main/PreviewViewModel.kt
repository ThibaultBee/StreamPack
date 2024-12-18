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
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CaptureResult
import android.util.Log
import android.util.Range
import android.util.Rational
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.databinding.Bindable
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import io.github.thibaultbee.streampack.app.BR
import io.github.thibaultbee.streampack.app.data.rotation.RotationRepository
import io.github.thibaultbee.streampack.app.data.storage.DataStoreRepository
import io.github.thibaultbee.streampack.app.ui.main.usecases.BuildStreamerUseCase
import io.github.thibaultbee.streampack.app.utils.ObservableViewModel
import io.github.thibaultbee.streampack.app.utils.dataStore
import io.github.thibaultbee.streampack.app.utils.isEmpty
import io.github.thibaultbee.streampack.app.utils.switchBackToFront
import io.github.thibaultbee.streampack.app.utils.toggleCamera
import io.github.thibaultbee.streampack.core.data.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.internal.endpoints.MediaSinkType
import io.github.thibaultbee.streampack.core.internal.sources.video.camera.CameraSettings
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICameraStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICoroutineStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.startStream
import io.github.thibaultbee.streampack.core.streamers.observers.StreamerLifeCycleObserver
import io.github.thibaultbee.streampack.core.utils.extensions.isClosedException
import io.github.thibaultbee.streampack.core.utils.extensions.isFrameRateSupported
import io.github.thibaultbee.streampack.ext.srt.regulator.controllers.DefaultSrtBitrateRegulatorController
import io.github.thibaultbee.streampack.ui.views.PreviewView
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PreviewViewModel(private val application: Application) : ObservableViewModel() {
    private val storageRepository = DataStoreRepository(application, application.dataStore)
    private val rotationRepository = RotationRepository.getInstance(application)

    private val buildStreamerUseCase = BuildStreamerUseCase(application, storageRepository)

    private var streamer = buildStreamerUseCase()
    val streamerLifeCycleObserver: StreamerLifeCycleObserver
        get() = ViewModelStreamerLifeCycleObserver(streamer)
    private val cameraSettings: CameraSettings?
        get() = (streamer as? ICameraStreamer)?.videoSource?.settings

    val requiredPermissions: List<String>
        get() {
            val permissions = mutableListOf<String>()
            if (streamer is ICameraStreamer) {
                permissions.add(Manifest.permission.CAMERA)
            }
            if (streamer.audioSource != null) {
                permissions.add(Manifest.permission.RECORD_AUDIO)
            }
            storageRepository.endpointDescriptorFlow.asLiveData().value?.let {
                if (it is UriMediaDescriptor) {
                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }

            return permissions
        }

    // Streamer errors
    private val _streamerError: MutableLiveData<String> = MutableLiveData()
    val streamerError: LiveData<String> = _streamerError
    private val _endpointError: MutableLiveData<String> = MutableLiveData()
    val endpointError: LiveData<String> = _endpointError

    // Streamer states
    val isStreaming: LiveData<Boolean>
        get() = streamer.isStreaming.asLiveData()
    private val _isTryingConnection = MutableLiveData<Boolean>()
    val isTryingConnection: LiveData<Boolean> = _isTryingConnection

    init {
        viewModelScope.launch {
            streamer.throwable.filterNotNull().filter { !it.isClosedException }
                .map { "${it.javaClass.simpleName}: ${it.message}" }.collect {
                    _streamerError.postValue(it)
                }
        }
        viewModelScope.launch {
            streamer.throwable.filterNotNull().filter { it.isClosedException }
                .map { "Connection lost: ${it.message}" }.collect {
                    _endpointError.postValue(it)
                }
        }
        viewModelScope.launch {
            streamer.isOpen
                .collect {
                    Log.i(TAG, "Streamer is opened: $it")
                }
        }
        viewModelScope.launch {
            streamer.isStreaming
                .collect {
                    Log.i(TAG, "Streamer is streaming: $it")
                }
        }
        viewModelScope.launch {
            rotationRepository.rotationFlow
                .collect {
                    streamer.targetRotation = it
                }
        }
        viewModelScope.launch {
            storageRepository.isAudioEnableFlow.combine(storageRepository.isVideoEnableFlow) { isAudioEnable, isVideoEnable ->
                Pair(isAudioEnable, isVideoEnable)
            }.collect { (_, _) ->
                val previousStreamer = streamer
                streamer = buildStreamerUseCase(previousStreamer)
                if (previousStreamer != streamer) {
                    previousStreamer.release()
                }
            }
        }
        viewModelScope.launch {
            storageRepository.audioConfigFlow
                .collect { config ->
                    if (ActivityCompat.checkSelfPermission(
                            application,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        config?.let {
                            streamer.configure(it)
                        } ?: Log.i(TAG, "Audio is disabled")
                    }
                }
        }
        viewModelScope.launch {
            storageRepository.videoConfigFlow
                .collect { config ->
                    config?.let {
                        streamer.configure(it)
                    } ?: Log.i(TAG, "Video is disabled")
                }
        }
    }

    fun setStreamerView(view: PreviewView) {
        if (streamer is ICameraStreamer) {
            view.streamer = streamer as ICameraStreamer
        }
    }

    fun onPreviewStarted() {
        notifyCameraChanged()
    }

    fun onZoomRationOnPinchChanged() {
        notifyPropertyChanged(BR.zoomRatio)
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun configureAudio() {
        viewModelScope.launch {
            try {
                storageRepository.audioConfigFlow.first()?.let { streamer.configure(it) } ?: Log.i(
                    TAG,
                    "Audio is disabled"
                )
            } catch (t: Throwable) {
                Log.e(TAG, "configureAudio failed", t)
                _streamerError.postValue("configureAudio: ${t.message ?: "Unknown error"}")
            }
        }
    }

    fun startStream() {
        viewModelScope.launch {
            _isTryingConnection.postValue(true)
            try {
                val descriptor = storageRepository.endpointDescriptorFlow.first()
                streamer.startStream(descriptor)

                if (descriptor.type.sinkType == MediaSinkType.SRT) {
                    val bitrateRegulatorConfig =
                        storageRepository.bitrateRegulatorConfigFlow.first()
                    if (bitrateRegulatorConfig != null) {
                        Log.i(TAG, "Add bitrate regulator controller")
                        streamer.addBitrateRegulatorController(
                            DefaultSrtBitrateRegulatorController.Factory(
                                bitrateRegulatorConfig = bitrateRegulatorConfig
                            )
                        )
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "startStream failed", e)
                _streamerError.postValue("startStream: ${e.message ?: "Unknown error"}")
            } finally {
                _isTryingConnection.postValue(false)
            }
        }
    }

    fun stopStream() {
        viewModelScope.launch {
            try {
                streamer.stopStream()
                streamer.close()
                streamer.removeBitrateRegulatorController()
            } catch (e: Throwable) {
                Log.e(TAG, "stopStream failed", e)
            }
        }
    }

    fun setMute(isMuted: Boolean) {
        streamer.audioSource?.isMuted = isMuted
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun switchBackToFront(): Boolean {
        /**
         * If video frame rate is not supported by the new camera, streamer will throw an
         * exception instead of crashing. You can either catch the exception or check if the
         * configuration is valid for the new camera with [Context.isFrameRateSupported].
         */
        val streamer = streamer
        if (streamer is ICameraStreamer) {
            streamer.switchBackToFront(application)
            notifyCameraChanged()
        }
        return true
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun toggleCamera() {
        /**
         * If video frame rate is not supported by the new camera, streamer will throw an
         * exception instead of crashing. You can either catch the exception or check if the
         * configuration is valid for the new camera with [Context.isFrameRateSupported].
         */
        val streamer = streamer
        if (streamer is ICameraStreamer) {
            streamer.toggleCamera(application)
            notifyCameraChanged()
        }
    }

    val isFlashAvailable = MutableLiveData(false)
    fun toggleFlash() {
        cameraSettings?.let {
            it.flash.enable = !it.flash.enable
        }
    }

    val isAutoWhiteBalanceAvailable = MutableLiveData(false)
    fun toggleAutoWhiteBalanceMode() {
        cameraSettings?.let {
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
        @Bindable get() =
            cameraSettings?.exposure?.let { it.compensation * it.availableCompensationStep.toFloat() }
                ?: 0f
        set(value) {
            cameraSettings?.exposure?.let {
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
        @Bindable get() = cameraSettings?.zoom?.zoomRatio
            ?: 1f
        set(value) {
            cameraSettings?.zoom?.let {
                it.zoomRatio = value
                notifyPropertyChanged(BR.zoomRatio)
            }
        }

    val isAutoFocusModeAvailable = MutableLiveData(false)
    fun toggleAutoFocusMode() {
        cameraSettings?.let {
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
        @Bindable get() = cameraSettings?.focus?.lensDistance
            ?: 0f
        set(value) {
            cameraSettings?.focus?.let {
                it.lensDistance = value
                notifyPropertyChanged(BR.lensDistance)
            }
        }

    private fun notifyCameraChanged() {
        cameraSettings?.let {
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
            streamer.release()
        } catch (t: Throwable) {
            Log.e(TAG, "Streamer release failed", t)
        }
    }

    companion object {
        private const val TAG = "PreviewViewModel"
    }

    class ViewModelStreamerLifeCycleObserver(streamer: ICoroutineStreamer) :
        StreamerLifeCycleObserver(streamer) {
        override fun onDestroy(owner: LifecycleOwner) {
            // Do nothing
            // The ViewModel onCleared() method will call release() method
        }
    }
}
