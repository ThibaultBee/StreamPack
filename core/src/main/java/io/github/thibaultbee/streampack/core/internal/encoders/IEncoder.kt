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
package io.github.thibaultbee.streampack.core.internal.encoders

import android.view.Surface
import io.github.thibaultbee.streampack.core.internal.data.Frame
import io.github.thibaultbee.streampack.core.internal.interfaces.Releaseable
import io.github.thibaultbee.streampack.core.internal.interfaces.SuspendStreamable
import java.nio.ByteBuffer
import java.util.concurrent.Executor

interface IPublicEncoder {

    /**
     * The encoder mime type
     * @see List of audio/video mime type on <a href="https://developer.android.com/reference/android/media/MediaFormat">Android developer guide</a>
     */
    val mimeType: String

    /**
     * The bitrate at which the encoder will start (ie the one in the configuration)
     */
    val startBitrate: Int

    /**
     * Gets/sets encoder bitrate.
     *
     * The setter is only applicable for video encoders. On audio encoders, it will throw an exception.
     */
    var bitrate: Int

    /**
     * The encoder info like the supported resolutions, bitrates, etc.
     */
    val info: IEncoderInfo

    interface IEncoderInfo {
        /**
         * The encoder name
         */
        val name: String
    }

    /**
     * Force the encoder to generate a key frame.
     */
    fun requestKeyFrame()
}

interface IEncoder : SuspendStreamable, Releaseable,
    IPublicEncoder {
    interface IListener {
        /**
         * Calls when an encoder has an error.
         */
        fun onError(e: Exception) {}

        /**
         * Calls when an encoder has generated an output frame.
         * @param frame Output frame with correct parameters and buffers
         */
        fun onOutputFrame(frame: Frame) {}
    }

    /**
     * Set the encoder listener
     *
     * @param listener the listener
     * @param listenerExecutor the executor where the listener will be called
     */
    fun setListener(
        listener: IListener,
        listenerExecutor: Executor
    )

    /**
     * The encoder input.
     * @see IEncoderInput
     */
    val input: IEncoderInput

    interface IEncoderInput

    /**
     * The [Surface] input
     */
    interface ISurfaceInput :
        IEncoderInput {
        /**
         * The surface update listener
         */
        var listener: OnSurfaceUpdateListener

        interface OnSurfaceUpdateListener {
            fun onSurfaceUpdated(surface: Surface) {}
        }
    }

    /**
     * The [ByteBuffer] input
     */
    interface IByteBufferInput :
        IEncoderInput {
        /**
         * The buffer available listener
         */
        var listener: OnFrameRequestedListener

        interface OnFrameRequestedListener {
            /**
             * Calls when a buffer is available for encoding.
             *
             * @param buffer the buffer where to write the frame
             */
            fun onFrameRequested(buffer: ByteBuffer): Frame
        }
    }

    /**
     * Reset the encoder
     */
    fun reset()

    /**
     * Configure the encoder
     */
    fun configure()
}
