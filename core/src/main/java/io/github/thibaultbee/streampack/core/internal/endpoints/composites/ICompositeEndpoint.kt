package io.github.thibaultbee.streampack.core.internal.endpoints.composites

import io.github.thibaultbee.streampack.core.internal.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.internal.endpoints.IEndpoint
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.IMuxer
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.sinks.ISink

interface ICompositeEndpoint : IEndpointInternal, IPublicCompositeEndpoint

interface IPublicCompositeEndpoint : IEndpoint {
    val muxer: IMuxer
    val sink: ISink
}