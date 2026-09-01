package io.github.thibaultbee.streampack.ui.utils

import androidx.camera.viewfinder.core.ImplementationMode
import io.github.thibaultbee.streampack.core.elements.sources.video.IPreviewableSource

internal object PreviewConfigurationMapper {
    internal fun toViewfinderImplementationMode(mode: IPreviewableSource.ImplementationMode): ImplementationMode {
        return when (mode) {
            IPreviewableSource.ImplementationMode.COMPATIBLE -> ImplementationMode.EXTERNAL
            IPreviewableSource.ImplementationMode.PERFORMANCE -> ImplementationMode.EMBEDDED
        }
    }
}
