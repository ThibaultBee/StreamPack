package com.github.thibaultbee.streampack.streamers.interfaces.builders

import com.github.thibaultbee.streampack.data.BitrateRegulatorConfig
import com.github.thibaultbee.streampack.regulator.DefaultSrtBitrateRegulatorFactory
import com.github.thibaultbee.streampack.regulator.ISrtBitrateRegulatorFactory

interface IAdaptiveLiveStreamerBuilder : ILiveStreamerBuilder {
    /**
     * Set SRT bitrate regulator class and configuration.
     *
     * @param bitrateRegulatorFactory bitrate regulator factory. If you don't want to implement your own bitrate regulator, use [DefaultSrtBitrateRegulatorFactory]
     * @param bitrateRegulatorConfig bitrate regulator configuration.
     */
    fun setBitrateRegulator(
        bitrateRegulatorFactory: ISrtBitrateRegulatorFactory?,
        bitrateRegulatorConfig: BitrateRegulatorConfig?
    ): IStreamerBuilder
}