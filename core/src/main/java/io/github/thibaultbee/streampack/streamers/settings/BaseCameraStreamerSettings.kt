package io.github.thibaultbee.streampack.streamers.settings

import io.github.thibaultbee.streampack.internal.encoders.AudioMediaCodecEncoder
import io.github.thibaultbee.streampack.internal.encoders.VideoMediaCodecEncoder
import io.github.thibaultbee.streampack.internal.sources.IAudioCapture
import io.github.thibaultbee.streampack.internal.sources.camera.CameraCapture
import io.github.thibaultbee.streampack.streamers.bases.BaseCameraStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.settings.IBaseCameraStreamerSettings
import io.github.thibaultbee.streampack.utils.CameraSettings


/**
 * Get the base camera settings ie all settings available for [BaseCameraStreamer].
 */
class BaseCameraStreamerSettings(
    audioCapture: IAudioCapture?,
    private val cameraCapture: CameraCapture,
    audioEncoder: AudioMediaCodecEncoder?,
    videoEncoder: VideoMediaCodecEncoder?
) : BaseStreamerSettings(audioCapture, audioEncoder, videoEncoder), IBaseCameraStreamerSettings {
    /**
     * Get the camera settings (focus, zoom,...).
     */
    override val camera: CameraSettings
        get() = cameraCapture.settings
}