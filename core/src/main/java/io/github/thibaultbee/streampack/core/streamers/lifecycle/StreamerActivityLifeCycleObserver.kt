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
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICallbackStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICoroutineStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.IStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.releaseBlocking

/**
 * A [DefaultLifecycleObserver] to control a streamer on [Activity] lifecycle.
 *
 * It stops streamer when application goes to background and release it when application is destroyed.
 *
 * To use it, call:
 *  - `lifeCycle.addObserver(StreamerActivityLifeCycleObserver(streamer))`
 *
 *  @param streamer The streamer to control
 */
open class StreamerActivityLifeCycleObserver(streamer: IStreamer) :
    StreamerViewModelLifeCycleObserver(streamer) {

    override fun onDestroy(owner: LifecycleOwner) {
        if (streamer is ICoroutineStreamer) {
            streamer.releaseBlocking()
        } else if (streamer is ICallbackStreamer) {
            streamer.release()
        }
    }
}