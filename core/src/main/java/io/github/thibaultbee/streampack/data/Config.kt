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
package io.github.thibaultbee.streampack.data

import android.media.MediaFormat
import io.github.thibaultbee.streampack.streamers.bases.BaseStreamer

/**
 * Base configuration class.
 * If you don't know how to set class members, [Video encoding recommendations](https://developer.android.com/guide/topics/media/media-formats#video-encoding) should give you hints.
 *
 * @see [BaseStreamer.configure]
 */
open class Config(
    /**
     * Audio encoder mime type.
     * Only [MediaFormat.MIMETYPE_AUDIO_AAC] is supported yet.
     *
     * **See Also:** [MediaFormat MIMETYPE_AUDIO_*](https://developer.android.com/reference/android/media/MediaFormat)
     */
    val mimeType: String,

    /**
     * Audio encoder bitrate in bits/s.
     */
    val startBitrate: Int
)
