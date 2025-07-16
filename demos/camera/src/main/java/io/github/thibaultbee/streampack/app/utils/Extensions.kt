/*
 * Copyright (C) 2022 Thibault B.
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
package io.github.thibaultbee.streampack.app.utils

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Range
import androidx.annotation.RequiresPermission
import androidx.datastore.preferences.preferencesDataStore
import io.github.thibaultbee.streampack.app.ApplicationConstants.userPrefName
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.ICameraSource
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.backCameras
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.cameras
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.frontCameras
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.isBackCamera
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoSource
import io.github.thibaultbee.streampack.core.interfaces.setCameraId

@RequiresPermission(Manifest.permission.CAMERA)
suspend fun IWithVideoSource.setNextCameraId(context: Context) {
    val cameras = context.cameras
    val videoSource = videoInput?.sourceFlow?.value

    val newCameraId = if (videoSource is ICameraSource) {
        val currentCameraIndex = cameras.indexOf(videoSource.cameraId)
        (currentCameraIndex + 1) % cameras.size
    } else {
        0
    }

    setCameraId(cameras[newCameraId])
}

@RequiresPermission(Manifest.permission.CAMERA)
suspend fun IWithVideoSource.toggleBackToFront(context: Context) {
    val videoSource = videoInput?.sourceFlow?.value
    val cameras = if (videoSource is ICameraSource) {
        if (context.isBackCamera(videoSource.cameraId)) {
            context.frontCameras
        } else {
            context.backCameras
        }
    } else {
        context.frontCameras
    }

    if (cameras.isNotEmpty()) {
        setCameraId(cameras[0])
    } else {
        throw Exception("No camera available")
    }
}

/**
 * Gets the application's data store.
 */
val Context.dataStore by preferencesDataStore(
    name = userPrefName
)

fun Context.createVideoContentUri(name: String): Uri {
    val videoDetails = ContentValues().apply {
        put(MediaStore.Video.Media.TITLE, name)
        put(
            MediaStore.Video.Media.DISPLAY_NAME,
            name
        )
    }

    val resolver = this.contentResolver
    val collection =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

    return resolver.insert(collection, videoDetails)
        ?: throw Exception("Unable to create video file: $name")
}

fun Context.createAudioContentUri(name: String): Uri {
    val audioDetails = ContentValues().apply {
        put(MediaStore.Audio.Media.TITLE, name)
        put(
            MediaStore.Audio.Media.DISPLAY_NAME,
            name
        )
    }

    val resolver = this.contentResolver
    val collection =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

    return resolver.insert(collection, audioDetails)
        ?: throw Exception("Unable to create audio file: $name")
}

fun String.appendIfNotEndsWith(suffix: String): String {
    return if (this.endsWith(suffix)) {
        this
    } else {
        this + suffix
    }
}

val Range<*>.isEmpty: Boolean
    get() = upper == lower
