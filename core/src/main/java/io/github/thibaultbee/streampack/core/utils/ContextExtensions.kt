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
package io.github.thibaultbee.streampack.core.utils

import android.content.Context
import android.content.pm.PackageManager

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