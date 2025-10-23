package io.github.thibaultbee.streampack.ext.srt.elements.endpoints

import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.createDefaultTsServiceInfo
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.CompositeEndpointFactory
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.ts.TsMuxer
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.ts.data.TSServiceInfo
import io.github.thibaultbee.streampack.ext.srt.elements.endpoints.composites.sinks.SrtSink
import kotlinx.coroutines.CoroutineDispatcher

/**
 * The SRT endpoint factory.
 *
 * It returns a [CompositeEndpointFactory] with a [TsMuxer] and a [SrtSink]
 *
 * @param serviceInfo The service info to use in the TS muxer. Default to a basic service info.
 * @param coroutineDispatcher The coroutine dispatcher to use in the SRT sink.
 */
fun SrtEndpointFactory(
    serviceInfo: TSServiceInfo = createDefaultTsServiceInfo(),
    coroutineDispatcher: CoroutineDispatcher
) =
    CompositeEndpointFactory(
        TsMuxer().apply { addService(serviceInfo) },
        SrtSink(coroutineDispatcher)
    )