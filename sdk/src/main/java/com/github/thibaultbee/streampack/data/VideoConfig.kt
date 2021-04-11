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

import android.media.MediaCodecInfo.CodecProfileLevel
import android.util.Size
import com.github.thibaultbee.streampack.utils.isVideo

data class VideoConfig(
    val mimeType: String,
    val startBitrate: Int,
    val resolution: Size,
    val fps: Int,
    val profile: Int = CodecProfileLevel.AVCProfileHigh,
    val level: Int = CodecProfileLevel.AVCLevel52,
) {
    init {
        require(mimeType.isVideo())
    }
}

