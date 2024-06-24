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
package io.github.thibaultbee.streampack.core.internal.utils.av.audio

import android.media.AudioFormat
import java.io.IOException

enum class ChannelConfiguration(val value: Short) {
    SPECIFIC(0),
    CHANNEL_1(1),
    CHANNEL_2(2),
    CHANNEL_3(3),
    CHANNEL_4(4),
    CHANNEL_5(5),
    CHANNEL_6(6),
    CHANNEL_8(7);

    companion object {
        fun fromValue(value: Short) = entries.first { it.value == value }

        fun fromChannelConfig(channelConfig: Int) = when (channelConfig) {
            AudioFormat.CHANNEL_IN_MONO -> CHANNEL_1
            AudioFormat.CHANNEL_IN_STEREO -> CHANNEL_2
            else -> throw IOException("Channel config is not supported: $channelConfig")
        }

        fun fromChannelCount(channelCount: Int) = when (channelCount) {
            1 -> CHANNEL_1
            2 -> CHANNEL_2
            3 -> CHANNEL_3
            4 -> CHANNEL_4
            5 -> CHANNEL_5
            6 -> CHANNEL_6
            8 -> CHANNEL_8
            else -> SPECIFIC
        }
    }
}