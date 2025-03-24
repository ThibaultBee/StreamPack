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
package io.github.thibaultbee.streampack.core.elements.sources

import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.MicrophoneSource
import io.github.thibaultbee.streampack.core.elements.utils.StubLogger
import io.github.thibaultbee.streampack.core.elements.utils.StubRawFrameFactory
import io.github.thibaultbee.streampack.core.logger.Logger
import org.junit.Assert
import org.junit.Test

class MicrophoneSourceUnitTest {
    init {
        Logger.logger = StubLogger()
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
            microphoneSource.getAudioFrame(StubRawFrameFactory())
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