/*
 * Copyright (C) 2024 Thibault B.
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
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICameraStreamer
import io.github.thibaultbee.streampack.core.streamers.single.ICoroutineSingleStreamer
import kotlinx.coroutines.runBlocking

/**
 * A [DefaultLifecycleObserver] to control a streamer on [Activity] lifecycle in a ViewModel.
 *
 * It stops streamer when application goes to background.
 *
 * To use it, call:
 *  - `lifeCycle.addObserver(StreamerActivityLifeCycleObserver(streamer))`
 *
 *  @param streamer The streamer to control
 */
open class StreamerViewModelLifeCycleObserver(protected val streamer: ICoroutineSingleStreamer) :
    DefaultLifecycleObserver {
    override fun onPause(owner: LifecycleOwner) {
        if (streamer is ICameraStreamer) {
            streamer.stopPreview()
        }
        runBlocking {
            streamer.stopStream()
            if (streamer.endpoint.isOpen.value) {
                streamer.close()
            }
        }
    }
}