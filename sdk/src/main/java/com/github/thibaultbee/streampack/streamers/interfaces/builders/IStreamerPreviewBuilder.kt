package com.github.thibaultbee.streampack.streamers.interfaces.builders

import android.view.Surface
import com.github.thibaultbee.streampack.streamers.BaseCameraStreamer
import com.github.thibaultbee.streampack.streamers.BaseStreamer

interface IStreamerPreviewBuilder : IStreamerBuilder {
    /**
     * Set preview surface.
     * If provided, it starts preview.
     *
     * @param previewSurface surface where to display preview
     */
    fun setPreviewSurface(previewSurface: Surface): IStreamerPreviewBuilder

    /**
     * Combines all of the characteristics that have been set and return a new [BaseStreamer] object.
     *
     * @return a new [BaseCameraStreamer] object
     */
    override fun build(): BaseCameraStreamer
}