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
package io.github.thibaultbee.streampack.core.elements.sources.video.camera

import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.ColorSpaceTransform
import android.hardware.camera2.params.MeteringRectangle
import android.hardware.camera2.params.RggbChannelVector
import android.os.Build
import android.util.Range
import android.util.Rational
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.controllers.CameraController
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.getAutoExposureModes
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.getAutoFocusModes
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.getAutoWhiteBalanceModes
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.getExposureMaxMeteringRegionsSupported
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.getExposureRange
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.getExposureStep
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.getFocusMaxMeteringRegionsSupported
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.getLensDistanceRange
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.getScalerMaxZoom
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.getSensitivityRange
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.getWhiteBalanceMeteringRegionsSupported
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.getZoomRatioRange
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.isFlashAvailable
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.isOpticalStabilizationAvailable
import io.github.thibaultbee.streampack.core.elements.utils.extensions.clamp
import io.github.thibaultbee.streampack.core.elements.utils.extensions.isApplicationPortrait
import io.github.thibaultbee.streampack.core.elements.utils.extensions.isNormalized
import io.github.thibaultbee.streampack.core.elements.utils.extensions.normalize
import io.github.thibaultbee.streampack.core.elements.utils.extensions.rotate
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


/**
 * Use to change camera settings.
 * This object is returned by [ICameraSource.settings].
 */
class CameraSettings internal constructor(
    cameraManager: CameraManager, private val cameraController: CameraController
) {
    /**
     * Current camera id.
     */
    val cameraId = cameraController.cameraId

    /**
     * Whether the camera is available.
     * To be used before calling any camera settings.
     */
    val isActiveFlow = cameraController.isActiveFlow

    /**
     * Current camera flash API.
     */
    val flash = Flash(cameraManager, this)

    /**
     * Current camera white balance API.
     */
    val whiteBalance = WhiteBalance(cameraManager, this)

    /**
     * Current camera ISO API.
     */
    val iso = Iso(cameraManager, this)

    /**
     * Current camera color correction API.
     */
    val colorCorrection = ColorCorrection(cameraManager, this)

    /**
     * Current camera exposure API.
     */
    val exposure = Exposure(cameraManager, this)

    /**
     * Current camera zoom API.
     */
    val zoom = Zoom.build(cameraManager, this)

    /**
     * Current focus API.
     */
    val focus = Focus(cameraManager, this)

    /**
     * Current stabilization API.
     */
    val stabilization = Stabilization(cameraManager, this)

    /**
     * Current focus metering API.
     */
    val focusMetering =
        FocusMetering(cameraManager, this, zoom, focus, exposure, whiteBalance)

    /**
     * Directly gets a [CaptureRequest] from the camera.
     *
     * @param key the key to get
     * @return the value associated with the key
     */
    fun <T> get(key: CaptureRequest.Key<T?>) = cameraController.getSetting(key)

    /**
     * Sets a [CaptureRequest] key to the camera.
     *
     * Call [applyRepeatingSession] to apply settings to the camera.
     *
     * @param key the key to set
     * @param value the value to set
     */
    fun <T> set(key: CaptureRequest.Key<T>, value: T) =
        cameraController.setSetting(key, value)

    /**
     * Applies settings to the camera repeatedly.
     */
    fun applyRepeatingSession() = runBlocking { cameraController.setRepeatingSession() }

    /**
     * Applies settings to the camera burst.
     */
    fun applyBurstSession() = runBlocking { cameraController.setBurstSession() }

    class Flash(
        private val cameraManager: CameraManager,
        private val cameraSettings: CameraSettings
    ) {
        /**
         * Whether the the current camera has a flash device.
         *
         * @return [Boolean.true] if camera has a flash device, [Boolean.false] otherwise.
         */
        val isAvailable: Boolean
            get() = cameraManager.isFlashAvailable(cameraSettings.cameraId)

        /**
         * Enables or disables flash.
         *
         * @see [isAvailable]
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
            cameraSettings.get(CaptureRequest.FLASH_MODE) ?: CaptureResult.FLASH_MODE_OFF

        private fun setFlash(mode: Int) {
            cameraSettings.set(CaptureRequest.FLASH_MODE, mode)
            cameraSettings.applyRepeatingSession()
        }
    }

    class WhiteBalance(
        private val cameraManager: CameraManager,
        private val cameraSettings: CameraSettings
    ) {
        /**
         * Gets supported auto white balance modes for the current camera
         *
         * @return list of supported white balance modes.
         */
        val availableAutoModes: List<Int>
            get() = cameraManager.getAutoWhiteBalanceModes(cameraSettings.cameraId)

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
            get() = cameraSettings.get(CaptureRequest.CONTROL_AWB_MODE)
                ?: CaptureResult.CONTROL_AWB_MODE_OFF
            /**
             * Get auto white balance mode.
             *
             * @param value auto white balance mode
             */
            set(value) {
                cameraSettings.set(CaptureRequest.CONTROL_AWB_MODE, value)
                cameraSettings.applyRepeatingSession()
            }

        /**
         * Get maximum number of available white balance metering regions.
         */
        val maxNumOfMeteringRegions: Int
            get() = cameraManager.getWhiteBalanceMeteringRegionsSupported(
                cameraSettings.cameraId
            ) ?: 0

        /**
         * Set/get white balance metering regions.
         */
        var meteringRegions: List<MeteringRectangle>
            get() = cameraSettings.get(CaptureRequest.CONTROL_AWB_REGIONS)?.toList()
                ?: emptyList()
            set(value) {
                cameraSettings.set(
                    CaptureRequest.CONTROL_AWB_REGIONS, value.toTypedArray()
                )
                cameraSettings.applyRepeatingSession()
            }

        /**
         * Gets or sets auto white balance lock.
         */
        var lock: Boolean
            get() = cameraSettings.get(CaptureRequest.CONTROL_AWB_LOCK) ?: false
            set(value) {
                cameraSettings.set(CaptureRequest.CONTROL_AWB_LOCK, value)
                cameraSettings.applyRepeatingSession()
            }
    }


    class Iso(
        private val cameraManager: CameraManager,
        private val cameraSettings: CameraSettings
    ) {
        /**
         * Get current camera supported sensitivity range.
         *
         * @return supported Sensitivity range
         *
         * @see [sensorSensitivity]
         */
        val availableSensorSensitivityRange: Range<Int>
            get() = cameraManager.getSensitivityRange(cameraSettings.cameraId)
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
            get() = cameraSettings.get(CaptureRequest.SENSOR_SENSITIVITY)
                ?: DEFAULT_SENSITIVITY
            /**
             * Set the sensitivity
             *
             * Only set lens focus distance if [Exposure.autoMode] == [CaptureResult.CONTROL_AE_MODE_OFF].
             *
             * @param value lens focus distance
             */
            set(value) {
                cameraSettings.set(
                    CaptureRequest.SENSOR_SENSITIVITY, value.clamp(availableSensorSensitivityRange)
                )
                cameraSettings.applyRepeatingSession()
            }

        companion object {
            const val DEFAULT_SENSITIVITY = 100
            val DEFAULT_SENSITIVITY_RANGE = Range(DEFAULT_SENSITIVITY, DEFAULT_SENSITIVITY)
        }
    }


    class ColorCorrection(
        private val cameraManager: CameraManager,
        private val cameraSettings: CameraSettings
    ) {
        /**
         * Whether the camera supports color correction.
         *
         * @return [Boolean.true] if camera has a flash device, [Boolean.false] otherwise.
         */
        val isAvailable: Boolean
            get() {
                return cameraManager.getCameraCharacteristics(cameraSettings.cameraId)
                    .get(CameraCharacteristics.COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES)
                    ?.contains(CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX) == true
            }

        /**
         * Sets or gets color correction gain.
         *
         * It will disable auto white balance mode if set.
         *
         * @return the color correction gain or null if not set
         * @see [isAvailable]
         * @see [WhiteBalance.autoMode]
         */
        var rggbChannelVector: RggbChannelVector?
            get() = cameraSettings.get(CaptureRequest.COLOR_CORRECTION_GAINS)
            set(value) {
                cameraSettings.set(
                    CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_OFF
                )
                cameraSettings.set(
                    CaptureRequest.COLOR_CORRECTION_MODE,
                    CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX
                )
                // 3*3 identity matrix represented in numerator, denominator format
                val identityMatrix =
                    intArrayOf(1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1)
                cameraSettings.set(
                    CaptureRequest.COLOR_CORRECTION_TRANSFORM,
                    ColorSpaceTransform(identityMatrix)
                )
                cameraSettings.set(
                    CaptureRequest.COLOR_CORRECTION_GAINS, value
                )
                cameraSettings.applyRepeatingSession()
            }
    }

    class Exposure(
        private val cameraManager: CameraManager,
        private val cameraSettings: CameraSettings
    ) {
        /**
         * Get current camera supported auto exposure mode.
         *
         * @return list of supported auto exposure mode
         *
         * @see [autoMode]
         */
        val availableAutoModes: List<Int>
            get() = cameraManager.getAutoExposureModes(cameraSettings.cameraId)


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
            get() = cameraSettings.get(CaptureRequest.CONTROL_AE_MODE)
                ?: CaptureResult.CONTROL_AE_MODE_OFF
            /**
             * Set the auto exposure mode.
             *
             * @param value auto exposure mode
             */
            set(value) {
                cameraSettings.set(CaptureRequest.CONTROL_AE_MODE, value)
                cameraSettings.applyRepeatingSession()
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
            get() = cameraManager.getExposureRange(cameraSettings.cameraId)
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
            get() = cameraManager.getExposureStep(cameraSettings.cameraId)
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
            get() = cameraSettings.get(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION)
                ?: DEFAULT_COMPENSATION
            /**
             * Set the exposure compensation.
             *
             * @param value exposure compensation
             */
            set(value) {
                cameraSettings.set(
                    CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,
                    value.clamp(availableCompensationRange)
                )
                cameraSettings.applyRepeatingSession()
            }

        /**
         * Get maximum number of available exposure metering regions.
         */
        val maxNumOfMeteringRegions: Int
            get() = cameraManager.getExposureMaxMeteringRegionsSupported(
                cameraSettings.cameraId
            ) ?: 0

        /**
         * Set/get exposure metering regions.
         */
        var meteringRegions: List<MeteringRectangle>
            get() = cameraSettings.get(CaptureRequest.CONTROL_AE_REGIONS)?.toList()
                ?: emptyList()
            set(value) {
                cameraSettings.set(
                    CaptureRequest.CONTROL_AE_REGIONS, value.toTypedArray()
                )
                cameraSettings.applyRepeatingSession()
            }

        /**
         * Gets or sets auto exposure lock.
         */
        var lock: Boolean
            get() = cameraSettings.get(CaptureRequest.CONTROL_AE_LOCK) ?: false
            set(value) {
                cameraSettings.set(
                    CaptureRequest.CONTROL_AE_LOCK, value
                )
                cameraSettings.applyRepeatingSession()
            }

        companion object {
            const val DEFAULT_COMPENSATION = 0
            val DEFAULT_COMPENSATION_RANGE = Range(DEFAULT_COMPENSATION, DEFAULT_COMPENSATION)
            val DEFAULT_COMPENSATION_STEP_RATIONAL = Rational(1, 1)
        }
    }

    sealed class Zoom(
        protected val cameraManager: CameraManager,
        protected val cameraSettings: CameraSettings
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

        class CropScalerRegionZoom(
            cameraManager: CameraManager, cameraSettings: CameraSettings
        ) : Zoom(cameraManager, cameraSettings) {
            // Keep the zoomRatio
            private var persistentZoomRatio = 1f
            private var currentCropRect: Rect? = null

            override val availableRatioRange: Range<Float>
                get() = Range(
                    DEFAULT_ZOOM_RATIO, cameraManager.getScalerMaxZoom(cameraSettings.cameraId)
                )

            override var zoomRatio: Float
                get() = synchronized(this) {
                    persistentZoomRatio
                }
                set(value) {
                    synchronized(this) {
                        val clampedValue = value.clamp(availableRatioRange)
                        currentCropRect = getCropRegion(
                            cameraManager.getCameraCharacteristics(cameraSettings.cameraId),
                            clampedValue
                        )
                        cameraSettings.set(
                            CaptureRequest.SCALER_CROP_REGION, currentCropRect
                        )
                        cameraSettings.applyRepeatingSession()
                        persistentZoomRatio = clampedValue
                    }
                }

            override val cropSensorRegion: Rect
                get() {
                    synchronized(this) {
                        return if (currentCropRect != null) {
                            currentCropRect!!
                        } else {
                            val cameraId = cameraSettings.cameraId
                            return cameraManager.getCameraCharacteristics(cameraId)
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
                    return Rect(
                        xCenter - xDelta, yCenter - yDelta, xCenter + xDelta, yCenter + yDelta
                    )
                }

                /**
                 * Calculates sensor crop region for a zoom ratio (zoom >= 1.0).
                 *
                 * @return the crop region.
                 */
                private fun getCropRegion(
                    characteristics: CameraCharacteristics, zoomRatio: Float
                ): Rect {
                    val sensorRect =
                        characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                            ?: throw IllegalStateException("Sensor rect is null")
                    return getCropRegion(sensorRect, zoomRatio)
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.R)
        class RZoom(cameraManager: CameraManager, cameraSettings: CameraSettings) :
            Zoom(cameraManager, cameraSettings) {
            override val availableRatioRange: Range<Float>
                get() = cameraManager.getZoomRatioRange(cameraSettings.cameraId)
                    ?: DEFAULT_ZOOM_RATIO_RANGE

            override var zoomRatio: Float
                get() = cameraSettings.get(CaptureRequest.CONTROL_ZOOM_RATIO)
                    ?: DEFAULT_ZOOM_RATIO
                set(value) {
                    cameraSettings.set(
                        CaptureRequest.CONTROL_ZOOM_RATIO, value.clamp(availableRatioRange)
                    )
                    cameraSettings.applyRepeatingSession()
                }

            override val cropSensorRegion: Rect
                get() {
                    val cameraId = cameraSettings.cameraId
                    return cameraManager.getCameraCharacteristics(cameraId)
                        .get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)!!
                }
        }

        companion object {
            const val DEFAULT_ZOOM_RATIO = 1f
            val DEFAULT_ZOOM_RATIO_RANGE = Range(DEFAULT_ZOOM_RATIO, DEFAULT_ZOOM_RATIO)

            fun build(
                cameraManager: CameraManager,
                cameraSettings: CameraSettings
            ): Zoom {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    RZoom(cameraManager, cameraSettings)
                } else {
                    CropScalerRegionZoom(cameraManager, cameraSettings)
                }
            }
        }
    }


    class Focus(
        private val cameraManager: CameraManager,
        private val cameraSettings: CameraSettings
    ) {
        /**
         * Get current camera supported auto focus mode.
         *
         * @return list of supported auto focus mode
         *
         * @see [autoMode]
         */
        val availableAutoModes: List<Int>
            get() = cameraManager.getAutoFocusModes(cameraSettings.cameraId)

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
            get() = cameraSettings.get(CaptureRequest.CONTROL_AF_MODE)
                ?: CaptureResult.CONTROL_AF_MODE_OFF
            /**
             * Set the auto focus mode.
             *
             * @param value auto focus mode
             */
            set(value) {
                cameraSettings.set(CaptureRequest.CONTROL_AF_MODE, value)
                cameraSettings.applyRepeatingSession()
            }

        /**
         * Get current camera lens distance range.
         *
         * @return camera lens distance range
         *
         * @see [lensDistance]
         */
        val availableLensDistanceRange: Range<Float>
            get() = cameraManager.getLensDistanceRange(cameraSettings.cameraId)

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
            get() = cameraSettings.get(CaptureRequest.LENS_FOCUS_DISTANCE)
                ?: DEFAULT_LENS_DISTANCE
            /**
             * Set the lens focus distance
             *
             * Only set lens focus distance if [autoMode] == [CaptureResult.CONTROL_AF_MODE_OFF].
             *
             * @param value lens focus distance
             */
            set(value) {
                cameraSettings.set(
                    CaptureRequest.LENS_FOCUS_DISTANCE, value.clamp(availableLensDistanceRange)
                )
                cameraSettings.applyRepeatingSession()
            }

        /**
         * Get maximum number of available focus metering regions.
         */
        val maxNumOfMeteringRegions: Int
            get() = cameraManager.getFocusMaxMeteringRegionsSupported(
                cameraSettings.cameraId
            ) ?: DEFAULT_MAX_NUM_OF_METERING_REGION

        /**
         * Set/get focus metering regions.
         */
        var meteringRegions: List<MeteringRectangle>
            get() = cameraSettings.get(CaptureRequest.CONTROL_AF_REGIONS)?.toList()
                ?: emptyList()
            set(value) {
                cameraSettings.set(
                    CaptureRequest.CONTROL_AF_REGIONS, value.toTypedArray()
                )
                cameraSettings.applyRepeatingSession()
            }

        companion object {
            const val DEFAULT_LENS_DISTANCE = 0f
            val DEFAULT_LENS_DISTANCE_RANGE = Range(DEFAULT_LENS_DISTANCE, DEFAULT_LENS_DISTANCE)

            const val DEFAULT_MAX_NUM_OF_METERING_REGION = 0
        }
    }

    class Stabilization(
        private val cameraManager: CameraManager,
        private val cameraSettings: CameraSettings
    ) {
        /**
         * Enable or disable video stabilization.
         *
         * Do not enable both [enableVideo] and [enableOptical] at the same time.
         */
        var enableVideo: Boolean
            /**
             * Whether the video stabilization is enabled.
             *
             * @return [Boolean.true] if video stabilization is enabled, otherwise [Boolean.false]
             */
            get() = cameraSettings.get(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE) == CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE_ON
            /**
             * Enable or disable the video stabilization.
             *
             * @param value [Boolean.true] to enable video stabilization, otherwise [Boolean.false]
             */
            set(value) {
                if (value) {
                    cameraSettings.set(
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                        CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE_ON
                    )
                } else {
                    cameraSettings.set(
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                        CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE_OFF
                    )
                    cameraSettings.applyRepeatingSession()
                }
            }

        /**
         * Whether the optical video stabilization is available.
         *
         * @return [Boolean.true] if optical video stabilization is supported, otherwise [Boolean.false]
         *
         * @see [enableOptical]
         */
        val isOpticalAvailable: Boolean
            get() = cameraManager.isOpticalStabilizationAvailable(
                cameraSettings.cameraId
            )


        /**
         * Enable or disable optical video stabilization.
         *
         * Do not enable both [enableVideo] and [enableOptical] at the same time.
         *
         * @see [isOpticalAvailable]
         */
        var enableOptical: Boolean
            /**
             * Whether the optical video stabilization is enabled.
             *
             * @return [Boolean.true] if optical video stabilization is enabled, otherwise [Boolean.false]
             */
            get() = cameraSettings.get(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE) == CaptureResult.LENS_OPTICAL_STABILIZATION_MODE_ON
            /**
             * Enable or disable the optical video stabilization.
             *
             * @param value [Boolean.true] to enable optical video stabilization, otherwise [Boolean.false]
             */
            set(value) {
                if (value) {
                    cameraSettings.set(
                        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                        CaptureResult.LENS_OPTICAL_STABILIZATION_MODE_ON
                    )
                } else {
                    cameraSettings.set(
                        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                        CaptureResult.LENS_OPTICAL_STABILIZATION_MODE_OFF
                    )
                }
                cameraSettings.applyRepeatingSession()
            }
    }

    class FocusMetering(
        private val cameraManager: CameraManager,
        private val cameraSettings: CameraSettings,
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
            cameraSettings.set(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_CANCEL
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraSettings.set(
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL
                )
            }
            cameraSettings.applyBurstSession()
        }

        @Suppress("UNCHECKED_CAST")
        private fun addFocusMetering(
            afRects: List<MeteringRectangle>,
            aeRects: List<MeteringRectangle>,
            awbRects: List<MeteringRectangle>
        ) {
            val afMode = getPreferredAFMode(
                cameraManager, cameraSettings.cameraId, CaptureRequest.CONTROL_AF_MODE_AUTO
            )

            // Add new regions
            cameraSettings.set(CaptureRequest.CONTROL_AF_MODE, afMode)
            cameraSettings.set(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_START
            )

            if (afRects.isNotEmpty()) {
                cameraSettings.set(
                    CaptureRequest.CONTROL_AF_REGIONS,
                    afRects.toTypedArray()
                )
            }
            if (aeRects.isNotEmpty()) {
                cameraSettings.set(
                    CaptureRequest.CONTROL_AE_REGIONS,
                    aeRects.toTypedArray()
                )
            }
            if (awbRects.isNotEmpty()) {
                cameraSettings.set(
                    CaptureRequest.CONTROL_AWB_REGIONS,
                    awbRects.toTypedArray()
                )
            }

            cameraSettings.applyRepeatingSession()
        }

        @Suppress("UNCHECKED_CAST")
        private fun triggerAf() {
            val aeMode = getPreferredAEMode(
                cameraManager, cameraSettings.cameraId, CaptureRequest.CONTROL_AE_MODE_ON
            )
            cameraSettings.set(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_START
            )
            cameraSettings.set(CaptureRequest.CONTROL_AE_MODE, aeMode)
            cameraSettings.applyBurstSession()
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

            val afRectangles = getMeteringRectangles(
                afPoints, DEFAULT_AF_SIZE, maxAFRegion, cropRegion, fovAspectRatio
            )
            val aeRectangles = getMeteringRectangles(
                aePoints, DEFAULT_AE_SIZE, maxAERegion, cropRegion, fovAspectRatio
            )
            val awbRectangles = getMeteringRectangles(
                awbPoints, DEFAULT_AF_SIZE, maxWbRegion, cropRegion, fovAspectRatio
            )

            addFocusMetering(afRectangles, aeRectangles, awbRectangles)
            triggerAf()

            // Auto cancel AF trigger after DEFAULT_AUTO_CANCEL_DURATION_MS
            autoCancelHandle = scheduler.schedule(
                { cancelFocusAndMetering() }, DEFAULT_AUTO_CANCEL_DURATION_MS, TimeUnit.MILLISECONDS
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
        @IntRange(from = 0, to = 359)
        private fun getSensorRotationDegrees(
            cameraManager: CameraManager,
            cameraId: String,
            @IntRange(from = 0, to = 359) surfaceRotationDegrees: Int = 0
        ): Int {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val sensorOrientationDegrees =
                characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)

            requireNotNull(sensorOrientationDegrees) {
                "Camera $cameraId has no defined sensor orientation."
            }

            // Reverse device orientation for back-facing cameras.
            val isFacingFront =
                characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT

            // Calculate desired orientation relative to camera orientation to make
            // the image upright relative to the device orientation.
            return getRelativeRotationDegrees(
                sensorOrientationDegrees, surfaceRotationDegrees, isFacingFront
            )
        }

        @IntRange(from = 0, to = 359)
        private fun getRelativeRotationDegrees(
            @IntRange(from = 0, to = 359) sourceRotationDegrees: Int,
            @IntRange(from = 0, to = 359) destRotationDegrees: Int,
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
         * @param context the application context
         * @param point the point to focus on in [fovRect] coordinate system
         * @param fovRect the field of view rectangle
         * @param fovRotationDegree the orientation of the field of view
         */
        fun onTap(context: Context, point: PointF, fovRect: Rect, fovRotationDegree: Int) {
            val points = listOf(point)
            return onTap(context, points, points, emptyList(), fovRect, fovRotationDegree)
        }

        /**
         * Sets the focus on tap.
         *
         * At least one of lost of points must not be empty.
         *
         * @param context the application context
         * @param afPoints the points where the focus is done in [fovRect] coordinate system
         * @param aePoints the points where the exposure is done in [fovRect] coordinate system
         * @param awbPoints the points where the white balance is done in [fovRect] coordinate system
         * @param fovRect the field of view rectangle
         * @param fovRotationDegree the orientation of the field of view
         */
        fun onTap(
            context: Context,
            afPoints: List<PointF> = emptyList(),
            aePoints: List<PointF> = emptyList(),
            awbPoints: List<PointF> = emptyList(),
            fovRect: Rect,
            fovRotationDegree: Int
        ) {
            val cameraId = cameraSettings.cameraId
            val relativeRotation =
                getSensorRotationDegrees(cameraManager, cameraId, fovRotationDegree)

            startFocusAndMetering(
                afPoints.map { normalizePoint(it, fovRect, relativeRotation) },
                aePoints.map { normalizePoint(it, fovRect, relativeRotation) },
                awbPoints.map { normalizePoint(it, fovRect, relativeRotation) },
                if (context.isApplicationPortrait) {
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
            cameraSettings.applyRepeatingSession()
        }

        companion object {
            private const val TAG = "CameraSettings"

            private const val DEFAULT_AF_SIZE = 1.0f / 6.0f
            private const val DEFAULT_AE_SIZE = DEFAULT_AF_SIZE * 1.5f
            private const val DEFAULT_METERING_WEIGHT_MAX = MeteringRectangle.METERING_WEIGHT_MAX
            private const val DEFAULT_AUTO_CANCEL_DURATION_MS = 5000L

            private fun getPreferredAFMode(
                cameraManager: CameraManager, cameraId: String, preferredMode: Int
            ): Int {
                val supportedAFModes = cameraManager.getAutoFocusModes(cameraId)

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
                cameraManager: CameraManager, cameraId: String, preferredMode: Int
            ): Int {
                val supportedAEModes = cameraManager.getAutoExposureModes(cameraId)

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
                    cropRegion.width(), cropRegion.height()
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
                        size, adjustedPoint, cropRegion
                    )

                    meteringRectangles.add(meteringRectangle)
                }

                return meteringRectangles
            }

            private fun getFovAdjustedPoint(
                point: PointF, cropRegionAspectRatio: Rational, previewAspectRatio: Rational
            ): PointF {
                if (previewAspectRatio != cropRegionAspectRatio) {
                    if (previewAspectRatio > cropRegionAspectRatio) {
                        // FOV is more narrow than crop region, top and down side of FOV is cropped.
                        val heightOfCropRegion =
                            (previewAspectRatio.toDouble() / cropRegionAspectRatio.toDouble()).toFloat()
                        val topPadding = ((heightOfCropRegion - 1.0) / 2).toFloat()
                        point.y = (topPadding + point.y) * (1 / heightOfCropRegion)
                    } else {
                        // FOV is wider than crop region, left and right side of FOV is cropped.
                        val widthOfCropRegion =
                            (cropRegionAspectRatio.toDouble() / previewAspectRatio.toDouble()).toFloat()
                        val leftPadding = ((widthOfCropRegion - 1.0) / 2).toFloat()
                        point.x = (leftPadding + point.x) * (1f / widthOfCropRegion)
                    }
                }
                return point
            }

            private fun getMeteringRect(
                size: Float, adjustedPoint: PointF, cropRegion: Rect
            ): MeteringRectangle {
                val centerX = (cropRegion.left + adjustedPoint.x * cropRegion.width()).toInt()
                val centerY = (cropRegion.top + adjustedPoint.y * cropRegion.height()).toInt()
                val width = (size * cropRegion.width())
                val height = (size * cropRegion.height())

                val focusRect = Rect(
                    (centerX - width / 2).toInt(),
                    (centerY - height / 2).toInt(),
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
}
