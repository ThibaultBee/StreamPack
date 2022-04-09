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
package io.github.thibaultbee.streampack.streamer.testcases

import android.Manifest
import androidx.test.rule.GrantPermissionRule
import io.github.thibaultbee.streampack.streamers.bases.BaseAudioOnlyStreamer
import io.github.thibaultbee.streampack.utils.AndroidUtils
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test

abstract class AudioOnlyStreamerTestCase :
    StreamerTestCase() {
    abstract override val streamer: BaseAudioOnlyStreamer

    @get:Rule
    val runtimePermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    @Test
    override fun defaultUsageTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig()
            )
            streamer.startStream()
            streamer.stopStream()
            streamer.release()
        } catch (e: Exception) {
            fail("Default usage must not throw exception $e")
        }
    }

    @Test
    override fun defaultUsageTest2() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig()
            )
            streamer.startStream()
            streamer.stopStream()
            streamer.release()
        } catch (e: Exception) {
            fail("Default usage must not throw exception $e")
        }
    }

    // Single method calls
    @Test
    override fun configureAudioTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig()
            )
        } catch (e: Exception) {
            fail("Must be possible to only configure audio without exception: $e")
        }
    }

    @Test
    override fun configureVideoTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidVideoConfig()
            )
            fail("Must not be possible to configure video")
        } catch (e: Exception) {
        }
    }

    @Test
    override fun configureTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig(),
                AndroidUtils.fakeValidVideoConfig()
            )
            fail("Must not be possible to configure video")
        } catch (e: Exception) {
        }
    }

    @Test
    override fun configureErrorTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeInvalidAudioConfig()
            )
            fail("Invalid configuration must throw an exception")
        } catch (e: Exception) {
        }
    }

    // Multiple methods calls
    @Test
    override fun configureReleaseTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig()
            )
            streamer.release()
        } catch (e: Exception) {
            fail("Must be possible to configure/release but catches exception: $e")
        }
    }

    @Test
    override fun configureStopStreamTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig()
            )
            streamer.stopStream()
        } catch (e: Exception) {
            fail("Must be possible to configure/stopStream but catches exception: $e")
        }
    }

    @Test
    override fun startStreamReleaseTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig()
            )
            streamer.startStream()
            streamer.release()
        } catch (e: Exception) {
            fail("Must be possible to startStream/release but catches exception: $e")
        }
    }

    @Test
    override fun startStreamStopStreamTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig()
            )
            streamer.startStream()
            streamer.stopStream()
        } catch (e: Exception) {
            fail("Must be possible to startStream/stopStream but catches exception: $e")
        }
    }

    @Test
    override fun multipleStartStreamStopStreamTest() {
        try {
            streamer.configure(
                AndroidUtils.fakeValidAudioConfig()
            )
            (0..10).forEach { _ ->
                streamer.startStream()
                streamer.stopStream()
            }
        } catch (e: Exception) {
            fail("Must be possible to startStream/stopStream multiple times but catches exception: $e")
        }
    }

    @Test
    override fun multipleConfigureTest() {
        try {
            (0..10).forEach { _ ->
                streamer.configure(
                    AndroidUtils.fakeValidAudioConfig()
                )
            }
        } catch (e: Exception) {
            fail("Must be possible to call configure multiple times but catches exception: $e")
        }
    }
}