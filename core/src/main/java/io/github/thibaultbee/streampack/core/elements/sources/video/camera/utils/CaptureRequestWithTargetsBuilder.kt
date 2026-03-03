/*
 * Copyright 2025 Thibault B.
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
package io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils

import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureRequest.Builder
import android.view.Surface
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A class to store for [CaptureRequest] with targets
 */
internal class CaptureRequestWithTargetsBuilder(
    private var captureRequestBuilder: Builder
) {
    private val mutableTargets = mutableSetOf<CameraSurface>()
    private val mutex = Mutex()

    /**
     * Gets the targets from the CaptureRequest
     *
     * @return The targets
     */
    suspend fun getTargets() = mutex.withLock { mutableTargets.toList() }

    /**
     * Whether the CaptureRequest has no target
     */
    suspend fun isEmpty() = mutex.withLock { mutableTargets.isEmpty() }

    /**
     * Whether the [CaptureRequest.Builder] has a target
     */
    suspend fun hasTarget(cameraSurface: CameraSurface): Boolean =
        mutex.withLock { mutableTargets.contains(cameraSurface) }

    /**
     * Whether the [CaptureRequest.Builder] has a target
     */
    suspend fun hasTarget(surface: Surface): Boolean = mutex.withLock {
        mutableTargets.any { it.surface == surface }
    }

    /**
     * Whether the [CaptureRequest.Builder] has a target
     */
    suspend fun hasTarget(name: String): Boolean = mutex.withLock {
        mutableTargets.any { it.name == name }
    }

    /**
     * Adds a target to the CaptureRequest from a [CameraSurface]
     *
     * @param cameraSurface The surface to add
     * @return true if the surface was added, false otherwise
     */
    suspend fun addTarget(cameraSurface: CameraSurface): Boolean = mutex.withLock {
        require(cameraSurface.surface.isValid) { "Target $cameraSurface must be valid" }

        val wasAdded = mutableTargets.add(cameraSurface)
        if (wasAdded) {
            captureRequestBuilder.addTarget(cameraSurface.surface)
        }
        return wasAdded
    }

    /**
     * Adds targets to the CaptureRequest
     *
     * @param cameraSurfaces The surfaces to add
     * @return true if the surface was added, false otherwise
     */
    suspend fun addTargets(cameraSurfaces: List<CameraSurface>) = mutex.withLock {
        require(cameraSurfaces.isNotEmpty()) { "At least one target is required" }
        require(cameraSurfaces.all { it.surface.isValid }) { "All targets must be valid" }

        cameraSurfaces.forEach {
            val wasAdded = mutableTargets.add(it)
            if (wasAdded) {
                captureRequestBuilder.addTarget(it.surface)
            }
        }
    }

    /**
     * Removes a target from the CaptureRequest
     *
     * @param cameraSurface The surface to remove
     * @return true if the surface was removed, false otherwise
     */
    suspend fun removeTarget(cameraSurface: CameraSurface): Boolean = mutex.withLock {
        val wasRemoved = mutableTargets.remove(cameraSurface)
        if (wasRemoved) {
            captureRequestBuilder.removeTarget(cameraSurface.surface)
        }
        return wasRemoved
    }

    /**
     * Clears all targets from the CaptureRequest
     */
    suspend fun clearTargets() = mutex.withLock {
        mutableTargets.forEach {
            captureRequestBuilder.removeTarget(it.surface)
        }
        mutableTargets.clear()
    }

    /**
     * Same as [CaptureRequest.Builder.set]
     */
    fun <T> set(key: CaptureRequest.Key<T>, value: T) = captureRequestBuilder.set(key, value)

    /**
     * Same as [CaptureRequest.Builder.get]
     */
    fun <T> get(key: CaptureRequest.Key<T?>) = captureRequestBuilder.get(key)

    /**
     * Same as [CaptureRequest.Builder.setTag]
     */
    fun setTag(tag: Any) = captureRequestBuilder.setTag(tag)

    /**
     * Same as [CaptureRequest.Builder.build]
     */
    fun build() = captureRequestBuilder.build()

    override fun toString(): String {
        return "$captureRequestBuilder with targets: $mutableTargets"
    }
}