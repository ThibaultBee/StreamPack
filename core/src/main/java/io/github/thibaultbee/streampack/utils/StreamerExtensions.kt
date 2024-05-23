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

package io.github.thibaultbee.streampack.utils

import io.github.thibaultbee.streampack.streamers.interfaces.ICameraStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.ICoroutineStreamer

/**
 * Get a streamer if it from generic class or interface
 */
inline fun <reified T> ICoroutineStreamer.getStreamer(): T? {
    return if (this is T) {
        this
    } else {
        null
    }
}

fun ICoroutineStreamer.getCameraStreamer(): ICameraStreamer? = getStreamer<ICameraStreamer>()