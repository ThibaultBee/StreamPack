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
package io.github.thibaultbee.streampack.app.models

import androidx.annotation.StringRes
import io.github.thibaultbee.streampack.app.R

enum class EndpointType(@StringRes val id: Int) {
    TS_FILE(R.string.to_ts_file),
    FLV_FILE(R.string.to_flv_file),
    SRT(R.string.to_srt),
    RTMP(R.string.to_rtmp),
    MP4_FILE(R.string.to_mp4_file),
    WEBM_FILE(R.string.to_webm_file),
    OGG_FILE(R.string.to_ogg_file),
    THREEGP_FILE(R.string.to_3gp_file);

    companion object {
        fun fromId(id: Int): EndpointType = entries.first { it.id == id }
    }
}
