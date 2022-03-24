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
package io.github.thibaultbee.streampack.utils

import io.github.thibaultbee.streampack.internal.data.Frame
import java.nio.ByteBuffer
import kotlin.random.Random

object FakeFrames {
    fun createFakeKeyFrame(mimeType: String) = Frame(
        ByteBuffer.wrap(Random.nextBytes(1024)),
        mimeType,
        Random.nextLong(),
        isKeyFrame = true,
        extra = listOf(ByteBuffer.wrap(Random.nextBytes(10)))
    )

    fun createFakeFrame(mimeType: String) = Frame(
        ByteBuffer.wrap(Random.nextBytes(1024)),
        mimeType,
        Random.nextLong(),
        isKeyFrame = false
    )
}