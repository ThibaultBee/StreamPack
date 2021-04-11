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
package com.github.thibaultbee.streampack.data

import android.media.AudioFormat
import com.github.thibaultbee.streampack.utils.isAudio
import java.security.InvalidParameterException

data class AudioConfig(
    val mimeType: String,
    val startBitrate: Int,
    val sampleRate: Int,
    val channelConfig: Int,
    val audioByteFormat: Int
) {
    init {
        require(mimeType.isAudio())
    }

    companion object {
        fun getChannelNumber(channelConfig: Int) = when (channelConfig) {
            AudioFormat.CHANNEL_IN_MONO -> 1
            AudioFormat.CHANNEL_IN_STEREO -> 2
            else -> throw InvalidParameterException("Unknown audio format: $channelConfig")
        }
    }
}
