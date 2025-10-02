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
package io.github.thibaultbee.streampack.core.elements.encoders

import android.view.Surface
import io.github.thibaultbee.streampack.core.elements.data.Frame
import io.github.thibaultbee.streampack.core.elements.data.RawFrame
import io.github.thibaultbee.streampack.core.elements.interfaces.Releasable
import io.github.thibaultbee.streampack.core.elements.interfaces.SuspendStreamable
import kotlinx.coroutines.channels.SendChannel
import java.nio.ByteBuffer

interface IEncoder {
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

interface IEncoderInternal : SuspendStreamable, Releasable,
    IEncoder {

    /**
     * The encoder configuration
     */
    val config: CodecConfig

    interface IListener {
        /**
         * Calls when an encoder has an error.
         */
        fun onError(t: Throwable) {}

        /**
         * A channel where the encoder will send encoded frames.
         */
        val outputChannel: SendChannel<Frame>
    }

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
         * The surface where to write the frame
         */
        val surface: Surface?

        /**
         * The surface update listener
         */
        var listener: OnSurfaceUpdateListener

        interface OnSurfaceUpdateListener {
            fun onSurfaceUpdated(surface: Surface) {}
        }
    }

    /**
     * The [IAsyncByteBufferInput] input
     */
    interface IAsyncByteBufferInput :
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
            fun onFrameRequested(buffer: ByteBuffer): RawFrame
        }
    }

    /**
     * The [ISyncByteBufferInput] input
     */
    interface ISyncByteBufferInput :
        IEncoderInput {
        fun queueInputFrame(frame: RawFrame)
    }

    /**
     * Reset the encoder
     */
    suspend fun reset()

    /**
     * Configure the encoder
     */
    suspend fun configure()
}

enum class EncoderMode {
    /**
     * Encoder will encode frames from a [Surface]
     */
    SURFACE,

    /**
     * Encoder will encode frames from a [ByteBuffer]
     */
    SYNC,

    /**
     * Encoder will encode frames from a [ByteBuffer] asynchronously
     */
    ASYNC
}
