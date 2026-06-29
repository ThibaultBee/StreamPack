package io.github.thibaultbee.streampack.core.regulator

import io.github.thibaultbee.streampack.core.configuration.BitrateRegulatorConfig
import io.github.thibaultbee.streampack.core.elements.metrics.EndpointMetricsTracker
import io.github.thibaultbee.streampack.core.elements.metrics.writtenBitrateInBps
import kotlin.math.max
import kotlin.math.min

/**
 * A [BitrateRegulator] that reduce video bitrate when packets are lost.
 *
 * @param bitrateRegulatorConfig bitrate regulation configuration
 * @param onVideoTargetBitrateChange call when you have to change video bitrate
 * @param onAudioTargetBitrateChange call when you have to change audio bitrate
 */
class SimpleBitrateRegulator(
    metricsTracker: EndpointMetricsTracker,
    bitrateRegulatorConfig: BitrateRegulatorConfig,
    onVideoTargetBitrateChange: ((Int) -> Unit),
    onAudioTargetBitrateChange: ((Int) -> Unit)
) : BitrateRegulator(
    metricsTracker,
    bitrateRegulatorConfig,
    onVideoTargetBitrateChange,
    onAudioTargetBitrateChange
) {
    companion object {
        private const val MIN_DECREASE_STEP = 100000 // b/s
        private const val MAX_INCREASE_STEP = 200000 // b/s

        private const val MAX_PERCENTAGE_DECREASE = 85 // %
        private const val MIN_PERCENTAGE_DECREASE = 20 // %
    }

    /**
     * Called regularly to get new endpoint metrics
     *
     * @param currentVideoBitrate current video bitrate target in bits/s.
     * @param currentAudioBitrate current audio bitrate target in bits/s.
     */
    override fun update(
        currentVideoBitrate: Int,
        currentAudioBitrate: Int
    ) {
        val metrics = metricsTracker.instant
        val packetsLostOrDropped = metrics.packetsWriteDropped + metrics.packetsWriteLost
        val writtenBitrate = metrics.writtenBitrateInBps

        if (packetsLostOrDropped > 0) {
            // Detected packet dropped or loss - we should reduce the bitrate with multiplicative decrease
            // How critical?
            val percentageReduction = if (metrics.packetsWritten == 0L) {
                MAX_PERCENTAGE_DECREASE
            } else {
                (packetsLostOrDropped * 100 / metrics.packetsWritten).toInt().coerceIn(MIN_PERCENTAGE_DECREASE, MAX_PERCENTAGE_DECREASE)
            }

            // Reduce current bitrate by percentageReduction %
            val newVideoBitrate = currentVideoBitrate - max(
                currentVideoBitrate * percentageReduction / 100,
                MIN_DECREASE_STEP // getting down by 100000 b/s minimum
            )
            onVideoTargetBitrateChange(newVideoBitrate)
        } else if (currentVideoBitrate < bitrateRegulatorConfig.videoBitrateRange.upper) {
            // Only increase if we are successfully sending at near the current target bitrate
            // This prevents us from increasing the target when the network is already saturated
            if (writtenBitrate > currentVideoBitrate * 0.9) {
                // Additive increase
                val newVideoBitrate = min(
                    currentVideoBitrate + MAX_INCREASE_STEP,
                    bitrateRegulatorConfig.videoBitrateRange.upper
                )
                onVideoTargetBitrateChange(newVideoBitrate)
            }
        }
    }

    /**
     * Factory interface you must use to create a [SimpleBitrateRegulator] object.
     * If you want to create a custom RTMP bitrate regulation implementation, create a factory that
     * implements this interface.
     */
    class Factory : IBitrateRegulator.Factory {
        /**
         * Creates a [SimpleBitrateRegulator] object from given parameters
         *
         * @param metricsTracker endpoint metrics tracker
         * @param bitrateRegulatorConfig bitrate regulation configuration
         * @param onVideoTargetBitrateChange call when you have to change video bitrate
         * @param onAudioTargetBitrateChange call when you have to change audio bitrate
         * @return a [SimpleBitrateRegulator] object
         */
        override fun newBitrateRegulator(
            metricsTracker: EndpointMetricsTracker,
            bitrateRegulatorConfig: BitrateRegulatorConfig,
            onVideoTargetBitrateChange: ((Int) -> Unit),
            onAudioTargetBitrateChange: ((Int) -> Unit)
        ): SimpleBitrateRegulator {
            return SimpleBitrateRegulator(
                metricsTracker,
                bitrateRegulatorConfig,
                onVideoTargetBitrateChange,
                onAudioTargetBitrateChange
            )
        }
    }
}