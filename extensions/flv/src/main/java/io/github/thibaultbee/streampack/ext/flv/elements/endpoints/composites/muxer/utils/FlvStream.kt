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
package io.github.thibaultbee.streampack.ext.flv.elements.endpoints.composites.muxer.utils

import io.github.komedia.komuxer.flv.config.FLVAudioConfig
import io.github.komedia.komuxer.flv.config.FLVConfig
import io.github.komedia.komuxer.flv.config.FLVVideoConfig
import io.github.komedia.komuxer.flv.tags.FLVData
import io.github.thibaultbee.streampack.core.elements.data.Frame
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig

/**
 * Internal FLV stream handler.
 */
internal sealed class FlvStream {
    protected var sentSequenceStart = false

    abstract val flvConfig: FLVConfig<*>

    fun create(frame: Frame): List<FLVData> {
        return createImpl(frame)
    }

    abstract fun createImpl(frame: Frame): List<FLVData>
}

internal class AudioFlvStream(codecConfig: AudioCodecConfig) : FlvStream() {
    private val frameFactory = FlvAudioDataFactory.createFactory(codecConfig)

    override val flvConfig: FLVAudioConfig = codecConfig.toFLVConfig()

    override fun createImpl(frame: Frame): List<FLVData> {
        var withSequenceStart = false
        if (!sentSequenceStart) {
            // Send config
            withSequenceStart = true
            sentSequenceStart = true
        }
        return frameFactory.create(frame, withSequenceStart)
    }
}


internal class VideoFlvStream(codecConfig: VideoCodecConfig) : FlvStream() {
    override val flvConfig: FLVVideoConfig = codecConfig.toFLVConfig()

    private val frameFactory = FlvVideoDataFactory.createFactory(codecConfig)

    override fun createImpl(frame: Frame): List<FLVData> {
        var withSequenceStart = false
        if ((!sentSequenceStart) && (frame.isKeyFrame)) {
            // Send config
            withSequenceStart = true
            sentSequenceStart = true
        }
        return frameFactory.create(frame, withSequenceStart)
    }
}
