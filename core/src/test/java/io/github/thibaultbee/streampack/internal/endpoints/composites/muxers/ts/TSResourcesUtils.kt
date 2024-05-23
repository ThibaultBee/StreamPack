/*
 * Copyright (C) 2023 Thibault B.
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
package io.github.thibaultbee.streampack.internal.endpoints.composites.muxers.ts

import io.github.thibaultbee.streampack.utils.ResourcesUtils
import java.nio.ByteBuffer

/**
 * Path to TS muxer test samples
 */
object TSResourcesUtils {
    fun readResources(fileName: String) = ResourcesUtils.readResources(TS_SAMPLES_PATH + fileName)
    fun readByteBuffer(fileName: String): ByteBuffer =
        ByteBuffer.wrap(readResources(fileName))

    private const val TS_SAMPLES_PATH = "test-samples/muxer/ts/"
}