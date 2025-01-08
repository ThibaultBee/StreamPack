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
package io.github.thibaultbee.streampack.ext.srt.internal.endpoints.composites

import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.createDefaultTsServiceInfo
import io.github.thibaultbee.streampack.ext.srt.internal.endpoints.composites.sinks.SrtSink
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.CompositeEndpoint
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.ts.TSMuxer
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.ts.data.TSServiceInfo

/**
 * Creates a [SrtEndpoint] with a default [TSServiceInfo].
 */
fun SrtEndpoint(serviceInfo: TSServiceInfo = createDefaultTsServiceInfo()) = SrtEndpoint().apply {
    addService(serviceInfo)
}

/**
 * A SRT endpoint.
 * It encapsulates a [TSMuxer] and a [SrtSink].
 */
class SrtEndpoint internal constructor() :
    CompositeEndpoint(TSMuxer(), SrtSink()) {
    fun addService(serviceInfo: TSServiceInfo = createDefaultTsServiceInfo()) {
        (muxer as TSMuxer).addService(serviceInfo)
    }

    fun removeService(serviceInfo: TSServiceInfo) {
        (muxer as TSMuxer).removeService(serviceInfo)
    }
}