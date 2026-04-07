/*
 * Copyright (C) 2022 Thibault B.
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
package io.github.thibaultbee.streampack.core.streamers.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.github.thibaultbee.streampack.core.interfaces.ICloseableStreamer
import io.github.thibaultbee.streampack.core.interfaces.IStreamer
import io.github.thibaultbee.streampack.core.interfaces.IWithAudioSource
import io.github.thibaultbee.streampack.core.interfaces.releaseBlocking
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A [DefaultLifecycleObserver] to control a streamer on [Activity] lifecycle.
 *
 * It stops streamer when application goes to background and release it when application is destroyed.
 *
 * To use it, call:
 *  - `lifeCycle.addObserver(StreamerActivityLifeCycleObserver(streamer))`
 *
 *  @param streamer The streamer to control
 *  @param releaseOnDestroy Whether to release the streamer when application is destroyed. If a view model is used, this should be set to false.
 *  @param startAudioCaptureOnResume Whether to start audio capture when application is resumed.
 */
open class StreamerLifeCycleObserver(
    private val streamer: IStreamer,
    private val releaseOnDestroy: Boolean = false,
    private val startAudioCaptureOnResume: Boolean = false
) : DefaultLifecycleObserver {
    override fun onResume(owner: LifecycleOwner) {
        if (startAudioCaptureOnResume) {
            owner.lifecycleScope.launch {
                withContext(NonCancellable) {
                    if (streamer is IWithAudioSource) {
                        try {
                            streamer.audioInput.startCapture()
                        } catch (t: Throwable) {
                            Logger.e(TAG, "Error while starting audio capture: $t")
                        }
                    }
                }
            }
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        owner.lifecycleScope.launch {
            withContext(NonCancellable) {
                streamer.stopStream()
                if (streamer is ICloseableStreamer) {
                    streamer.close()
                }
                if (streamer is IWithAudioSource) {
                    streamer.audioInput.stopCapture()
                }
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        if (releaseOnDestroy) {
            streamer.releaseBlocking()
        }
    }

    companion object {
        private const val TAG = "StreamerLifeCycleObserver"
    }
}