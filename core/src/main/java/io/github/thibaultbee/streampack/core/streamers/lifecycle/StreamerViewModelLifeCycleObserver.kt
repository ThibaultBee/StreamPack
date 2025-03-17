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
package io.github.thibaultbee.streampack.core.streamers.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICoroutineStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.IVideoStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.stopPreview
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
open class StreamerViewModelLifeCycleObserver(protected val streamer: ICoroutineStreamer) :
    DefaultLifecycleObserver {

    override fun onPause(owner: LifecycleOwner) {
        runBlocking {
            if (streamer is IVideoStreamer) {
                streamer.stopPreview()
            }
            streamer.stopStream()
            if (streamer.isOpenFlow.value) {
                streamer.close()
            }
        }
    }
}