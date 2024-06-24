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
package io.github.thibaultbee.streampack.core.internal.utils

import android.media.MediaFormat
import android.util.Size
import io.github.thibaultbee.streampack.core.internal.utils.TimeUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import java.nio.ByteBuffer

object MockUtils {
    fun mockSizeConstructor(width: Int, height: Int) {
        mockkConstructor(Size::class)
        every { anyConstructed<Size>().width } returns width
        every { anyConstructed<Size>().height } returns height
    }

    fun mockSize(width: Int, height: Int): Size {
        val mockk = mockk<Size>()
        every { mockk.width } returns width
        every { mockk.height } returns height
        return mockk
    }

    fun mockTimeUtils(currentTime: Long) {
        mockkObject(TimeUtils)
        every { TimeUtils.currentTime() } returns currentTime
    }

    fun mockMediaFormatConstructor(mimeType: String, csd: ByteBuffer? = null) {
        mockkConstructor(MediaFormat::class)
        every { anyConstructed<MediaFormat>().getString(MediaFormat.KEY_MIME) } returns mimeType
        every { anyConstructed<MediaFormat>().getByteBuffer("csd-0") } returns csd
    }
}