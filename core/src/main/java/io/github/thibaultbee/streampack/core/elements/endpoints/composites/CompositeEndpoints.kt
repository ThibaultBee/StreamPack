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
package io.github.thibaultbee.streampack.core.elements.endpoints.composites

import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.IMuxerInternal
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.ts.TsMuxer
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.ts.data.TSServiceInfo
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.sinks.ISinkInternal

/**
 * An [IEndpointInternal] implementation that combines a [IMuxerInternal] and a [ISinkInternal].
 */
object CompositeEndpoints {
    /**
     * Creates an endpoint for SRT (with a TS muxer)
     */
    internal fun createSrtEndpoint(serviceInfo: TSServiceInfo?): IEndpointInternal {
        val sink = createSrtSink()
        val muxer = TsMuxer()
        if (serviceInfo != null) {
            muxer.addService(serviceInfo)
        }
        return CompositeEndpoint(muxer, sink)
    }

    private fun createSrtSink(): ISinkInternal {
        return try {
            val clazz =
                Class.forName("io.github.thibaultbee.streampack.ext.srt.elements.endpoints.composites.sinks.SrtSink")
            clazz.getConstructor().newInstance() as ISinkInternal
        } catch (e: ClassNotFoundException) {
            // Expected if the app was built without the SRT extension.
            throw ClassNotFoundException(
                "Attempting to stream SRT stream without depending on the SRT extension",
                e
            )
        } catch (t: Throwable) {
            // The SRT extension is present, but instantiation failed.
            throw RuntimeException("Error instantiating SRT extension", t)
        }
    }
}
