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
import android.graphics.BitmapFactory
import android.hardware.camera2.CaptureResult
import android.util.Log
import android.util.Range
import android.util.Rational
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.databinding.Bindable
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import io.github.thibaultbee.streampack.app.BR
import io.github.thibaultbee.streampack.app.R
import io.github.thibaultbee.streampack.app.data.rotation.RotationRepository
import io.github.thibaultbee.streampack.app.data.storage.DataStoreRepository
import io.github.thibaultbee.streampack.app.ui.main.usecases.BuildStreamerUseCase
import io.github.thibaultbee.streampack.app.utils.ObservableViewModel
import io.github.thibaultbee.streampack.app.utils.dataStore
import io.github.thibaultbee.streampack.app.utils.isEmpty
import io.github.thibaultbee.streampack.app.utils.switchBackToFront
import io.github.thibaultbee.streampack.app.utils.toggleCamera
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.elements.endpoints.MediaSinkType
import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.AudioRecordSource
import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.MicrophoneSource
import io.github.thibaultbee.streampack.core.elements.sources.video.bitmap.BitmapSource
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraSettings
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraSource
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.ICameraSource
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.isFrameRateSupported
import io.github.thibaultbee.streampack.core.streamers.interfaces.IVideoStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.releaseBlocking
import io.github.thibaultbee.streampack.core.streamers.lifecycle.StreamerViewModelLifeCycleObserver
import io.github.thibaultbee.streampack.core.streamers.single.startStream
import io.github.thibaultbee.streampack.core.utils.extensions.isClosedException
import io.github.thibaultbee.streampack.ext.srt.regulator.controllers.DefaultSrtBitrateRegulatorController
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

    var streamer = buildStreamerUseCase()
        private set
    val streamerLifeCycleObserver: DefaultLifecycleObserver
        get() = StreamerViewModelLifeCycleObserver(streamer)

    /**
     * Test bitmap for [BitmapSource].
     */
    private val testBitmap =
        BitmapFactory.decodeResource(application.resources, R.drawable.img_test)

    /**
     * Camera settings.
     */
    private val cameraSettings: CameraSettings?
        get() {
            val videoSource = (streamer as? IVideoStreamer)?.videoSourceFlow?.value
            return (videoSource as? ICameraSource)?.settings
        }

    val requiredPermissions: List<String>
        get() {
            val permissions = mutableListOf<String>()
            if (streamer.videoSourceFlow is ICameraSource) {
                permissions.add(Manifest.permission.CAMERA)
            }
            if (streamer.audioSourceFlow.value is AudioRecordSource) {
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
        get() = streamer.isStreamingFlow.asLiveData()
    private val _isTryingConnection = MutableLiveData<Boolean>()
    val isTryingConnection: LiveData<Boolean> = _isTryingConnection

    init {
        viewModelScope.launch {
            // Set audio source and video source
            streamer.setAudioSource(MicrophoneSource.buildDefaultMicrophoneSource())
            streamer.setVideoSource(CameraSource(application))
        }
        viewModelScope.launch {
            streamer.videoSourceFlow.collect {
                notifySourceChanged()
            }
        }
        viewModelScope.launch {
            streamer.throwableFlow.filterNotNull().filter { !it.isClosedException }
                .map { "${it.javaClass.simpleName}: ${it.message}" }.collect {
                    _streamerError.postValue(it)
                }
        }
        viewModelScope.launch {
            streamer.throwableFlow.filterNotNull().filter { it.isClosedException }
                .map { "Connection lost: ${it.message}" }.collect {
                    _endpointError.postValue(it)
                }
        }
        viewModelScope.launch {
            streamer.isOpenFlow
                .collect {
                    Log.i(TAG, "Streamer is opened: $it")
                }
        }
        viewModelScope.launch {
            streamer.isStreamingFlow
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
                            streamer.setAudioConfig(it)
                        } ?: Log.i(TAG, "Audio is disabled")
                    }
                }
        }
        viewModelScope.launch {
            storageRepository.videoConfigFlow
                .collect { config ->
                    config?.let {
                        streamer.setVideoConfig(it)
                    } ?: Log.i(TAG, "Video is disabled")
                }
        }
    }

    fun onZoomRationOnPinchChanged() {
        notifyPropertyChanged(BR.zoomRatio)
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun configureAudio() {
        viewModelScope.launch {
            try {
                storageRepository.audioConfigFlow.first()?.let { streamer.setAudioConfig(it) }
                    ?: Log.i(
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
        streamer.audioProcessor?.isMuted = isMuted
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun switchBackToFront(): Boolean {
        /**
         * If video frame rate is not supported by the new camera, streamer will throw an
         * exception instead of crashing. You can either catch the exception or check if the
         * configuration is valid for the new camera with [Context.isFrameRateSupported].
         */
        val videoSource = streamer.videoSourceFlow.value
        if (videoSource is ICameraSource) {
            viewModelScope.launch {
                streamer.switchBackToFront(application)
                notifySourceChanged()
            }
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
        val videoSource = streamer.videoSourceFlow.value
        if (videoSource is ICameraSource) {
            viewModelScope.launch {
                streamer.toggleCamera(application)
                notifySourceChanged()
            }
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun toggleVideoSource() {
        val videoSource = streamer.videoSourceFlow.value
        viewModelScope.launch {
            val nextSource = when (videoSource) {
                is CameraSource -> {
                    BitmapSource(testBitmap)
                }

                is BitmapSource -> {
                    CameraSource(application)
                }

                else -> {
                    Log.i(TAG, "Unknown video source. Fallback to camera sources")
                    CameraSource(application)
                }
            }
            Log.i(TAG, "Switch video source to $nextSource")
            streamer.setVideoSource(nextSource)
        }
    }

    val isCameraSource = streamer.videoSourceFlow.map { it is ICameraSource }.asLiveData()

    val isFlashAvailable = MutableLiveData(false)
    fun toggleFlash() {
        cameraSettings?.let {
            it.flash.enable = !it.flash.enable
        } ?: Log.e(TAG, "Camera settings is not accessible")
    }

    val isAutoWhiteBalanceAvailable = MutableLiveData(false)
    fun toggleAutoWhiteBalanceMode() {
        cameraSettings?.let {
            val awbModes = it.whiteBalance.availableAutoModes
            val index = awbModes.indexOf(it.whiteBalance.autoMode)
            it.whiteBalance.autoMode = awbModes[(index + 1) % awbModes.size]
        } ?: Log.e(TAG, "Camera settings is not accessible")
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
            } ?: Log.e(TAG, "Camera settings is not accessible")
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
            } ?: Log.e(TAG, "Camera settings is not accessible")
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
        } ?: Log.e(TAG, "Camera settings is not accessible")
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
            } ?: Log.e(TAG, "Camera settings is not accessible")
        }

    private fun notifySourceChanged() {
        cameraSettings?.let {
            if (!it.isAvailable) {
                Log.e(TAG, "Camera settings are not available")
                return
            }

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
        } ?: Log.e(TAG, "Camera settings is not accessible")
    }

    override fun onCleared() {
        super.onCleared()
        try {
            streamer.releaseBlocking()
        } catch (t: Throwable) {
            Log.e(TAG, "Streamer release failed", t)
        }
    }

    companion object {
        private const val TAG = "PreviewViewModel"
    }
}
