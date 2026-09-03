package io.github.thibaultbee.streampack.core.elements.endpoints.composites

import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpoint
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.IMuxer
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.sinks.ISink
import io.github.thibaultbee.streampack.core.utils.InternalStreamPackApi

@InternalStreamPackApi
interface ICompositeEndpointInternal : IEndpointInternal, ICompositeEndpoint

interface ICompositeEndpoint : IEndpoint {
    val muxer: IMuxer
    val sink: ISink
}