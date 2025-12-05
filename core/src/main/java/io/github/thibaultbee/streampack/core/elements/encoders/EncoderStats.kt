package io.github.thibaultbee.streampack.core.elements.encoders

/**
 * Statistics for encoder performance monitoring.
 */
data class EncoderStats(
    /**
     * Total number of frames output from the encoder
     */
    val outputFrameCount: Long = 0,
    
    /**
     * Current output frames per second (encoded FPS)
     */
    val outputFps: Float = 0f
)
