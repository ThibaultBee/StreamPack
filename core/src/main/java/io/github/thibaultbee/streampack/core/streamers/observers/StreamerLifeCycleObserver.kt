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
package io.github.thibaultbee.streampack.core.streamers.observers

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICoroutineStreamer
import io.github.thibaultbee.streampack.core.utils.getCameraStreamer
import kotlinx.coroutines.runBlocking

/**
 * Add [DefaultLifecycleObserver] to a streamer.
 *
 * You will not have to call [ICoroutineStreamer.release] when application is destroyed nor to to call
 * [ICoroutineStreamer.stopStream] when application goes to background.
 *
 * To use it, call:
 *  - `lifeCycle.addObserver(StreamerLifeCycleObserver(streamer))`
 *
 *  @param streamer The streamer to observe
 */
open class StreamerLifeCycleObserver(var streamer: ICoroutineStreamer) : DefaultLifecycleObserver {
    override fun onPause(owner: LifecycleOwner) {
        streamer.getCameraStreamer()?.stopPreview()
        runBlocking {
            streamer.stopStream()
            if (streamer.endpoint.isOpened.value) {
                streamer.close()
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        streamer.release()
    }
}