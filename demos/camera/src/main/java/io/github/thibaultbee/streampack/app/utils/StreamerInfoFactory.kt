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
package io.github.thibaultbee.streampack.app.utils

import io.github.thibaultbee.streampack.app.models.EndpointType
import io.github.thibaultbee.streampack.streamers.helpers.CameraStreamerConfigurationInfo

class StreamerInfoFactory(
    private val endpointType: EndpointType,
) {
    fun build(): CameraStreamerConfigurationInfo {
        return when (endpointType) {
            EndpointType.TS_FILE -> CameraStreamerConfigurationInfo.tsInfo
            EndpointType.FLV_FILE -> CameraStreamerConfigurationInfo.flvInfo
            EndpointType.SRT -> CameraStreamerConfigurationInfo.tsInfo
            EndpointType.RTMP -> CameraStreamerConfigurationInfo.flvInfo
            EndpointType.MP4_FILE -> CameraStreamerConfigurationInfo.mp4Info
        }
    }
}