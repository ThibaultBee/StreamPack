/*
 * Copyright (C) 2025 Thibault B.
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
package io.github.thibaultbee.streampack.core.elements.processing.audio

import io.github.thibaultbee.streampack.core.elements.data.Frame

open class AudioFrameProcessor(onFrame: (Frame) -> Unit) : FrameProcessor(onFrame),
    IAudioFrameProcessor {
    override var isMuted = false
    private var mutedByteArray: ByteArray? = null

    override fun processFrame(frame: Frame): Frame {
        if (isMuted) {
            val remaining = frame.buffer.remaining()
            val position = frame.buffer.position()
            if (remaining != mutedByteArray?.size) {
                mutedByteArray = ByteArray(remaining)
            }
            frame.buffer.put(mutedByteArray!!)
            frame.buffer.position(position)
            return frame
        }
        return frame
    }
}