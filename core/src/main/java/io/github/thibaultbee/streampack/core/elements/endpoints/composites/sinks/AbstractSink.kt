package io.github.thibaultbee.streampack.core.elements.endpoints.composites.sinks

import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.endpoints.MediaSinkType
import io.github.thibaultbee.streampack.core.logger.Logger

/**
 * Abstract class to write data to a sink
 * It provides a default implementation for [open] method.
 *
 * Use [openImpl] to implement the opening of the sink.
 */
abstract class AbstractSink : ISinkInternal {
    abstract val supportedSinkTypes: List<MediaSinkType>

    override val metrics: Any
        get() = TODO("Not yet implemented")

    override suspend fun open(mediaDescriptor: MediaDescriptor) {
        if (isOpenFlow.value) {
            Logger.w(TAG, "Sink is already opened")
            return
        }
        require(supportedSinkTypes.contains(mediaDescriptor.type.sinkType)) {
            "MediaDescriptor type not supported ${mediaDescriptor.type.sinkType}. It must be ${
                supportedSinkTypes.joinToString(
                    ", "
                )
            } "
        }
        openImpl(mediaDescriptor)
    }

    abstract suspend fun openImpl(mediaDescriptor: MediaDescriptor)

    companion object {
        const val TAG = "AbstractSink"
    }
}