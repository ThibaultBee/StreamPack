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

import android.content.Context
import io.github.thibaultbee.streampack.app.models.EndpointType
import io.github.thibaultbee.streampack.core.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.internal.endpoints.DynamicEndpoint
import io.github.thibaultbee.streampack.core.internal.endpoints.MediaContainerType
import io.github.thibaultbee.streampack.core.internal.endpoints.MediaSinkType
import io.github.thibaultbee.streampack.core.streamers.infos.CameraStreamerConfigurationInfo

class StreamerInfoFactory(
    context: Context,
    private val endpointType: EndpointType,
) {
    private val endpoint = DynamicEndpoint(context)

    fun build(): CameraStreamerConfigurationInfo {
        return when (endpointType) {
            EndpointType.TS_FILE -> getInfo(
                MediaDescriptor.Type(MediaContainerType.TS, MediaSinkType.FILE)
            )

            EndpointType.FLV_FILE -> getInfo(
                MediaDescriptor.Type(
                    MediaContainerType.FLV,
                    MediaSinkType.FILE
                )
            )

            EndpointType.SRT -> getInfo(
                MediaDescriptor.Type(
                    MediaContainerType.TS,
                    MediaSinkType.SRT
                )
            )

            EndpointType.RTMP -> getInfo(
                MediaDescriptor.Type(
                    MediaContainerType.FLV,
                    MediaSinkType.RTMP
                )
            )

            EndpointType.MP4_FILE -> getInfo(
                MediaDescriptor.Type(
                    MediaContainerType.MP4,
                    MediaSinkType.FILE
                )
            )

            EndpointType.WEBM_FILE -> getInfo(
                MediaDescriptor.Type(
                    MediaContainerType.WEBM,
                    MediaSinkType.FILE
                )
            )

            EndpointType.OGG_FILE -> getInfo(
                MediaDescriptor.Type(
                    MediaContainerType.OGG,
                    MediaSinkType.FILE
                )
            )

            EndpointType.THREEGP_FILE -> getInfo(
                MediaDescriptor.Type(
                    MediaContainerType.THREEGP,
                    MediaSinkType.FILE
                )
            )
        }
    }

    /**
     * This is valid only if the [IEndpoint] of the streamer is the [DynamicEndpoint] (default).
     */
    private fun getInfo(type: MediaDescriptor.Type) =
        CameraStreamerConfigurationInfo(endpoint.getInfo(type))
}