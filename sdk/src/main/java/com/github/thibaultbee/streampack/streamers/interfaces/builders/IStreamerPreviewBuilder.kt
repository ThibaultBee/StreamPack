package com.github.thibaultbee.streampack.streamers.interfaces.builders

import android.view.Surface

interface IStreamerPreviewBuilder {
    /**
     * Set preview surface.
     * If provided, it starts preview.
     *
     * @param previewSurface surface where to display preview
     */
    fun setPreviewSurface(previewSurface: Surface): IStreamerPreviewBuilder
}