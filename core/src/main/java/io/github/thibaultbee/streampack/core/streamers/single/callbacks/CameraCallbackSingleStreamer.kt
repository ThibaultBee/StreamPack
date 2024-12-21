package io.github.thibaultbee.streampack.core.streamers.single.callbacks

import android.Manifest
import android.content.Context
import android.view.Surface
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.internal.endpoints.DynamicEndpoint
import io.github.thibaultbee.streampack.core.internal.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.internal.sources.video.camera.CameraSource
import io.github.thibaultbee.streampack.core.internal.sources.video.camera.ICameraSource
import io.github.thibaultbee.streampack.core.streamers.infos.CameraStreamerConfigurationInfo
import io.github.thibaultbee.streampack.core.streamers.infos.IConfigurationInfo
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICameraCallbackStreamer
import io.github.thibaultbee.streampack.core.streamers.single.CameraSingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.ICallbackSingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.ICoroutineSingleStreamer
import kotlinx.coroutines.launch

/**
 * Default implementation of [ICallbackSingleStreamer] that uses [ICoroutineSingleStreamer] to handle streamer logic.
 * It is a bridge between [ICoroutineSingleStreamer] and [ICallbackSingleStreamer].
 *
 * @param context application context
 * @param enableMicrophone [Boolean.true] to capture audio
 * @param internalEndpoint the [IEndpointInternal] implementation
 */
class CameraCallbackSingleStreamer(
    private val context: Context,
    enableMicrophone: Boolean = true,
    internalEndpoint: IEndpointInternal = DynamicEndpoint(context)
) : CallbackSingleStreamer(CameraSingleStreamer(context, enableMicrophone, internalEndpoint)),
    ICameraCallbackStreamer {
    private val cameraSource = (streamer as CameraSingleStreamer).videoSource as CameraSource

    /**
     * Gets the camera source.
     * It allows to configure camera settings and to set the camera id.
     */
    override val videoSource = cameraSource as ICameraSource

    /**
     * Get/Set current camera id.
     * It is a shortcut for [CameraSource.cameraId]
     */
    override var cameraId: String
        /**
         * Get current camera id.
         *
         * @return a string that described current camera
         */
        get() = videoSource.cameraId
        /**
         * Set current camera id.
         *
         * @param value string that described the camera. Retrieves list of camera from [Context.cameras]
         */
        @RequiresPermission(Manifest.permission.CAMERA)
        set(value) {
            videoSource.cameraId = value
        }

    /**
     * Gets configuration information.
     *
     * When using the [DynamicEndpoint], the endpoint type is unknown until [open] is called.
     * In this case, prefer using [getInfo] with the [MediaDescriptor] used in [open].
     */
    override val info: IConfigurationInfo
        get() = CameraStreamerConfigurationInfo(endpoint.info)


    /**
     * Gets configuration information from [MediaDescriptor].
     *
     * @param descriptor the media descriptor
     */
    override fun getInfo(descriptor: MediaDescriptor): IConfigurationInfo {
        val endpointInfo = if (endpoint is DynamicEndpoint) {
            (endpoint as DynamicEndpoint).getInfo(descriptor)
        } else {
            endpoint.info
        }
        return CameraStreamerConfigurationInfo(endpointInfo)
    }

    /**
     * Sets a preview surface.
     */
    override fun setPreview(surface: Surface) {
        cameraSource.previewSurface = surface
    }

    /**
     * Starts video preview.
     *
     * The preview will be rendered on the surface set by [setPreview].
     * It is recommend to call configure before call [startPreview] to avoid camera restart when
     * encoder surface will be added.
     *
     * @see [stopPreview]
     * @see [setPreview]
     */
    override fun startPreview() {
        /**
         * Trying to set encoder surface to avoid a camera restart.
         */
        coroutineScope.launch {
            try {
                cameraSource.startPreview()
            } catch (t: Throwable) {
                listeners.forEach { it.onError(t) }
            }
        }
    }

    /**
     * Stops capture.
     * It also stops stream if the stream is running.
     *
     * @see [startPreview]
     */
    override fun stopPreview() {
        cameraSource.stopPreview()
    }

    /**
     * Same as [CallbackSingleStreamer.release] but it also calls [stopPreview].
     */
    override fun release() {
        stopPreview()
        super.release()
    }
}