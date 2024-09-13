/*
 * Copyright (C) 2024 Thibault B.
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
package io.github.thibaultbee.streampack.core.streamer.file

import android.Manifest
import android.content.Context
import android.media.MediaFormat.MIMETYPE_AUDIO_AAC
import android.media.MediaFormat.MIMETYPE_AUDIO_OPUS
import android.media.MediaFormat.MIMETYPE_VIDEO_AVC
import android.media.MediaFormat.MIMETYPE_VIDEO_VP9
import android.util.Size
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import io.github.thibaultbee.streampack.core.data.AudioConfig
import io.github.thibaultbee.streampack.core.data.VideoConfig
import io.github.thibaultbee.streampack.core.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.data.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.streamers.DefaultCameraStreamer
import io.github.thibaultbee.streampack.core.utils.FileUtils
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@LargeTest
@RunWith(Parameterized::class)
class CameraStreamerFileTest(private val descriptor: MediaDescriptor, private val verify: Boolean) {
    private val context: Context = InstrumentationRegistry.getInstrumentation().context
    private val streamer = DefaultCameraStreamer(context)
    private val info = streamer.getInfo(descriptor)

    private val videoCodec =
        if (info.video.supportedEncoders.contains(MIMETYPE_VIDEO_AVC)) {
            MIMETYPE_VIDEO_AVC
        } else if (info.video.supportedEncoders.contains(MIMETYPE_VIDEO_VP9)) {
            MIMETYPE_VIDEO_VP9
        } else {
            throw IllegalArgumentException("No supported video codec")
        }

    private val audioCodec =
        if (info.audio.supportedEncoders.contains(MIMETYPE_AUDIO_AAC)) {
            MIMETYPE_AUDIO_AAC
        } else if (info.audio.supportedEncoders.contains(MIMETYPE_AUDIO_OPUS)) {
            MIMETYPE_AUDIO_OPUS
        } else {
            throw IllegalArgumentException("No supported audio codec")
        }

    private val audioSampleRate =
        if (info.audio.getSupportedSampleRates(audioCodec).contains(44_100)) {
            44100
        } else if (info.audio.getSupportedSampleRates(audioCodec).contains(48_000)) {
            48_000
        } else {
            throw IllegalArgumentException("No supported audio sample rate for $audioCodec")
        }

    @get:Rule
    val runtimePermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

    @Test
    fun writeToFile() = runTest(timeout = 200.seconds) {
        val audioConfig = AudioConfig(mimeType = audioCodec, sampleRate = audioSampleRate)
        val videoConfig =
            VideoConfig(mimeType = videoCodec, resolution = Size(VIDEO_WIDTH, VIDEO_HEIGHT))

        // Run stream
        streamer.configure(
            audioConfig,
            videoConfig
        )

        // Run stream
        StreamerUtils.runStream(
            streamer,
            descriptor,
            STREAM_DURATION_MS.milliseconds,
            STREAM_POLLING_MS.milliseconds
        )
        streamer.release()

        // Check file
        val uri = descriptor.uri
        VideoUtils.verifyFile(uri.toFile())

        // Check video metadata
        if (verify) {
            VideoUtils.verify(context, uri, audioConfig, videoConfig)
        }
    }

    companion object {
        private const val STREAM_DURATION_MS = 30_000L
        private const val STREAM_POLLING_MS = 1_000L

        private const val VIDEO_WIDTH = 1280
        private const val VIDEO_HEIGHT = 720

        @JvmStatic
        @Parameterized.Parameters(
            name = "MediaDescriptor: {0} - Verify: {1}"
        )
        fun getMediaDescriptor(): Iterable<Array<Any>> {
            return arrayListOf(
                arrayOf(UriMediaDescriptor(FileUtils.createCacheFile("video.ts").toUri()), true),
                arrayOf(UriMediaDescriptor(FileUtils.createCacheFile("video.mp4").toUri()), true),
                arrayOf(UriMediaDescriptor(FileUtils.createCacheFile("video.flv").toUri()), false),
                arrayOf(UriMediaDescriptor(FileUtils.createCacheFile("video.webm").toUri()), true)
            )
        }
    }
}