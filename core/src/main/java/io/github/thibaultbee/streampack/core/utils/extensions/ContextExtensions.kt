/*
 * Copyright 2022 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.core.utils.extensions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager

/**
 * Get if the system supports the given feature.
 *
 * @param featureName the feature name from [PackageManager.FEATURE_*]
 * @return true if the feature is supported, false otherwise
 */
fun Context.hasSystemFeature(featureName: String) = packageManager.hasSystemFeature(featureName)

/**
 * Get if the system supports external cameras.
 *
 * @return true if the feature is supported, false otherwise
 */
fun Context.hasExternalCamera() = hasSystemFeature(PackageManager.FEATURE_CAMERA_EXTERNAL)

/**
 * Gets the [MediaProjection] from the result of the screen capture request.
 *
 * This is a convenience method to avoid casting the system service to [MediaProjectionManager].
 *
 * @param resultCode the result code from the screen capture request
 * @param resultData the result data from the screen capture request
 */
fun Context.getMediaProjection(resultCode: Int, resultData: Intent): MediaProjection {
    val mediaProjectionManager =
        getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    return mediaProjectionManager.getMediaProjection(
        resultCode, resultData
    )
}
