package io.github.thibaultbee.streampack.core.elements.utils.pool

import io.github.thibaultbee.streampack.core.elements.data.RawFrame

interface IReadOnlyRawFrameFactory {
    /**
     * Creates a [RawFrame].
     *
     * The returned frame must be released by calling [RawFrame.close] when it is not used anymore.
     *
     * @param bufferSize the buffer size
     * @param timestampInUs the frame timestamp in µs
     * @return a frame
     */
    fun create(bufferSize: Int, timestampInUs: Long): RawFrame
}

/**
 * A pool of frames.
 */
interface IRawFrameFactory : IReadOnlyRawFrameFactory {
    /**
     * Clears the factory.
     */
    fun clear()

    /**
     * Closes the factory.
     */
    fun close()
}