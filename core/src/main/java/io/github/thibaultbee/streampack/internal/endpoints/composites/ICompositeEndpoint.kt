package io.github.thibaultbee.streampack.internal.endpoints.composites

import io.github.thibaultbee.streampack.internal.endpoints.IEndpoint
import io.github.thibaultbee.streampack.internal.endpoints.IPublicEndpoint
import io.github.thibaultbee.streampack.internal.endpoints.composites.muxers.IPublicMuxer
import io.github.thibaultbee.streampack.internal.endpoints.composites.sinks.IPublicSink

interface ICompositeEndpoint : IEndpoint, IPublicCompositeEndpoint

interface IPublicCompositeEndpoint : IPublicEndpoint {
    val muxer: IPublicMuxer
    val sink: IPublicSink
}