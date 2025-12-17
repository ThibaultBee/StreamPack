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
import io.github.thibaultbee.streampack.app.utils.setNextCameraId
import io.github.thibaultbee.streampack.app.utils.toggleBackToFront
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.elements.endpoints.MediaSinkType
import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.IAudioRecordSource
import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.MicrophoneSourceFactory
import io.github.thibaultbee.streampack.core.elements.sources.video.bitmap.BitmapSourceFactory
import io.github.thibaultbee.streampack.core.elements.sources.video.bitmap.IBitmapSource
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraSettings
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraSourceFactory
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.ICameraSource
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.cameraManager
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.defaultCameraId
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoSource
import io.github.thibaultbee.streampack.core.interfaces.releaseBlocking
import io.github.thibaultbee.streampack.core.interfaces.startStream
import io.github.thibaultbee.streampack.core.streamers.single.SingleStreamer
import io.github.thibaultbee.streampack.core.utils.extensions.isClosedException
import io.github.thibaultbee.streampack.ext.srt.regulator.controllers.DefaultSrtBitrateRegulatorController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PreviewViewModel(private val application: Application) : ObservableViewModel() {
    private val storageRepository = DataStoreRepository(application, application.dataStore)
    private val rotationRepository = RotationRepository.getInstance(application)

    private val buildStreamerUseCase = BuildStreamerUseCase(application, storageRepository)

    private val streamerFlow =
        MutableStateFlow(
            SingleStreamer(
                application,
                runBlocking { storageRepository.isAudioEnableFlow.first() }) // TODO avoid runBlocking
        )
    private val streamer: SingleStreamer
        get() = streamerFlow.value
    val streamerLiveData = streamerFlow.asLiveData()

    /**
     * Test bitmap for [BitmapSource].
     */
    private val testBitmap =
        BitmapFactory.decodeResource(application.resources, R.drawable.img_test)

    private val defaultCameraId = application.cameraManager.defaultCameraId

    /**
     * Camera settings.
     */
    private val cameraSettings: CameraSettings?
        get() {
            val videoSource = (streamer as? IWithVideoSource)?.videoInput?.sourceFlow?.value
            return (videoSource as? ICameraSource)?.settings
        }

    val requiredPermissions: List<String>
        get() {
            val permissions = mutableListOf<String>()
            if (streamer.videoInput?.sourceFlow is ICameraSource) {
                permissions.add(Manifest.permission.CAMERA)
            }
            if (streamer.audioInput?.sourceFlow?.value is IAudioRecordSource) {
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
    private val _streamerErrorLiveData: MutableLiveData<String> = MutableLiveData()
    val streamerErrorLiveData: LiveData<String> = _streamerErrorLiveData
    private val _endpointErrorLiveData: MutableLiveData<String> = MutableLiveData()
    val endpointErrorLiveData: LiveData<String> = _endpointErrorLiveData

    // Streamer states
    private val _isStreamingFlow = MutableStateFlow(false)
    val isStreamingLiveData: LiveData<Boolean>
        get() = _isStreamingFlow.asLiveData()
    private val _isTryingConnectionLiveData = MutableLiveData<Boolean>()
    val isTryingConnectionLiveData: LiveData<Boolean> = _isTryingConnectionLiveData

    private val videoSourceMutex = Mutex()

    private var startStreamJob: Job? = null

    init {
        viewModelScope.launch {
            streamerFlow.collect { streamer ->
                // Set audio source and video source
                if (streamer.withAudio) {
                    Log.i(TAG, "Audio source is enabled. Setting audio source")
                    streamer.setAudioSource(MicrophoneSourceFactory())
                } else {
                    Log.i(TAG, "Audio source is disabled")
                }
                if (streamer.withVideo) {
                    if (ActivityCompat.checkSelfPermission(
                            application,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        initializeVideoSource()
                    }
                } else {
                    Log.i(TAG, "Video source is disabled")
                }

                // TODO: cancel jobs linked to previous streamer
                viewModelScope.launch {
                    streamer.videoInput?.sourceFlow?.collect {
                        notifySourceChanged()
                    }
                }

                viewModelScope.launch {
                    streamer.throwableFlow.filterNotNull().filter { !it.isClosedException }
                        .map { "${it.javaClass.simpleName}: ${it.message}" }.collect {
                            _streamerErrorLiveData.postValue(it)
                        }
                }
                viewModelScope.launch {
                    streamer.throwableFlow.filterNotNull().filter { it.isClosedException }
                        .map { "Connection lost: ${it.message}" }.collect {
                            _endpointErrorLiveData.postValue(it)
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
                        .collect { isStreaming ->
                            _isStreamingFlow.emit(isStreaming)
                            Log.i(TAG, "Streamer is streaming: $isStreaming")
                        }
                }
            }
        }

        viewModelScope.launch {
            rotationRepository.rotationFlow
                .collect {
                    streamer.setTargetRotation(it)
                }
        }
        viewModelScope.launch {
            storageRepository.isAudioEnableFlow.combine(storageRepository.isVideoEnableFlow) { isAudioEnable, isVideoEnable ->
                Pair(isAudioEnable, isVideoEnable)
            }.drop(1).collect { (isAudioEnable, _) ->
                streamerFlow.emit(buildStreamerUseCase(streamer, isAudioEnable))
            }
        }
        viewModelScope.launch {
            storageRepository.audioConfigFlow.filterNotNull()
                .collect { config ->
                    if (!streamer.withAudio) {
                        Log.i(TAG, "Audio is disabled. Skip setting audio config")
                        return@collect
                    }
                    if (ActivityCompat.checkSelfPermission(
                            application,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        try {
                            streamer.setAudioConfig(config)
                        } catch (t: Throwable) {
                            Log.e(TAG, "setAudioConfig failed", t)
                            _streamerErrorLiveData.postValue("setAudioConfig: ${t.message ?: "Unknown error"}")
                        }
                    }
                }
        }
        viewModelScope.launch {
            storageRepository.videoConfigFlow.filterNotNull()
                .collect { config ->
                    try {
                        streamer.setVideoConfig(config)
                    } catch (t: Throwable) {
                        Log.e(TAG, "setVideoConfig failed", t)
                        _streamerErrorLiveData.postValue("setVideoConfig: ${t.message ?: "Unknown error"}")
                    }
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
                _streamerErrorLiveData.postValue("configureAudio: ${t.message ?: "Unknown error"}")
            }
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun initializeVideoSource() {
        viewModelScope.launch {
            videoSourceMutex.withLock {
                if (streamer.videoInput?.sourceFlow?.value == null) {
                    streamer.setVideoSource(CameraSourceFactory(defaultCameraId))
                } else {
                    Log.i(TAG, "Camera source already set")
                }
            }
        }
    }

    fun startStream() {
        startStreamJob = viewModelScope.launch {
            _isTryingConnectionLiveData.postValue(true)
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
            } catch (e: CancellationException) {
                Log.i(TAG, "startStream cancelled", e)
                _streamerErrorLiveData.postValue("startStream cancelled")
            } catch (t: Throwable) {
                Log.e(TAG, "startStream failed", t)
                _streamerErrorLiveData.postValue("startStream: ${t.message ?: "Unknown error"}")
            } finally {
                _isTryingConnectionLiveData.postValue(false)
            }
        }
    }

    fun stopStream() {
        startStreamJob?.cancel()
        startStreamJob = null

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
        streamer.audioInput?.isMuted = isMuted
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun switchBackToFront(): Boolean {
        /**
         * If video frame rate is not supported by the new camera, streamer will throw an
         * exception instead of crashing. You can either catch the exception or check if the
         * configuration is valid for the new camera with [Context.isFpsSupported].
         */
        val videoSource = streamer.videoInput?.sourceFlow?.value
        if (videoSource is ICameraSource) {
            viewModelScope.launch {
                streamer.toggleBackToFront(application)
            }
        }
        return true
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun toggleCamera() {
        /**
         * If video frame rate is not supported by the new camera, streamer will throw an
         * exception instead of crashing. You can either catch the exception or check if the
         * configuration is valid for the new camera with [Context.isFpsSupported].
         */
        viewModelScope.launch {
            videoSourceMutex.withLock {
                val videoSource = streamer.videoInput?.sourceFlow?.value
                if (videoSource is ICameraSource) {
                    streamer.setNextCameraId(application)
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun toggleVideoSource() {
        viewModelScope.launch {
            videoSourceMutex.withLock {
                val videoSource = streamer.videoInput?.sourceFlow?.value
                val nextSource = when (videoSource) {
                    is ICameraSource -> {
                        BitmapSourceFactory(testBitmap)
                    }

                    is IBitmapSource -> {
                        CameraSourceFactory(defaultCameraId)
                    }

                    else -> {
                        Log.i(TAG, "Unknown video source. Fallback to camera sources")
                        CameraSourceFactory(defaultCameraId)
                    }
                }
                Log.i(TAG, "Switch video source to $nextSource")
                streamer.setVideoSource(nextSource)
            }
        }
    }

    val isCameraSource = streamer.videoInput?.sourceFlow?.map { it is ICameraSource }?.asLiveData()

    val isFlashAvailable = MutableLiveData(false)
    fun toggleFlash() {
        cameraSettings?.let {
            viewModelScope.launch {
                it.flash.setIsEnable(!it.flash.isEnable)
            }
        } ?: Log.e(TAG, "Camera settings is not accessible")
    }

    val isAutoWhiteBalanceAvailable = MutableLiveData(false)
    fun toggleAutoWhiteBalanceMode() {
        cameraSettings?.let { settings ->
            val awbModes = settings.whiteBalance.availableAutoModes
            val index = awbModes.indexOf(settings.whiteBalance.autoMode)
            viewModelScope.launch {
                settings.whiteBalance.setAutoMode(awbModes[(index + 1) % awbModes.size])
            }
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
        @Bindable get() {
            val settings = cameraSettings
            return if (settings != null && settings.isActiveFlow.value) {
                settings.exposure.compensation * settings.exposure.availableCompensationStep.toFloat()
            } else {
                0f
            }
        }
        set(value) {
            cameraSettings?.let { settings ->
                settings.exposure.let {
                    viewModelScope.launch {
                        if (settings.isActiveFlow.value) {
                            it.setCompensation((value / it.availableCompensationStep.toFloat()).toInt())
                        }
                        notifyPropertyChanged(BR.exposureCompensation)
                    }
                }
            } ?: Log.e(TAG, "Camera settings is not accessible")
        }

    val showZoomSlider = MutableLiveData(false)
    fun toggleZoomSlider() {
        showZoomSlider.postValue(!(showZoomSlider.value)!!)
    }

    val isZoomAvailable = MutableLiveData(false)
    val zoomRatioRange = MutableLiveData<Range<Float>>()
    var zoomRatio: Float
        @Bindable get() {
            val settings = cameraSettings
            return if (settings != null && settings.isActiveFlow.value) {
                runBlocking {
                    settings.zoom.getZoomRatio()
                }
            } else {
                1f
            }
        }
        set(value) {
            cameraSettings?.let { settings ->
                viewModelScope.launch {
                    if (settings.isActiveFlow.value) {
                        settings.zoom.setZoomRatio(value)
                    }
                    notifyPropertyChanged(BR.zoomRatio)
                }
            } ?: Log.e(TAG, "Camera settings is not accessible")
        }

    val isAutoFocusModeAvailable = MutableLiveData(false)
    fun toggleAutoFocusMode() {
        cameraSettings?.let {
            val afModes = it.focus.availableAutoModes
            val index = afModes.indexOf(it.focus.autoMode)
            viewModelScope.launch {
                it.focus.setAutoMode(afModes[(index + 1) % afModes.size])
                if (it.focus.autoMode == CaptureResult.CONTROL_AF_MODE_OFF) {
                    showLensDistanceSlider.postValue(true)
                } else {
                    showLensDistanceSlider.postValue(false)
                }
            }
        } ?: Log.e(TAG, "Camera settings is not accessible")
    }

    val showLensDistanceSlider = MutableLiveData(false)
    val lensDistanceRange = MutableLiveData<Range<Float>>()
    var lensDistance: Float
        @Bindable get() {
            val settings = cameraSettings
            return if ((settings != null) &&
                settings.isActiveFlow.value
            ) {
                settings.focus.lensDistance
            } else {
                0f
            }
        }
        set(value) {
            cameraSettings?.let { settings ->
                settings.focus.let {
                    viewModelScope.launch {
                        if (settings.isActiveFlow.value) {
                            it.setLensDistance(value)
                        }
                        notifyPropertyChanged(BR.lensDistance)
                    }
                }
            } ?: Log.e(TAG, "Camera settings is not accessible")
        }

    private fun notifySourceChanged() {
        val videoSource = streamer.videoInput?.sourceFlow?.value ?: return
        if (videoSource is ICameraSource) {
            notifyCameraChanged(videoSource)
        } else {
            isFlashAvailable.postValue(false)
            isAutoWhiteBalanceAvailable.postValue(false)
            isExposureCompensationAvailable.postValue(false)
            isZoomAvailable.postValue(false)
            isAutoFocusModeAvailable.postValue(false)
        }
    }

    private fun notifyCameraChanged(videoSource: ICameraSource) {
        val settings = videoSource.settings
        // Set optical stabilization first
        // Do not set both video and optical stabilization at the same time
        if (settings.isActiveFlow.value) {
            viewModelScope.launch {
                if (settings.stabilization.isOpticalAvailable) {
                    settings.stabilization.setIsEnableOptical(true)
                } else {
                    settings.stabilization.setIsEnableVideo(true)
                }
            }
        }

        // Flash
        isFlashAvailable.postValue(settings.flash.isAvailable)

        // WB
        isAutoWhiteBalanceAvailable.postValue(settings.whiteBalance.availableAutoModes.size > 1)

        // Exposure
        isExposureCompensationAvailable.postValue(
            !settings.exposure.availableCompensationRange.isEmpty
        )
        exposureCompensationRange.postValue(
            Range(
                (settings.exposure.availableCompensationRange.lower * settings.exposure.availableCompensationStep.toFloat()).toInt(),
                (settings.exposure.availableCompensationRange.upper * settings.exposure.availableCompensationStep.toFloat()).toInt()
            )
        )
        exposureCompensationStep.postValue(settings.exposure.availableCompensationStep)
        exposureCompensation = 0f

        // Zoom
        isZoomAvailable.postValue(
            !settings.zoom.availableRatioRange.isEmpty
        )
        zoomRatioRange.postValue(settings.zoom.availableRatioRange)
        zoomRatio = 1.0f

        // Focus
        isAutoFocusModeAvailable.postValue(settings.focus.availableAutoModes.size > 1)

        // Lens distance
        showLensDistanceSlider.postValue(false)
        lensDistanceRange.postValue(settings.focus.availableLensDistanceRange)
        lensDistance = 0f
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
