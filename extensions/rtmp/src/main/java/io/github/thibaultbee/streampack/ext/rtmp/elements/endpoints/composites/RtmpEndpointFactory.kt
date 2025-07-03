/*
 * Copyright (C) 2024 Thibault B.
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
package io.github.thibaultbee.streampack.ext.rtmp.elements.endpoints.composites

import io.github.thibaultbee.streampack.core.elements.endpoints.composites.CompositeEndpointFactory
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.flv.FlvMuxer
import io.github.thibaultbee.streampack.ext.rtmp.elements.endpoints.composites.sinks.RtmpSink

/**
 * The RTMP endpoint factory.
 *
 * It returns a [CompositeEndpointFactory] with a [FlvMuxer] and a [RtmpSink]
 */
fun RtmpEndpointFactory() =
    CompositeEndpointFactory(
        FlvMuxer(
            isForFile = false
        ), RtmpSink()
    )
