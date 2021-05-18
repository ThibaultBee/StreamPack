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
package com.github.thibaultbee.streampack.internal.encoders

import com.github.thibaultbee.streampack.internal.data.Frame
import java.nio.ByteBuffer

interface IEncoderListener {
    /**
     * Calls when an encoder needs an input frame.
     * @param buffer ByteBuffer to fill. It comes from MediaCodec
     * @return frame with correct pts and buffer filled with an input buffer
     */
    fun onInputFrame(buffer: ByteBuffer): Frame

    /**
     * Calls when an encoder has generated an output frame.
     * @param frame Output frame with correct parameters and buffers
     */
    fun onOutputFrame(frame: Frame)
}