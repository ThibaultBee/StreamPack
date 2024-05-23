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
package io.github.thibaultbee.streampack.internal.endpoints.composites.muxers

import io.github.thibaultbee.streampack.data.Config
import io.github.thibaultbee.streampack.internal.data.Frame
import io.github.thibaultbee.streampack.internal.data.Packet
import io.github.thibaultbee.streampack.internal.interfaces.Releaseable
import io.github.thibaultbee.streampack.internal.interfaces.Streamable

interface IMuxer :
    IPublicMuxer, Streamable,
    Releaseable {
    var listener: IMuxerListener?

    interface IMuxerListener {
        fun onOutputFrame(packet: Packet)
    }

    fun write(frame: Frame, streamPid: Int)

    fun addStreams(streamsConfig: List<Config>): Map<Config, Int>

    fun addStream(streamConfig: Config): Int
}

interface IPublicMuxer {
    val info: IMuxerInfo

    interface IMuxerInfo {
        val audio: IAudioMuxerInfo

        interface IAudioMuxerInfo {
            val supportedEncoders: List<String>

            /**
             * Supported sample rates in Hz
             *
             * Returns null if not applicable (no limitation)
             */
            val supportedSampleRates: List<Int>?

            /**
             * Supported byte formats
             *
             * Returns null if not applicable (no limitation)
             */
            val supportedByteFormats: List<Int>?
        }

        val video: IVideoMuxerInfo

        interface IVideoMuxerInfo {
            val supportedEncoders: List<String>
        }
    }
}