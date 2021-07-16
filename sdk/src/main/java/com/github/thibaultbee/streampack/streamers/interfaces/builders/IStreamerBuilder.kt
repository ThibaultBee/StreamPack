package com.github.thibaultbee.streampack.streamers.interfaces.builders

import android.content.Context
import android.view.Surface
import com.github.thibaultbee.streampack.data.AudioConfig
import com.github.thibaultbee.streampack.data.VideoConfig
import com.github.thibaultbee.streampack.internal.muxers.ts.data.ServiceInfo
import com.github.thibaultbee.streampack.streamers.BaseCameraStreamer
import com.github.thibaultbee.streampack.logger.ILogger

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
     * Set preview surface.
     * If provided, it starts preview.
     *
     * @param previewSurface surface where to display preview
     */
    fun setPreviewSurface(previewSurface: Surface): IStreamerBuilder

    /**
     * Combines all of the characteristics that have been set and return a new [BaseCameraStreamer] object.
     *
     * @return a new [BaseCameraStreamer] object
     */
    fun build(): BaseCameraStreamer
}