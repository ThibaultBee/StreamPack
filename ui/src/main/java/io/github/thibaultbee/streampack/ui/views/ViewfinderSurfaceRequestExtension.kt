package io.github.thibaultbee.streampack.ui.views

import androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest


fun ViewfinderSurfaceRequest.isEquals(otherRequest: ViewfinderSurfaceRequest): Boolean {
    return (this.resolution == otherRequest.resolution) && (this.sourceOrientation == otherRequest.sourceOrientation) && (this.implementationMode == otherRequest.implementationMode) && (this.outputMirrorMode == otherRequest.outputMirrorMode)
}