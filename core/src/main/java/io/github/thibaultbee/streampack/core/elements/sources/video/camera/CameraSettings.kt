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
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.extensions.is90or270
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.controllers.CameraController
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.autoExposureModes
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.autoFocusModes
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.autoWhiteBalanceModes
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.exposureRange
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.exposureStep
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.isBackCamera
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.isFlashAvailable
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.isFrontCamera
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.isOpticalStabilizationAvailable
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.lensDistanceRange
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.maxNumberOfExposureMeteringRegions
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.maxNumberOfFocusMeteringRegions
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.maxNumberOfWhiteBalanceMeteringRegions
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.scalerMaxZoom
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.sensitivityRange
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.zoomRatioRange
import io.github.thibaultbee.streampack.core.elements.utils.extensions.clamp
import io.github.thibaultbee.streampack.core.elements.utils.extensions.isApplicationPortrait
import io.github.thibaultbee.streampack.core.elements.utils.extensions.isNormalized
import io.github.thibaultbee.streampack.core.elements.utils.extensions.launchIn
import io.github.thibaultbee.streampack.core.elements.utils.extensions.normalize
import io.github.thibaultbee.streampack.core.elements.utils.extensions.rotate
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


/**
 * Use to change camera settings.
 * This object is returned by [ICameraSource.settings].
 *
 * @param characteristics Camera characteristics of the current camera.
 */
class CameraSettings internal constructor(
    val coroutineScope: CoroutineScope,
    val characteristics: CameraCharacteristics,
    private val cameraController: CameraController
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
    val flash = Flash(characteristics, this)

    /**
     * Current camera white balance API.
     */
    val whiteBalance = WhiteBalance(characteristics, this)

    /**
     * Current camera ISO API.
     */
    val iso = Iso(characteristics, this)

    /**
     * Current camera color correction API.
     */
    val colorCorrection = ColorCorrection(characteristics, this)

    /**
     * Current camera exposure API.
     */
    val exposure = Exposure(characteristics, this)

    /**
     * Current camera zoom API.
     */
    val zoom = Zoom.build(characteristics, this)

    /**
     * Current focus API.
     */
    val focus = Focus(characteristics, this)

    /**
     * Current stabilization API.
     */
    val stabilization = Stabilization(characteristics, this)

    /**
     * Current focus metering API.
     */
    val focusMetering =
        FocusMetering(coroutineScope, characteristics, this, zoom, focus, exposure, whiteBalance)

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
     * Applies settings to the camera repeatedly in a synchronized way.
     *
     * This method returns when the capture callback is received with the passed request.
     */
    suspend fun applyRepeatingSessionSync() = cameraController.setRepeatingSessionSync()

    /**
     * Applies settings to the camera repeatedly.
     */
    suspend fun applyRepeatingSession() = cameraController.setRepeatingSession()

    class Flash(
        private val characteristics: CameraCharacteristics,
        private val cameraSettings: CameraSettings
    ) {
        /**
         * Whether the the current camera has a flash device.
         *
         * @return `true` if camera has a flash device, [Boolean.Companion.toString] otherwise.
         */
        val isAvailable: Boolean
            get() = characteristics.isFlashAvailable

        /**
         * Enables or disables flash.
         *
         * @see [isAvailable]
         */
        val isEnable: Boolean
            /**
             * @return `true` if flash is already on, otherwise `false`
             */
            get() = getFlash() == CaptureResult.FLASH_MODE_TORCH

        private fun getFlash(): Int =
            cameraSettings.get(CaptureRequest.FLASH_MODE) ?: CaptureResult.FLASH_MODE_OFF

        /**
         * Enable or disable the flash.
         *
         * @param isEnable `true` to enable flash, otherwise `false`
         */
        suspend fun setIsEnable(isEnable: Boolean) {
            if (isEnable) {
                setFlash(CaptureRequest.FLASH_MODE_TORCH)
            } else {
                setFlash(CaptureRequest.FLASH_MODE_OFF)
            }
        }

        /**
         * Set flash mode.
         *
         * **See Also:** [FLASH_MODE](https://developer.android.com/reference/android/hardware/camera2/CaptureRequest#FLASH_MODE)
         *
         * @param mode flash mode
         */
        private suspend fun setFlash(mode: Int) {
            cameraSettings.set(CaptureRequest.FLASH_MODE, mode)
            cameraSettings.applyRepeatingSession()
        }
    }

    class WhiteBalance(
        private val characteristics: CameraCharacteristics,
        private val cameraSettings: CameraSettings
    ) {
        /**
         * Gets supported auto white balance modes for the current camera
         *
         * @return list of supported white balance modes.
         */
        val availableAutoModes: List<Int>
            get() = characteristics.autoWhiteBalanceModes

        /**
         * Gets the auto white balance mode.
         *
         * **See Also:** [CONTROL_AWB_MODE](https://developer.android.com/reference/android/hardware/camera2/CaptureRequest#CONTROL_AWB_MODE)
         * @see [availableAutoModes]
         */
        val autoMode: Int
            /**
             * Gets auto white balance mode.
             *
             * @return current camera auto white balance mode
             */
            get() = cameraSettings.get(CaptureRequest.CONTROL_AWB_MODE)
                ?: CaptureResult.CONTROL_AWB_MODE_OFF

        /**
         * Sets auto white balance mode.
         *
         * @param autoMode auto white balance mode
         * @see [availableAutoModes]
         */
        suspend fun setAutoMode(autoMode: Int) {
            cameraSettings.set(CaptureRequest.CONTROL_AWB_MODE, autoMode)
            cameraSettings.applyRepeatingSession()
        }

        /**
         * Get maximum number of available white balance metering regions.
         */
        val maxNumOfMeteringRegions: Int
            get() = characteristics.maxNumberOfWhiteBalanceMeteringRegions

        /**
         * Gets the white balance metering regions.
         */
        val meteringRegions: List<MeteringRectangle>
            get() = cameraSettings.get(CaptureRequest.CONTROL_AWB_REGIONS)?.toList()
                ?: emptyList()

        /**
         * Sets the white balance metering regions.
         */
        suspend fun setMeteringRegions(value: List<MeteringRectangle>) {
            cameraSettings.set(
                CaptureRequest.CONTROL_AWB_REGIONS, value.toTypedArray()
            )
            cameraSettings.applyRepeatingSession()
        }

        /**
         * Gets the auto white balance lock state.
         */
        val isLocked: Boolean
            get() = cameraSettings.get(CaptureRequest.CONTROL_AWB_LOCK) ?: false

        /**
         * Sets the auto white balance lock state.
         *
         * @param isLocked the lock state. `true` to lock auto white balance, otherwise `false`
         */
        suspend fun setIsLocked(isLocked: Boolean) {
            cameraSettings.set(CaptureRequest.CONTROL_AWB_LOCK, isLocked)
            cameraSettings.applyRepeatingSession()
        }
    }

    class Iso(
        private val characteristics: CameraCharacteristics,
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
            get() = characteristics.sensitivityRange ?: DEFAULT_SENSITIVITY_RANGE

        /**
         * Gets lens focus distance.
         *
         * @see [availableSensorSensitivityRange]
         */
        val sensorSensitivity: Int
            /**
             * Gets the sensitivity
             *
             * @return the sensitivity
             */
            get() = cameraSettings.get(CaptureRequest.SENSOR_SENSITIVITY)
                ?: DEFAULT_SENSITIVITY


        /**
         * Sets the sensitivity
         *
         * @param sensorSensitivity the sensitivity
         */
        suspend fun setSensorSensitivity(sensorSensitivity: Int) {
            cameraSettings.set(
                CaptureRequest.SENSOR_SENSITIVITY,
                sensorSensitivity.clamp(availableSensorSensitivityRange)
            )
            cameraSettings.applyRepeatingSession()
        }

        companion object {
            const val DEFAULT_SENSITIVITY = 100
            val DEFAULT_SENSITIVITY_RANGE = Range(DEFAULT_SENSITIVITY, DEFAULT_SENSITIVITY)
        }
    }

    class ColorCorrection(
        private val characteristics: CameraCharacteristics,
        private val cameraSettings: CameraSettings
    ) {
        /**
         * Whether the camera supports color correction.
         *
         * @return `true` if camera has a flash device, `false` otherwise.
         */
        val isAvailable: Boolean
            get() {
                return characteristics[CameraCharacteristics.COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES]
                    ?.contains(CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX) == true
            }

        /**
         * Gets color correction gain.
         *
         * @return the color correction gain or null if not set
         * @see [isAvailable]
         * @see [WhiteBalance.autoMode]
         */
        val rggbChannelVector: RggbChannelVector?
            get() = cameraSettings.get(CaptureRequest.COLOR_CORRECTION_GAINS)

        /**
         * Sets color correction gain.
         *
         * It will disable auto white balance mode if set.
         *
         * @param rggbChannelVector the color correction gain
         */
        suspend fun setRggbChannelVector(rggbChannelVector: RggbChannelVector) {
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
                CaptureRequest.COLOR_CORRECTION_GAINS, rggbChannelVector
            )
            cameraSettings.applyRepeatingSession()
        }
    }

    class Exposure(
        private val characteristics: CameraCharacteristics,
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
            get() = characteristics.autoExposureModes


        /**
         * Gets auto exposure mode.
         *
         * @see [availableAutoModes]
         */
        val autoMode: Int
            /**
             * Gets the auto exposure mode.
             *
             * @return the auto exposure mode
             */
            get() = cameraSettings.get(CaptureRequest.CONTROL_AE_MODE)
                ?: CaptureResult.CONTROL_AE_MODE_OFF

        /**
         * Sets the auto exposure mode.
         *
         * @param autoMode the exposure auto mode
         */
        suspend fun setAutoMode(autoMode: Int) {
            cameraSettings.set(CaptureRequest.CONTROL_AE_MODE, autoMode)
            cameraSettings.applyRepeatingSession()
        }

        /**
         * Gets current camera exposure range.
         *
         * @return exposure range.
         *
         * @see [availableCompensationStep]
         * @see [compensation]
         */
        val availableCompensationRange: Range<Int>
            get() = characteristics.exposureRange
                ?: DEFAULT_COMPENSATION_RANGE

        /**
         * Gets current camera exposure compensation step.
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
            get() = characteristics.exposureStep
                ?: DEFAULT_COMPENSATION_STEP_RATIONAL

        /**
         * Gets exposure compensation.
         *
         * @see [availableCompensationRange]
         * @see [availableCompensationStep]
         */
        val compensation: Int
            /**
             * Gets the exposure compensation.
             *
             * @return the exposure compensation
             */
            get() = cameraSettings.get(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION)
                ?: DEFAULT_COMPENSATION

        /**
         * Sets the exposure compensation.
         *
         * @param compensation the exposure compensation
         */
        suspend fun setCompensation(compensation: Int) {
            cameraSettings.set(
                CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,
                compensation.clamp(availableCompensationRange)
            )
            cameraSettings.applyRepeatingSession()
        }

        /**
         * Get maximum number of available exposure metering regions.
         */
        val maxNumOfMeteringRegions: Int
            get() = characteristics.maxNumberOfExposureMeteringRegions

        /**
         * Gets the exposure metering regions.
         */
        val meteringRegions: List<MeteringRectangle>
            get() = cameraSettings.get(CaptureRequest.CONTROL_AE_REGIONS)?.toList()
                ?: emptyList()

        /**
         * Sets the exposure metering regions.
         *
         * @param meteringRegions the metering regions
         */
        suspend fun setMeteringRegions(meteringRegions: List<MeteringRectangle>) {
            cameraSettings.set(
                CaptureRequest.CONTROL_AE_REGIONS, meteringRegions.toTypedArray()
            )
            cameraSettings.applyRepeatingSession()
        }

        /**
         * Gets auto exposure lock.
         *
         * @return the auto exposure lock state
         */
        val isLocked: Boolean
            get() = cameraSettings.get(CaptureRequest.CONTROL_AE_LOCK) ?: false

        /**
         * Sets the auto exposure lock.
         *
         * @param isLocked the lock state. `true` to lock auto exposure, otherwise `false`
         */
        suspend fun setIsLocked(isLocked: Boolean) {
            cameraSettings.set(
                CaptureRequest.CONTROL_AE_LOCK, isLocked
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
        protected val characteristics: CameraCharacteristics,
        protected val cameraSettings: CameraSettings
    ) {
        abstract val availableRatioRange: Range<Float>
        internal abstract suspend fun getCropSensorRegion(): Rect

        abstract suspend fun getZoomRatio(): Float
        abstract suspend fun setZoomRatio(zoomRatio: Float)

        /**
         * Sets the zoom on pinch scale gesture.
         *
         * @param scale the scale factor
         */
        suspend fun onPinch(scale: Float) {
            val scaledRatio: Float = getZoomRatio() * speedUpZoomByX(scale, 2)
            // Clamp the ratio with the zoom range.
            setZoomRatio(scaledRatio.clamp(availableRatioRange.lower, availableRatioRange.upper))
        }

        private fun speedUpZoomByX(scaleFactor: Float, ratio: Int): Float {
            return if (scaleFactor > 1f) {
                1.0f + (scaleFactor - 1.0f) * ratio
            } else {
                1.0f - (1.0f - scaleFactor) * ratio
            }
        }

        class CropScalerRegionZoom(
            characteristics: CameraCharacteristics,
            cameraSettings: CameraSettings
        ) : Zoom(characteristics, cameraSettings) {
            private val mutex = Mutex()

            // Keep the zoomRatio
            private var persistentZoomRatio = 1f
            private var currentCropRect: Rect? = null

            override val availableRatioRange: Range<Float>
                get() = Range(
                    DEFAULT_ZOOM_RATIO, characteristics.scalerMaxZoom
                )

            override suspend fun getZoomRatio(): Float = mutex.withLock {
                persistentZoomRatio
            }

            override suspend fun setZoomRatio(zoomRatio: Float) {
                mutex.withLock {
                    val clampedValue = zoomRatio.clamp(availableRatioRange)
                    currentCropRect = getCropRegion(
                        characteristics,
                        clampedValue
                    )
                    cameraSettings.set(
                        CaptureRequest.SCALER_CROP_REGION, currentCropRect
                    )
                    cameraSettings.applyRepeatingSession()
                    persistentZoomRatio = clampedValue
                }
            }

            override suspend fun getCropSensorRegion(): Rect = mutex.withLock {
                return if (currentCropRect != null) {
                    currentCropRect!!
                } else {
                    return characteristics[CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE]!!
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
        class RZoom(characteristics: CameraCharacteristics, cameraSettings: CameraSettings) :
            Zoom(characteristics, cameraSettings) {
            override val availableRatioRange: Range<Float>
                get() = characteristics.zoomRatioRange
                    ?: DEFAULT_ZOOM_RATIO_RANGE

            override suspend fun getZoomRatio(): Float {
                return cameraSettings.get(CaptureRequest.CONTROL_ZOOM_RATIO)
                    ?: DEFAULT_ZOOM_RATIO
            }

            override suspend fun setZoomRatio(zoomRatio: Float) {
                cameraSettings.set(
                    CaptureRequest.CONTROL_ZOOM_RATIO, zoomRatio.clamp(availableRatioRange)
                )
                cameraSettings.applyRepeatingSession()
            }

            override suspend fun getCropSensorRegion(): Rect {
                return characteristics[CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE]!!
            }
        }

        companion object {
            const val DEFAULT_ZOOM_RATIO = 1f
            val DEFAULT_ZOOM_RATIO_RANGE = Range(DEFAULT_ZOOM_RATIO, DEFAULT_ZOOM_RATIO)

            fun build(
                characteristics: CameraCharacteristics,
                cameraSettings: CameraSettings
            ): Zoom {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    RZoom(characteristics, cameraSettings)
                } else {
                    CropScalerRegionZoom(characteristics, cameraSettings)
                }
            }
        }
    }


    class Focus(
        private val characteristics: CameraCharacteristics,
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
            get() = characteristics.autoFocusModes

        /**
         * Gets the auto focus mode.
         *
         * @see [availableAutoModes]
         */
        val autoMode: Int
            /**
             * Gets the auto focus mode.
             *
             * @return the auto focus mode
             */
            get() = cameraSettings.get(CaptureRequest.CONTROL_AF_MODE)
                ?: CaptureResult.CONTROL_AF_MODE_OFF

        /**
         * Sets the auto focus mode.
         *
         * @param autoMode the auto focus mode
         */
        suspend fun setAutoMode(autoMode: Int) {
            cameraSettings.set(CaptureRequest.CONTROL_AF_MODE, autoMode)
            cameraSettings.applyRepeatingSession()
        }

        /**
         * Gets current camera lens distance range.
         *
         * @return camera lens distance range
         *
         * @see [lensDistance]
         */
        val availableLensDistanceRange: Range<Float>
            get() = characteristics.lensDistanceRange

        /**
         * Gets the lens focus distance.
         *
         * @see [availableLensDistanceRange]
         */
        val lensDistance: Float
            /**
             * Get the lens focus distance.
             *
             * @return the lens focus distance
             */
            get() = cameraSettings.get(CaptureRequest.LENS_FOCUS_DISTANCE)
                ?: DEFAULT_LENS_DISTANCE

        /**
         * Sets the lens focus distance
         *
         * Only set lens focus distance if [autoMode] == [CaptureResult.CONTROL_AF_MODE_OFF].
         *
         * @param lensDistance the lens focus distance
         */
        suspend fun setLensDistance(lensDistance: Float) {
            cameraSettings.set(
                CaptureRequest.LENS_FOCUS_DISTANCE, lensDistance.clamp(availableLensDistanceRange)
            )
            cameraSettings.applyRepeatingSession()
        }

        /**
         * Get maximum number of available focus metering regions.
         */
        val maxNumOfMeteringRegions: Int
            get() = characteristics.maxNumberOfFocusMeteringRegions
                ?: DEFAULT_MAX_NUM_OF_METERING_REGION

        /**
         * Gets the focus metering regions.
         */
        val meteringRegions: List<MeteringRectangle>
            get() = cameraSettings.get(CaptureRequest.CONTROL_AF_REGIONS)?.toList()
                ?: emptyList()

        /**
         * Sets the focus metering regions.
         *
         * @param meteringRegions the metering regions
         */
        suspend fun setMeteringRegions(meteringRegions: List<MeteringRectangle>) {
            cameraSettings.set(
                CaptureRequest.CONTROL_AF_REGIONS, meteringRegions.toTypedArray()
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
        private val characteristics: CameraCharacteristics,
        private val cameraSettings: CameraSettings
    ) {
        /**
         * Whether the video stabilization is enabled.
         *
         * Do not enable both [isEnableVideo] and [isEnableOptical] at the same time.
         */
        val isEnableVideo: Boolean
            /**
             * Whether the video stabilization is enabled.
             *
             * @return `true` if video stabilization is enabled, otherwise `false`
             */
            get() = cameraSettings.get(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE) == CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE_ON

        /**
         * Enables or disables the video stabilization.
         *
         * @param isEnableVideo `true` to enable video stabilization, otherwise `false`
         */
        suspend fun setIsEnableVideo(isEnableVideo: Boolean) {
            if (isEnableVideo) {
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
         * @return `true` if optical video stabilization is supported, otherwise `false`
         *
         * @see [isEnableOptical]
         */
        val isOpticalAvailable: Boolean
            get() = characteristics.isOpticalStabilizationAvailable


        /**
         * Whether the optical video stabilization is enabled.
         *
         * Do not enable both [isEnableVideo] and [isEnableOptical] at the same time.
         *
         * @see [isOpticalAvailable]
         */
        val isEnableOptical: Boolean
            /**
             * Whether the optical video stabilization is enabled.
             *
             * @return `true` if optical video stabilization is enabled, otherwise `false`
             */
            get() = cameraSettings.get(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE) == CaptureResult.LENS_OPTICAL_STABILIZATION_MODE_ON

        /**
         * Enable or disable the optical video stabilization.
         *
         * @param isEnableOptical `true` to enable optical video stabilization, otherwise `false`
         */
        suspend fun setIsEnableOptical(isEnableOptical: Boolean) {
            if (isEnableOptical) {
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
        private val coroutineScope: CoroutineScope,
        private val characteristics: CameraCharacteristics,
        private val cameraSettings: CameraSettings,
        private val zoom: Zoom,
        private val focus: Focus,
        private val exposure: Exposure,
        private val whiteBalance: WhiteBalance
    ) {
        private var autoCancelHandle: Job? = null

        @Suppress("UNCHECKED_CAST")
        private suspend fun cancelAfAeTrigger() {
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
            cameraSettings.applyRepeatingSession()
        }

        @Suppress("UNCHECKED_CAST")
        private fun addFocusMetering(
            afRects: List<MeteringRectangle>,
            aeRects: List<MeteringRectangle>,
            awbRects: List<MeteringRectangle>
        ) {
            val afMode = getPreferredAFMode(
                characteristics, CaptureRequest.CONTROL_AF_MODE_AUTO
            )

            cameraSettings.set(CaptureRequest.CONTROL_AF_MODE, afMode)

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
        }

        @Suppress("UNCHECKED_CAST")
        private suspend fun triggerAf(overrideAeMode: Boolean) {
            cameraSettings.set(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_START
            )
            if (overrideAeMode) {
                val aeMode = getPreferredAEMode(
                    characteristics, CaptureRequest.CONTROL_AE_MODE_ON
                )
                cameraSettings.set(CaptureRequest.CONTROL_AE_MODE, aeMode)
            }
            cameraSettings.applyRepeatingSession()
        }

        private suspend fun executeMetering(
            afRectangles: List<MeteringRectangle>,
            aeRectangles: List<MeteringRectangle>,
            awbRectangles: List<MeteringRectangle>,
            timeoutDurationMs: Long
        ) {
            disableAutoCancel()

            /**
             * Cancel previous AF/AE trigger.
             * Otherwise, some devices may ignore the new AF/AE trigger request.
             */
            cameraSettings.set(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_IDLE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraSettings.set(
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE
                )
            }
            cameraSettings.applyRepeatingSessionSync()

            addFocusMetering(afRectangles, aeRectangles, awbRectangles)
            if (afRectangles.isNotEmpty()) {
                triggerAf(true)
            }

            // Auto cancel AF trigger after timeoutDurationMs
            if (timeoutDurationMs > 0) {
                autoCancelHandle = coroutineScope.launchIn(timeoutDurationMs)
                {
                    try {
                        cancelFocusAndMetering()
                    } catch (t: Throwable) {
                        Logger.w(TAG, "Failed to auto cancel focus and metering", t)
                    }
                }
            }
        }

        private suspend fun startFocusAndMetering(
            afPoints: List<PointF>,
            aePoints: List<PointF>,
            awbPoints: List<PointF>,
            fovAspectRatio: Rational,
            timeoutDurationMs: Long
        ) {
            val cropRegion = zoom.getCropSensorRegion()

            val maxAFRegion = focus.maxNumOfMeteringRegions
            val maxAERegion = exposure.maxNumOfMeteringRegions
            val maxWbRegion = whiteBalance.maxNumOfMeteringRegions

            val afRectangles = getMeteringRectangles(
                afPoints, DEFAULT_AF_SIZE, maxAFRegion, cropRegion, fovAspectRatio
            )
            val aeRectangles = getMeteringRectangles(
                aePoints, DEFAULT_AE_SIZE, maxAERegion, cropRegion, fovAspectRatio
            )
            val awbRectangles = getMeteringRectangles(
                awbPoints, DEFAULT_AF_SIZE, maxWbRegion, cropRegion, fovAspectRatio
            )

            require(afRectangles.isNotEmpty() || aeRectangles.isNotEmpty() || awbRectangles.isNotEmpty()) {
                "At least one of AF, AE, AWB points must be non empty"
            }

            executeMetering(afRectangles, aeRectangles, awbRectangles, timeoutDurationMs)
        }

        private fun disableAutoCancel() {
            autoCancelHandle?.cancel()
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
            characteristics: CameraCharacteristics,
            cameraId: String,
            @IntRange(from = 0, to = 359) surfaceRotationDegrees: Int = 0
        ): Int {
            val sensorOrientationDegrees =
                characteristics[CameraCharacteristics.SENSOR_ORIENTATION]

            requireNotNull(sensorOrientationDegrees) {
                "Camera $cameraId has no defined sensor orientation."
            }

            // Reverse device orientation for back-facing cameras.
            val isOppositeFacing = characteristics.isBackCamera

            // Calculate desired orientation relative to camera orientation to make
            // the image upright relative to the device orientation.
            return getRelativeRotationDegrees(
                sensorOrientationDegrees, surfaceRotationDegrees, isOppositeFacing
            )
        }

        @IntRange(from = 0, to = 359)
        private fun getRelativeRotationDegrees(
            @IntRange(from = 0, to = 359) sourceRotationDegrees: Int,
            @IntRange(from = 0, to = 359) destRotationDegrees: Int,
            isOppositeFacing: Boolean
        ): Int {
            return if (isOppositeFacing) {
                (sourceRotationDegrees - destRotationDegrees + 360) % 360
            } else {
                (sourceRotationDegrees + destRotationDegrees) % 360
            }
        }

        private fun normalizePoint(point: PointF, fovRect: Rect, relativeRotation: Int): PointF {
            val normalizedPoint = point.normalize(fovRect)
            return normalizedPoint.rotate(relativeRotation)
        }

        private fun normalizePoint(
            point: PointF,
            fovRect: Rect,
            relativeRotation: Int,
            isFrontCamera: Boolean
        ): PointF {
            val normalizedPoint = normalizePoint(point, fovRect, relativeRotation)
            return if (isFrontCamera) {
                if (relativeRotation.is90or270) {
                    // If the rotation is 90/270, the Point should be flipped vertically.
                    //   +---+     90 +---+  270 +---+
                    //   | ^ | -->    | < |      | > |
                    //   +---+        +---+      +---+
                    PointF(
                        normalizedPoint.x,
                        1f - normalizedPoint.y
                    )
                } else {
                    // If the rotation is 0/180, the Point should be flipped horizontally.
                    //   +---+      0 +---+  180 +---+
                    //   | ^ | -->    | ^ |      | v |
                    //   +---+        +---+      +---+
                    PointF(
                        1f - normalizedPoint.x,
                        normalizedPoint.y
                    )
                }
            } else {
                normalizedPoint
            }
        }

        /**
         * Sets the focus on tap.
         *
         * @param context the application context
         * @param point the point to focus on in [fovRect] coordinate system
         * @param fovRect the field of view rectangle
         * @param fovRotationDegree the orientation of the field of view
         * @param timeoutDurationMs duration in milliseconds after which the focus and metering will be cancelled automatically
         */
        suspend fun onTap(
            context: Context,
            point: PointF,
            fovRect: Rect,
            fovRotationDegree: Int,
            timeoutDurationMs: Long = DEFAULT_AUTO_CANCEL_DURATION_MS
        ) {
            val points = listOf(point)
            return onTap(
                context,
                points,
                points,
                emptyList(),
                fovRect,
                fovRotationDegree,
                timeoutDurationMs
            )
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
         * @param timeoutDurationMs duration in milliseconds after which the focus and metering will be cancelled automatically
         */
        suspend fun onTap(
            context: Context,
            afPoints: List<PointF>,
            aePoints: List<PointF>,
            awbPoints: List<PointF>,
            fovRect: Rect,
            fovRotationDegree: Int,
            timeoutDurationMs: Long = DEFAULT_AUTO_CANCEL_DURATION_MS
        ) {
            val relativeRotation =
                getSensorRotationDegrees(
                    characteristics,
                    cameraSettings.cameraId,
                    fovRotationDegree
                )

            val isFrontCamera = characteristics.isFrontCamera
            startFocusAndMetering(
                afPoints.map { normalizePoint(it, fovRect, relativeRotation, isFrontCamera) },
                aePoints.map { normalizePoint(it, fovRect, relativeRotation, isFrontCamera) },
                awbPoints.map { normalizePoint(it, fovRect, relativeRotation, isFrontCamera) },
                if (context.isApplicationPortrait) {
                    Rational(fovRect.height(), fovRect.width())
                } else {
                    Rational(fovRect.width(), fovRect.height())
                },
                timeoutDurationMs
            )
        }

        /**
         * Cancel the focus and metering.
         */
        suspend fun cancelFocusAndMetering() {
            disableAutoCancel()

            cancelAfAeTrigger()
        }

        companion object {
            private const val DEFAULT_AF_SIZE = 1.0f / 6.0f
            private const val DEFAULT_AE_SIZE = DEFAULT_AF_SIZE * 1.5f
            private const val DEFAULT_METERING_WEIGHT_MAX =
                MeteringRectangle.METERING_WEIGHT_MAX
            const val DEFAULT_AUTO_CANCEL_DURATION_MS = 5000L

            private fun getPreferredAFMode(
                characteristics: CameraCharacteristics, preferredMode: Int
            ): Int {
                val supportedAFModes = characteristics.autoFocusModes

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
                characteristics: CameraCharacteristics, preferredMode: Int
            ): Int {
                val supportedAEModes = characteristics.autoExposureModes

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

    companion object {
        private const val TAG = "CameraSettings"
    }
}
