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
import android.view.Surface
import androidx.annotation.RequiresPermission
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.thibaultbee.streampack.app.utils.StreamerManager
import com.github.thibaultbee.streampack.error.StreamPackError
import com.github.thibaultbee.streampack.listeners.OnConnectionListener
import com.github.thibaultbee.streampack.listeners.OnErrorListener
import com.github.thibaultbee.streampack.utils.isFrameRateSupported
import kotlinx.coroutines.launch
import java.io.File

class PreviewViewModel(private val streamerManager: StreamerManager) : ViewModel() {
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

    val isFlashAvailable = MutableLiveData<Boolean>()
    fun toggleFlash() {
        streamerManager.toggleFlash()
    }

    val isAutoWhiteBalanceAvailable = MutableLiveData<Boolean>()
    fun toggleAutoWhiteBalanceMode() {
        streamerManager.toggleAutoWhiteBalanceMode()
    }

    private fun notifyCameraChange() {
        isAutoWhiteBalanceAvailable.postValue(streamerManager.autoWhiteBalanceModes.size > 1)
        isFlashAvailable.postValue(streamerManager.isFlashAvailable)
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
