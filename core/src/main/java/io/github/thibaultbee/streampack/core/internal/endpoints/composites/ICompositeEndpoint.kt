package io.github.thibaultbee.streampack.core.internal.endpoints.composites

import io.github.thibaultbee.streampack.core.internal.endpoints.IEndpoint
import io.github.thibaultbee.streampack.core.internal.endpoints.IPublicEndpoint
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.IPublicMuxer
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.sinks.IPublicSink

interface ICompositeEndpoint : IEndpoint, IPublicCompositeEndpoint

interface IPublicCompositeEndpoint : IPublicEndpoint {
    val muxer: IPublicMuxer
    val sink: IPublicSink
}