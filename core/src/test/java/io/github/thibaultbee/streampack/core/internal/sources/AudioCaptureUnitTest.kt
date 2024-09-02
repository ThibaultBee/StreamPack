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
package io.github.thibaultbee.streampack.core.internal.sources

import io.github.thibaultbee.streampack.core.internal.sources.audio.MicrophoneSource
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.internal.utils.FakeLogger
import org.junit.Assert
import org.junit.Test
import java.nio.ByteBuffer

class MicrophoneSourceUnitTest {
    init {
        Logger.logger = FakeLogger()
    }

    @Test
    fun `assert exception on bad state`() {
        val microphoneSource = MicrophoneSource()
        try {
            microphoneSource.startStream()
            Assert.fail()
        } catch (_: Throwable) {
        }
        try {
            microphoneSource.getFrame(ByteBuffer.allocate(10))
            Assert.fail()
        } catch (_: Throwable) {
        }
    }

    @Test
    fun `assert no exception on bad state`() {
        val microphoneSource = MicrophoneSource()
        try {
            microphoneSource.stopStream()
        } catch (t: Throwable) {
            Assert.fail()
        }
        try {
            microphoneSource.release()
        } catch (t: Throwable) {
            Assert.fail()
        }
    }
}