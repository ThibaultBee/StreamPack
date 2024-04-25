/*
 * Copyright (C) 2021 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.internal.sources.video.camera

import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.MeteringRectangle
import android.os.Build
import android.util.Range
import android.util.Rational
import androidx.annotation.RequiresApi
import io.github.thibaultbee.streampack.internal.utils.*
import io.github.thibaultbee.streampack.internal.utils.extensions.clamp
import io.github.thibaultbee.streampack.internal.utils.extensions.isDevicePortrait
import io.github.thibaultbee.streampack.internal.utils.extensions.isNormalized
import io.github.thibaultbee.streampack.internal.utils.extensions.normalize
import io.github.thibaultbee.streampack.internal.utils.extensions.rotate
import io.github.thibaultbee.streampack.logger.Logger
import io.github.thibaultbee.streampack.streamers.bases.BaseCameraStreamer
import io.github.thibaultbee.streampack.utils.getAutoExposureModes
import io.github.thibaultbee.streampack.utils.getAutoFocusModes
import io.github.thibaultbee.streampack.utils.getAutoWhiteBalanceModes
import io.github.thibaultbee.streampack.utils.getCameraCharacteristics
import io.github.thibaultbee.streampack.utils.getExposureMaxMeteringRegionsSupported
import io.github.thibaultbee.streampack.utils.getExposureRange
import io.github.thibaultbee.streampack.utils.getExposureStep
import io.github.thibaultbee.streampack.utils.getFocusMaxMeteringRegionsSupported
import io.github.thibaultbee.streampack.utils.getLensDistanceRange
import io.github.thibaultbee.streampack.utils.getScalerMaxZoom
import io.github.thibaultbee.streampack.utils.getSensitivityRange
import io.github.thibaultbee.streampack.utils.getWhiteBalanceMeteringRegionsSupported
import io.github.thibaultbee.streampack.utils.getZoomRatioRange
import io.github.thibaultbee.streampack.utils.isFlashAvailable
import io.github.thibaultbee.streampack.utils.isOpticalStabilizationAvailable
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


/**
 * Use to change camera settings.
 * This object is returned by [BaseCameraStreamer.videoSource.settings].
 */
class CameraSettings(context: Context, cameraController: CameraController) {
    /**
     * Current camera flash API.
     */
    val flash = Flash(context, cameraController)

    /**
     * Current camera white balance API.
     */
    val whiteBalance = WhiteBalance(context, cameraController)

    /**
     * Current camera ISO API.
     */
    val iso = Iso(context, cameraController)

    /**
     * Current camera exposure API.
     */
    val exposure = Exposure(context, cameraController)

    /**
     * Current camera zoom API.
     */
    val zoom = Zoom.build(context, cameraController)

    /**
     * Current focus API.
     */
    val focus = Focus(context, cameraController)

    /**
     * Current stabilization API.
     */
    val stabilization = Stabilization(context, cameraController)

    /**
     * Current focus metering API.
     */
    val focusMetering =
        FocusMetering(context, cameraController, zoom, focus, exposure, whiteBalance)
}

class Flash(private val context: Context, private val cameraController: CameraController) {
    /**
     * Checks if the current camera has a flash device.
     *
     * @return [Boolean.true] if camera has a flash device, [Boolean.false] otherwise.
     */
    val available: Boolean
        get() = cameraController.cameraId?.let { context.isFlashAvailable(it) } ?: false

    /**
     * Enables or disables flash.
     *
     * @see [available]
     */
    var enable: Boolean
        /**
         * @return [Boolean.true] if flash is already on, otherwise [Boolean.false]
         */
        get() = getFlash() == CaptureResult.FLASH_MODE_TORCH
        /**
         * @param value [Boolean.true] to switch on flash, [Boolean.false] to switch off flash
         */
        set(value) {
            if (value) {
                setFlash(CaptureResult.FLASH_MODE_TORCH)
            } else {
                setFlash(CaptureResult.FLASH_MODE_OFF)
            }
        }

    private fun getFlash(): Int =
        cameraController.getSetting(CaptureRequest.FLASH_MODE) ?: CaptureResult.FLASH_MODE_OFF

    private fun setFlash(mode: Int) {
        cameraController.setRepeatingSetting(CaptureRequest.FLASH_MODE, mode)
    }
}

class WhiteBalance(private val context: Context, private val cameraController: CameraController) {
    /**
     * Gets supported auto white balance modes for the current camera
     *
     * @return list of supported white balance modes.
     */
    val availableAutoModes: List<Int>
        get() = cameraController.cameraId?.let { context.getAutoWhiteBalanceModes(it) }
            ?: emptyList()

    /**
     * Set or get auto white balance mode.
     *
     * **See Also:** [CONTROL_AWB_MODE](https://developer.android.com/reference/android/hardware/camera2/CaptureRequest#CONTROL_AWB_MODE)
     * @see [availableAutoModes]
     */
    var autoMode: Int
        /**
         * Get auto white balance mode.
         *
         * @return current camera audo white balance mode
         */
        get() = cameraController.getSetting(CaptureRequest.CONTROL_AWB_MODE)
            ?: CaptureResult.CONTROL_AWB_MODE_OFF
        /**
         * Get auto white balance mode.
         *
         * @param value auto white balance mode
         */
        set(value) {
            cameraController.setRepeatingSetting(CaptureRequest.CONTROL_AWB_MODE, value)
        }

    /**
     * Get maximum number of available white balance metering regions.
     */
    val maxNumOfMeteringRegions: Int
        get() = cameraController.cameraId?.let { context.getWhiteBalanceMeteringRegionsSupported(it) }
            ?: 0

    /**
     * Set/get white balance metering regions.
     */
    var meteringRegions: List<MeteringRectangle>
        get() = cameraController.getSetting(CaptureRequest.CONTROL_AWB_REGIONS)?.toList()
            ?: emptyList()
        set(value) {
            cameraController.setRepeatingSetting(
                CaptureRequest.CONTROL_AWB_REGIONS,
                value.toTypedArray()
            )
        }
}


class Iso(private val context: Context, private val cameraController: CameraController) {
    /**
     * Get current camera supported sensitivity range.
     *
     * @return supported Sensitivity range
     *
     * @see [sensorSensitivity]
     */
    val availableSensorSensitivityRange: Range<Int>
        get() = cameraController.cameraId?.let { context.getSensitivityRange(it) }
            ?: DEFAULT_SENSITIVITY_RANGE

    /**
     * Set or get lens focus distance.
     *
     * @see [availableSensorSensitivityRange]
     */
    var sensorSensitivity: Int
        /**
         * Get the sensitivity
         *
         * @return the sensitivity
         */
        get() = cameraController.getSetting(CaptureRequest.SENSOR_SENSITIVITY)
            ?: DEFAULT_SENSITIVITY
        /**
         * Set the sensitivity
         *
         * Only set lens focus distance if [Exposure.autoMode] == [CaptureResult.CONTROL_AE_MODE_OFF].
         *
         * @param value lens focus distance
         */
        set(value) {
            cameraController.setRepeatingSetting(
                CaptureRequest.SENSOR_SENSITIVITY,
                value.clamp(availableSensorSensitivityRange)
            )
        }

    companion object {
        const val DEFAULT_SENSITIVITY = 100
        val DEFAULT_SENSITIVITY_RANGE = Range(DEFAULT_SENSITIVITY, DEFAULT_SENSITIVITY)
    }
}

class Exposure(private val context: Context, private val cameraController: CameraController) {
    /**
     * Get current camera supported auto exposure mode.
     *
     * @return list of supported auto exposure mode
     *
     * @see [autoMode]
     */
    val availableAutoModes: List<Int>
        get() = cameraController.cameraId?.let { context.getAutoExposureModes(it) } ?: emptyList()

    /**
     * Set or get auto exposure mode.
     *
     * @see [availableAutoModes]
     */
    var autoMode: Int
        /**
         * Get the auto exposure mode.
         *
         * @return auto exposure mode
         */
        get() = cameraController.getSetting(CaptureRequest.CONTROL_AE_MODE)
            ?: CaptureResult.CONTROL_AE_MODE_OFF
        /**
         * Set the auto exposure mode.
         *
         * @param value auto exposure mode
         */
        set(value) {
            cameraController.setRepeatingSetting(CaptureRequest.CONTROL_AE_MODE, value)
        }

    /**
     * Get current camera exposure range.
     *
     * @return exposure range.
     *
     * @see [availableCompensationStep]
     * @see [compensation]
     */
    val availableCompensationRange: Range<Int>
        get() = cameraController.cameraId?.let { context.getExposureRange(it) }
            ?: DEFAULT_COMPENSATION_RANGE

    /**
     * Get current camera exposure compensation step.
     *
     * This is the unit for [getExposureRange]. For example, if this key has a value of 1/2, then a
     * setting of -2 for  [getExposureRange] means that the target EV offset for the auto-exposure
     * routine is -1 EV.
     *
     * @return exposure range.
     *
     * @see [availableCompensationRange]
     * @see [compensation]
     */
    val availableCompensationStep: Rational
        get() = cameraController.cameraId?.let { context.getExposureStep(it) }
            ?: DEFAULT_COMPENSATION_STEP_RATIONAL

    /**
     * Set or get exposure compensation.
     *
     * @see [availableCompensationRange]
     * @see [availableCompensationStep]
     */
    var compensation: Int
        /**
         * Get the exposure compensation.
         *
         * @return exposure compensation
         */
        get() = cameraController.getSetting(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION)
            ?: DEFAULT_COMPENSATION
        /**
         * Set the exposure compensation.
         *
         * @param value exposure compensation
         */
        set(value) {
            cameraController.setRepeatingSetting(
                CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,
                value.clamp(availableCompensationRange)
            )
        }

    /**
     * Get maximum number of available exposure metering regions.
     */
    val maxNumOfMeteringRegions: Int
        get() = cameraController.cameraId?.let { context.getExposureMaxMeteringRegionsSupported(it) }
            ?: 0

    /**
     * Set/get exposure metering regions.
     */
    var meteringRegions: List<MeteringRectangle>
        get() = cameraController.getSetting(CaptureRequest.CONTROL_AE_REGIONS)?.toList()
            ?: emptyList()
        set(value) {
            cameraController.setRepeatingSetting(
                CaptureRequest.CONTROL_AE_REGIONS,
                value.toTypedArray()
            )
        }

    companion object {
        const val DEFAULT_COMPENSATION = 0
        val DEFAULT_COMPENSATION_RANGE = Range(DEFAULT_COMPENSATION, DEFAULT_COMPENSATION)
        val DEFAULT_COMPENSATION_STEP_RATIONAL = Rational(1, 1)
    }
}

sealed class Zoom(
    protected val context: Context,
    protected val cameraController: CameraController
) {
    abstract val availableRatioRange: Range<Float>
    internal abstract val cropSensorRegion: Rect

    abstract var zoomRatio: Float

    /**
     * Sets the zoom on pinch scale gesture.
     *
     * @param scale the scale factor
     */
    fun onPinch(scale: Float) {
        val scaledRatio: Float = zoomRatio * speedUpZoomByX(scale, 2)
        // Clamp the ratio with the zoom range.
        zoomRatio = scaledRatio.clamp(availableRatioRange.lower, availableRatioRange.upper)
    }

    private fun speedUpZoomByX(scaleFactor: Float, ratio: Int): Float {
        return if (scaleFactor > 1f) {
            1.0f + (scaleFactor - 1.0f) * ratio
        } else {
            1.0f - (1.0f - scaleFactor) * ratio
        }
    }

    class CropScalerRegionZoom(context: Context, cameraController: CameraController) :
        Zoom(context, cameraController) {
        // Keep the zoomRatio
        private var persistentZoomRatio = 1f
        private var currentCropRect: Rect? = null

        override val availableRatioRange: Range<Float>
            get() = cameraController.cameraId?.let {
                Range(
                    DEFAULT_ZOOM_RATIO,
                    context.getScalerMaxZoom(it)
                )
            }
                ?: DEFAULT_ZOOM_RATIO_RANGE

        override var zoomRatio: Float
            get() = synchronized(this) {
                persistentZoomRatio
            }
            set(value) {
                synchronized(this) {
                    val clampedValue = value.clamp(availableRatioRange)
                    cameraController.cameraId?.let { cameraId ->
                        currentCropRect = getCropRegion(
                            context.getCameraCharacteristics(cameraId),
                            clampedValue
                        )
                        cameraController.setRepeatingSetting(
                            CaptureRequest.SCALER_CROP_REGION,
                            currentCropRect
                        )
                    }
                    persistentZoomRatio = clampedValue
                }
            }

        override val cropSensorRegion: Rect
            get() {
                synchronized(this) {
                    return if (currentCropRect != null) {
                        currentCropRect!!
                    } else {
                        val cameraId = cameraController.cameraId
                            ?: throw IllegalStateException("Camera ID is null")
                        return context.getCameraCharacteristics(cameraId)
                            .get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)!!
                    }
                }
            }

        companion object {
            /**
             * Calculates sensor crop region for a zoom ratio (zoom >= 1.0).
             *
             * @return the crop region.
             */
            private fun getCropRegion(sensorRect: Rect, zoomRatio: Float): Rect {
                val xCenter: Int = sensorRect.width() / 2
                val yCenter: Int = sensorRect.height() / 2
                val xDelta = (0.5f * sensorRect.width() / zoomRatio).toInt()
                val yDelta = (0.5f * sensorRect.height() / zoomRatio).toInt()
                return Rect(xCenter - xDelta, yCenter - yDelta, xCenter + xDelta, yCenter + yDelta)
            }

            /**
             * Calculates sensor crop region for a zoom ratio (zoom >= 1.0).
             *
             * @return the crop region.
             */
            private fun getCropRegion(
                characteristics: CameraCharacteristics,
                zoomRatio: Float
            ): Rect {
                val sensorRect =
                    characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                        ?: throw IllegalStateException("Sensor rect is null")
                return getCropRegion(sensorRect, zoomRatio)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    class RZoom(context: Context, cameraController: CameraController) :
        Zoom(context, cameraController) {
        override val availableRatioRange: Range<Float>
            get() = cameraController.cameraId?.let { context.getZoomRatioRange(it) }
                ?: DEFAULT_ZOOM_RATIO_RANGE

        override var zoomRatio: Float
            get() = cameraController.getSetting(CaptureRequest.CONTROL_ZOOM_RATIO)
                ?: DEFAULT_ZOOM_RATIO
            set(value) {
                cameraController.setRepeatingSetting(
                    CaptureRequest.CONTROL_ZOOM_RATIO,
                    value.clamp(availableRatioRange)
                )
            }

        override val cropSensorRegion: Rect
            get() {
                val cameraId = cameraController.cameraId
                    ?: throw IllegalStateException("Camera ID is null")
                return context.getCameraCharacteristics(cameraId)
                    .get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)!!
            }
    }

    companion object {
        const val DEFAULT_ZOOM_RATIO = 1f
        val DEFAULT_ZOOM_RATIO_RANGE = Range(DEFAULT_ZOOM_RATIO, DEFAULT_ZOOM_RATIO)

        fun build(context: Context, cameraController: CameraController): Zoom {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                RZoom(context, cameraController)
            } else {
                CropScalerRegionZoom(context, cameraController)
            }
        }
    }
}


class Focus(private val context: Context, private val cameraController: CameraController) {
    /**
     * Get current camera supported auto focus mode.
     *
     * @return list of supported auto focus mode
     *
     * @see [autoMode]
     */
    val availableAutoModes: List<Int>
        get() = cameraController.cameraId?.let { context.getAutoFocusModes(it) } ?: emptyList()

    /**
     * Set or get auto focus mode.
     *
     * @see [availableAutoModes]
     */
    var autoMode: Int
        /**
         * Get the auto focus mode.
         *
         * @return auto focus mode
         */
        get() = cameraController.getSetting(CaptureRequest.CONTROL_AF_MODE)
            ?: CaptureResult.CONTROL_AF_MODE_OFF
        /**
         * Set the auto focus mode.
         *
         * @param value auto focus mode
         */
        set(value) {
            cameraController.setRepeatingSetting(CaptureRequest.CONTROL_AF_MODE, value)
        }

    /**
     * Get current camera lens distance range.
     *
     * @return camera lens distance range
     *
     * @see [lensDistance]
     */
    val availableLensDistanceRange: Range<Float>
        get() = cameraController.cameraId?.let { context.getLensDistanceRange(it) }
            ?: DEFAULT_LENS_DISTANCE_RANGE

    /**
     * Set or get lens focus distance.
     *
     * @see [availableLensDistanceRange]
     */
    var lensDistance: Float
        /**
         * Get the lens focus distance.
         *
         * @return lens focus distance
         */
        get() = cameraController.getSetting(CaptureRequest.LENS_FOCUS_DISTANCE)
            ?: DEFAULT_LENS_DISTANCE
        /**
         * Set the lens focus distance
         *
         * Only set lens focus distance if [autoMode] == [CaptureResult.CONTROL_AF_MODE_OFF].
         *
         * @param value lens focus distance
         */
        set(value) {
            cameraController.setRepeatingSetting(
                CaptureRequest.LENS_FOCUS_DISTANCE,
                value.clamp(availableLensDistanceRange)
            )
        }

    /**
     * Get maximum number of available focus metering regions.
     */
    val maxNumOfMeteringRegions: Int
        get() = cameraController.cameraId?.let { context.getFocusMaxMeteringRegionsSupported(it) }
            ?: DEFAULT_MAX_NUM_OF_METERING_REGION

    /**
     * Set/get focus metering regions.
     */
    var meteringRegions: List<MeteringRectangle>
        get() = cameraController.getSetting(CaptureRequest.CONTROL_AF_REGIONS)?.toList()
            ?: emptyList()
        set(value) {
            cameraController.setRepeatingSetting(
                CaptureRequest.CONTROL_AF_REGIONS,
                value.toTypedArray()
            )
        }

    companion object {
        const val DEFAULT_LENS_DISTANCE = 0f
        val DEFAULT_LENS_DISTANCE_RANGE = Range(DEFAULT_LENS_DISTANCE, DEFAULT_LENS_DISTANCE)

        const val DEFAULT_MAX_NUM_OF_METERING_REGION = 0
    }
}

class Stabilization(private val context: Context, private val cameraController: CameraController) {
    /**
     * Enable or disable video stabilization.
     *
     * Do not enable both [enableVideo] and [enableOptical] at the same time.
     */
    var enableVideo: Boolean
        /**
         * Checks if video stabilization is enabled.
         *
         * @return [Boolean.true] if video stabilization is enabled, otherwise [Boolean.false]
         */
        get() = cameraController.getSetting(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE) == CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE_ON
        /**
         * Enable or disable the video stabilization.
         *
         * @param value [Boolean.true] to enable video stabilization, otherwise [Boolean.false]
         */
        set(value) {
            if (value) {
                cameraController.setRepeatingSetting(
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE_ON
                )
            } else {
                cameraController.setRepeatingSetting(
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE_OFF
                )
            }
        }

    /**
     * Check if optical video stabilization is available.
     *
     * @return [Boolean.true] if optical video stabilization is supported, otherwise [Boolean.false]
     *
     * @see [enableOptical]
     */
    val availableOptical: Boolean
        get() = cameraController.cameraId?.let { context.isOpticalStabilizationAvailable(it) }
            ?: false

    /**
     * Enable or disable optical video stabilization.
     *
     * Do not enable both [enableVideo] and [enableOptical] at the same time.
     *
     * @see [availableOptical]
     */
    var enableOptical: Boolean
        /**
         * Checks if optical video stabilization is enabled.
         *
         * @return [Boolean.true] if optical video stabilization is enabled, otherwise [Boolean.false]
         */
        get() = cameraController.getSetting(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE) == CaptureResult.LENS_OPTICAL_STABILIZATION_MODE_ON
        /**
         * Enable or disable the optical video stabilization.
         *
         * @param value [Boolean.true] to enable optical video stabilization, otherwise [Boolean.false]
         */
        set(value) {
            if (value) {
                cameraController.setRepeatingSetting(
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                    CaptureResult.LENS_OPTICAL_STABILIZATION_MODE_ON
                )
            } else {
                cameraController.setRepeatingSetting(
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                    CaptureResult.LENS_OPTICAL_STABILIZATION_MODE_OFF
                )
            }
        }
}

class FocusMetering(
    private val context: Context,
    private val cameraController: CameraController,
    private val zoom: Zoom,
    private val focus: Focus,
    private val exposure: Exposure,
    private val whiteBalance: WhiteBalance
) {
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private var autoCancelHandle: ScheduledFuture<*>? = null

    @Suppress("UNCHECKED_CAST")
    private fun cancelAfAeTrigger() {
        // Cancel previous AF trigger
        val cancelTriggerMap =
            mutableMapOf(
                CaptureRequest.CONTROL_AF_TRIGGER to CameraMetadata.CONTROL_AF_TRIGGER_CANCEL
            )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cancelTriggerMap[CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER] =
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL
        }
        cameraController.setBurstSettings(cancelTriggerMap as Map<CaptureRequest.Key<Any>, Any>)
    }

    @Suppress("UNCHECKED_CAST")
    private fun addFocusMetering(
        afRects: List<MeteringRectangle>,
        aeRects: List<MeteringRectangle>,
        awbRects: List<MeteringRectangle>
    ) {
        val afMode = cameraController.cameraId?.let {
            getPreferredAFMode(
                context,
                it, CaptureRequest.CONTROL_AF_MODE_AUTO
            )
        } ?: throw IllegalStateException("Camera ID is null")

        // Add new regions
        val settingsMap: MutableMap<CaptureRequest.Key<*>, Any> = mutableMapOf(
            CaptureRequest.CONTROL_AF_MODE to afMode,
            CaptureRequest.CONTROL_AF_TRIGGER to CameraMetadata.CONTROL_AF_TRIGGER_START
        )

        if (afRects.isNotEmpty()) {
            settingsMap[CaptureRequest.CONTROL_AF_REGIONS] = afRects.toTypedArray()
        }
        if (aeRects.isNotEmpty()) {
            settingsMap[CaptureRequest.CONTROL_AE_REGIONS] = aeRects.toTypedArray()
        }
        if (awbRects.isNotEmpty()) {
            settingsMap[CaptureRequest.CONTROL_AWB_REGIONS] = awbRects.toTypedArray()
        }

        cameraController.setRepeatingSettings(settingsMap as Map<CaptureRequest.Key<Any>, Any>)
    }

    @Suppress("UNCHECKED_CAST")
    private fun triggerAf() {
        val aeMode = cameraController.cameraId?.let {
            getPreferredAEMode(
                context,
                it, CaptureRequest.CONTROL_AE_MODE_ON
            )
        } ?: throw IllegalStateException("Camera ID is null")
        val settingsMap: MutableMap<CaptureRequest.Key<*>, Any> = mutableMapOf(
            CaptureRequest.CONTROL_AF_TRIGGER to CameraMetadata.CONTROL_AF_TRIGGER_START,
            CaptureRequest.CONTROL_AE_MODE to aeMode
        )
        cameraController.setBurstSettings(settingsMap as Map<CaptureRequest.Key<Any>, Any>)
    }

    private fun startFocusAndMetering(
        afPoints: List<PointF>,
        aePoints: List<PointF>,
        awbPoints: List<PointF>,
        fovAspectRatio: Rational
    ) {
        if (afPoints.isEmpty() && aePoints.isEmpty() && awbPoints.isEmpty()) {
            Logger.e(TAG, "No focus/metering points provided")
            return
        }

        val cropRegion = zoom.cropSensorRegion

        disableAutoCancel()

        val maxAFRegion = focus.maxNumOfMeteringRegions
        val maxAERegion = exposure.maxNumOfMeteringRegions
        val maxWbRegion = whiteBalance.maxNumOfMeteringRegions

        if (maxAFRegion == 0 && maxAERegion == 0 && maxWbRegion == 0) {
            Logger.w(TAG, "No metering regions available")
            return
        }

        val afRectangles =
            getMeteringRectangles(
                afPoints,
                DEFAULT_AF_SIZE,
                maxAFRegion,
                cropRegion,
                fovAspectRatio
            )
        val aeRectangles =
            getMeteringRectangles(
                aePoints,
                DEFAULT_AE_SIZE,
                maxAERegion,
                cropRegion,
                fovAspectRatio
            )
        val awbRectangles =
            getMeteringRectangles(
                awbPoints,
                DEFAULT_AF_SIZE,
                maxWbRegion,
                cropRegion,
                fovAspectRatio
            )

        addFocusMetering(afRectangles, aeRectangles, awbRectangles)
        triggerAf()

        // Auto cancel AF trigger after DEFAULT_AUTO_CANCEL_DURATION_MS
        autoCancelHandle = scheduler.schedule(
            { cancelFocusAndMetering() },
            DEFAULT_AUTO_CANCEL_DURATION_MS,
            TimeUnit.MILLISECONDS
        )
    }

    private fun disableAutoCancel() {
        autoCancelHandle?.cancel(true)
        autoCancelHandle = null
    }


    /**
     * Computes rotation required to transform the camera sensor output orientation to the
     * device's current orientation in degrees.
     *
     * @param cameraId The camera to query for the sensor orientation.
     * @param surfaceRotationDegrees The current Surface orientation in degrees.
     * @return Relative rotation of the camera sensor output.
     */
    private fun getSensorRotationDegrees(
        context: Context,
        cameraId: String,
        surfaceRotationDegrees: Int = 0
    ): Int {
        val characteristics = context.getCameraCharacteristics(cameraId)
        val sensorOrientationDegrees =
            characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)

        require(sensorOrientationDegrees != null) {
            "Camera $cameraId has no defined sensor orientation."
        }

        // Reverse device orientation for back-facing cameras.
        val isFacingFront = characteristics.get(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_FRONT

        // Calculate desired orientation relative to camera orientation to make
        // the image upright relative to the device orientation.
        return getRelativeRotationDegrees(
            sensorOrientationDegrees,
            surfaceRotationDegrees,
            isFacingFront
        )
    }

    private fun getRelativeRotationDegrees(
        sourceRotationDegrees: Int,
        destRotationDegrees: Int,
        isFacingFront: Boolean
    ): Int {
        return if (isFacingFront) {
            (sourceRotationDegrees + destRotationDegrees + 360) % 360
        } else {
            (sourceRotationDegrees - destRotationDegrees + 360) % 360
        }
    }

    private fun normalizePoint(point: PointF, fovRect: Rect, relativeRotation: Int): PointF {
        val normalizedPoint = point.normalize(fovRect)
        return normalizedPoint.rotate(relativeRotation)
    }

    /**
     * Sets the focus on tap.
     *
     * @param point the point to focus on in [fovRect] coordinate system
     * @param fovRect the field of view rectangle
     * @param fovRotationDegree the orientation of the field of view
     */
    fun onTap(point: PointF, fovRect: Rect, fovRotationDegree: Int) {
        val points = listOf(point)
        return onTap(points, points, emptyList(), fovRect, fovRotationDegree)
    }

    /**
     * Sets the focus on tap.
     *
     * At least one of lost of points must not be empty.
     *
     * @param afPoints the points where the focus is done in [fovRect] coordinate system
     * @param aePoints the points where the exposure is done in [fovRect] coordinate system
     * @param awbPoints the points where the white balance is done in [fovRect] coordinate system
     * @param fovRect the field of view rectangle
     * @param fovRotationDegree the orientation of the field of view
     */
    fun onTap(
        afPoints: List<PointF> = emptyList(),
        aePoints: List<PointF> = emptyList(),
        awbPoints: List<PointF> = emptyList(),
        fovRect: Rect,
        fovRotationDegree: Int
    ) {
        val cameraId = cameraController.cameraId ?: throw IllegalStateException("Camera ID is null")
        val relativeRotation = getSensorRotationDegrees(context, cameraId, fovRotationDegree)

        startFocusAndMetering(
            afPoints.map { normalizePoint(it, fovRect, relativeRotation) },
            aePoints.map { normalizePoint(it, fovRect, relativeRotation) },
            awbPoints.map { normalizePoint(it, fovRect, relativeRotation) },
            if (context.isDevicePortrait) {
                Rational(fovRect.height(), fovRect.width())
            } else {
                Rational(fovRect.width(), fovRect.height())
            }
        )
    }

    /**
     * Cancel the focus and metering.
     */
    fun cancelFocusAndMetering() {
        disableAutoCancel()

        cancelAfAeTrigger()
        cameraController.updateRepeatingSession()
    }

    companion object {
        private const val TAG = "CameraSettings"

        private const val DEFAULT_AF_SIZE = 1.0f / 6.0f
        private const val DEFAULT_AE_SIZE = DEFAULT_AF_SIZE * 1.5f
        private const val DEFAULT_METERING_WEIGHT_MAX = MeteringRectangle.METERING_WEIGHT_MAX
        private const val DEFAULT_AUTO_CANCEL_DURATION_MS = 5000L

        private fun getPreferredAFMode(
            context: Context,
            cameraId: String,
            preferredMode: Int
        ): Int {
            val supportedAFModes = context.getAutoFocusModes(cameraId)

            if (supportedAFModes.contains(preferredMode)) {
                return preferredMode
            }

            if (supportedAFModes.contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)) {
                return CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            } else if (supportedAFModes.contains(CaptureRequest.CONTROL_AF_MODE_AUTO)) {
                return CaptureRequest.CONTROL_AF_MODE_AUTO
            }

            return CaptureRequest.CONTROL_AF_MODE_OFF
        }

        private fun getPreferredAEMode(
            context: Context,
            cameraId: String,
            preferredMode: Int
        ): Int {
            val supportedAEModes =
                context.getAutoExposureModes(cameraId)

            if (supportedAEModes.isEmpty()) {
                return CaptureRequest.CONTROL_AE_MODE_OFF
            }
            if (supportedAEModes.contains(preferredMode)) {
                return preferredMode
            }

            if (supportedAEModes.contains(CaptureRequest.CONTROL_AE_MODE_ON)) {
                return CaptureRequest.CONTROL_AE_MODE_ON
            }

            return CaptureRequest.CONTROL_AE_MODE_OFF
        }

        private fun getMeteringRectangles(
            points: List<PointF>,
            size: Float,
            maxNumOfRegions: Int,
            cropRegion: Rect,
            fovAspectRatio: Rational
        ): List<MeteringRectangle> {
            if (maxNumOfRegions == 0) {
                return emptyList()
            }

            val meteringRectangles = mutableListOf<MeteringRectangle>()
            val cropRegionAspectRatio = Rational(
                cropRegion.width(),
                cropRegion.height()
            )

            for (point in points) {
                if (meteringRectangles.size >= maxNumOfRegions) {
                    break
                }

                if (!point.isNormalized) {
                    continue
                }

                val adjustedPoint =
                    getFovAdjustedPoint(point, cropRegionAspectRatio, fovAspectRatio)
                val meteringRectangle = getMeteringRect(
                    size,
                    adjustedPoint,
                    cropRegion
                )

                meteringRectangles.add(meteringRectangle)
            }

            return meteringRectangles
        }

        private fun getFovAdjustedPoint(
            point: PointF,
            cropRegionAspectRatio: Rational,
            previewAspectRatio: Rational
        ): PointF {
            if (previewAspectRatio != cropRegionAspectRatio) {
                if (previewAspectRatio > cropRegionAspectRatio) {
                    // FOV is more narrow than crop region, top and down side of FOV is cropped.
                    val heightOfCropRegion = (previewAspectRatio.toDouble()
                            / cropRegionAspectRatio.toDouble()).toFloat()
                    val topPadding = ((heightOfCropRegion - 1.0) / 2).toFloat()
                    point.y = (topPadding + point.y) * (1 / heightOfCropRegion)
                } else {
                    // FOV is wider than crop region, left and right side of FOV is cropped.
                    val widthOfCropRegion = (cropRegionAspectRatio.toDouble()
                            / previewAspectRatio.toDouble()).toFloat()
                    val leftPadding = ((widthOfCropRegion - 1.0) / 2).toFloat()
                    point.x = (leftPadding + point.x) * (1f / widthOfCropRegion)
                }
            }
            return point
        }

        private fun getMeteringRect(
            size: Float,
            adjustedPoint: PointF,
            cropRegion: Rect
        ): MeteringRectangle {
            val centerX = (cropRegion.left + adjustedPoint.x * cropRegion.width()).toInt()
            val centerY = (cropRegion.top + adjustedPoint.y * cropRegion.height()).toInt()
            val width = (size * cropRegion.width())
            val height = (size * cropRegion.height())

            val focusRect = Rect(
                (centerX - width / 2).toInt(), (centerY - height / 2).toInt(),
                (centerX + width / 2).toInt(),
                (centerY + height / 2).toInt()
            )

            focusRect.left = focusRect.left.clamp(cropRegion.right, cropRegion.left)
            focusRect.right = focusRect.right.clamp(cropRegion.right, cropRegion.left)
            focusRect.top = focusRect.top.clamp(cropRegion.bottom, cropRegion.top)
            focusRect.bottom = focusRect.bottom.clamp(cropRegion.bottom, cropRegion.top)

            return MeteringRectangle(focusRect, DEFAULT_METERING_WEIGHT_MAX)
        }
    }
}
