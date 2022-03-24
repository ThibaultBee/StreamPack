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
package io.github.thibaultbee.streampack.internal.muxers.flv.packet

import android.content.Context
import io.github.thibaultbee.streampack.data.AudioConfig
import io.github.thibaultbee.streampack.data.Config
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.internal.muxers.flv.amf.containers.AmfContainer
import io.github.thibaultbee.streampack.internal.muxers.flv.amf.containers.AmfEcmaArray
import java.io.IOException
import java.nio.ByteBuffer

class OnMetadata(context: Context, manageVideoOrientation: Boolean, streams: List<Config>) :
    FlvTag(0, TagType.SCRIPT) {
    private val amfContainer = AmfContainer()

    init {
        amfContainer.add("onMetaData")
        val ecmaArray = AmfEcmaArray()
        ecmaArray.add("duration", 0.0)
        streams.forEach {
            when (it) {
                is AudioConfig -> {
                    ecmaArray.add(
                        "audiocodecid",
                        SoundFormat.fromMimeType(it.mimeType).value.toDouble()
                    )
                    ecmaArray.add("audiodatarate", it.startBitrate.toDouble())
                    ecmaArray.add("audiosamplerate", it.sampleRate.toDouble())
                    ecmaArray.add(
                        "audiosamplesize",
                        AudioConfig.getNumOfBytesPerSample(it.byteFormat).toDouble()
                    )
                    ecmaArray.add(
                        "stereo",
                        AudioConfig.getNumberOfChannels(it.channelConfig) == 2
                    )

                }
                is VideoConfig -> {
                    val resolution = if (manageVideoOrientation) {
                        it.getOrientedResolution(context)
                    } else {
                        it.resolution
                    }
                    ecmaArray.add(
                        "videocodecid",
                        CodecID.fromMimeType(it.mimeType).value.toDouble()
                    )
                    ecmaArray.add("videodatarate", it.startBitrate.toDouble())
                    ecmaArray.add("width", resolution.width.toDouble())
                    ecmaArray.add("height", resolution.height.toDouble())
                    ecmaArray.add("framerate", it.fps.toDouble())
                }
                else -> {
                    throw IOException("Not supported mime type: ${it.mimeType}")
                }
            }
        }
        amfContainer.add(ecmaArray)
    }

    override fun writeTagHeader(buffer: ByteBuffer) {
        // Do nothing
    }

    override val tagHeaderSize: Int
        get() = 0

    override fun writePayload(buffer: ByteBuffer) {
        amfContainer.encode(buffer)
    }

    override val payloadSize: Int
        get() = amfContainer.size
}