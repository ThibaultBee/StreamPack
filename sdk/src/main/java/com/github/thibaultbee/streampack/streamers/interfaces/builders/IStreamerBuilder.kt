package com.github.thibaultbee.streampack.streamers.interfaces.builders

import android.content.Context
import com.github.thibaultbee.streampack.data.AudioConfig
import com.github.thibaultbee.streampack.data.VideoConfig
import com.github.thibaultbee.streampack.internal.muxers.ts.data.ServiceInfo
import com.github.thibaultbee.streampack.logger.ILogger
import com.github.thibaultbee.streampack.streamers.BaseStreamer

interface IStreamerBuilder {

    /**
     * Set application context. It is mandatory to set context.
     *
     * @param context application context.
     */
    fun setContext(context: Context): IStreamerBuilder

    /**
     * Set TS service info. It is mandatory to set TS service info.
     *
     * @param serviceInfo TS service info.
     */
    fun setServiceInfo(serviceInfo: ServiceInfo): IStreamerBuilder

    /**
     * Set logger.
     *
     * @param logger [ILogger] implementation
     */
    fun setLogger(logger: ILogger): IStreamerBuilder

    /**
     * Set both audio and video configuration. Can be change with [configure].
     *
     * @param audioConfig audio configuration
     * @param videoConfig video configuration
     */
    fun setConfiguration(audioConfig: AudioConfig, videoConfig: VideoConfig): IStreamerBuilder

    /**
     * Combines all of the characteristics that have been set and return a new [BaseStreamer] object.
     *
     * @return a new [BaseStreamer] object
     */
    fun build(): BaseStreamer
}