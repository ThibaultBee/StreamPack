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
package io.github.thibaultbee.streampack.utils

import android.media.MediaFormat
import android.util.Size
import io.github.thibaultbee.streampack.data.AudioConfig
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.internal.endpoints.muxers.ts.data.TsServiceInfo
import kotlin.random.Random

object AndroidUtils {
    /**
     * Generates a TS service information for test
     *
     * @return a [TsServiceInfo] for test
     */
    fun fakeServiceInfo() = TsServiceInfo(
        TsServiceInfo.ServiceType.DIGITAL_TV,
        Random.nextInt().toShort(),
        "testName",
        "testServiceName"
    )

    /**
     * Generates a valid audio configuration for test
     *
     * @return a [AudioConfig] for test
     */
    fun fakeValidAudioConfig() = AudioConfig()

    /**
     * Generates an invalid audio configuration for test
     *
     * @return a [AudioConfig] for test
     */
    fun fakeInvalidAudioConfig() = AudioConfig(
        mimeType = MediaFormat.MIMETYPE_VIDEO_AVC // Video instead of audio
    )

    /**
     * Generates a valid video configuration for test
     *
     * @return a [VideoConfig] for test
     */
    fun fakeValidVideoConfig() = VideoConfig(
        resolution = Size(640, 360)
    )


    /**
     * Generates an invalid video configuration for test
     *
     * @return a [VideoConfig] for test
     */
    fun fakeInvalidVideoConfig() = VideoConfig(
        mimeType = MediaFormat.MIMETYPE_AUDIO_AAC // Audio instead of video
    )
}