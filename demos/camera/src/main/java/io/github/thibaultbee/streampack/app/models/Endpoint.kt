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

sealed class Endpoint(
    val hasTSCapabilities: Boolean,
    val hasFLVCapabilities: Boolean,
    val hasMP4Capabilities: Boolean,
    val hasFileCapabilities: Boolean,
    val hasSrtCapabilities: Boolean,
    val hasRtmpCapabilities: Boolean
) {
    class TsFileEndpoint : Endpoint(true, false, false, true, false, false)
    class FlvFileEndpoint : Endpoint(false, true, false, true, false, false)
    class Mp4FileEndpoint : Endpoint(false, false, true, true, false, false)
    class SrtEndpoint : Endpoint(true, false, false, false, true, false)
    class RtmpEndpoint : Endpoint(false, true, false, false, false, true)

    class WebmFileEndpoint : Endpoint(false, false, false, true, false, false)
    class OggFileEndpoint : Endpoint(false, false, false, true, false, false)
    class ThreeGPFileEndpoint : Endpoint(false, false, false, true, false, false)
}

class EndpointFactory(private val type: EndpointType) {
    fun build(): Endpoint {
        return when (type) {
            EndpointType.TS_FILE -> Endpoint.TsFileEndpoint()
            EndpointType.FLV_FILE -> Endpoint.FlvFileEndpoint()
            EndpointType.SRT -> Endpoint.SrtEndpoint()
            EndpointType.RTMP -> Endpoint.RtmpEndpoint()
            EndpointType.MP4_FILE -> Endpoint.Mp4FileEndpoint()
            EndpointType.WEBM_FILE -> Endpoint.WebmFileEndpoint()
            EndpointType.OGG_FILE -> Endpoint.OggFileEndpoint()
            EndpointType.THREEGP_FILE -> Endpoint.ThreeGPFileEndpoint()
        }
    }
}