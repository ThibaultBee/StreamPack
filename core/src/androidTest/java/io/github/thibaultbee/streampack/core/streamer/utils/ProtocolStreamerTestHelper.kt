/*
 * Copyright (C) 2025 Thibault B.
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
package io.github.thibaultbee.streampack.core.streamer.utils

import android.content.Context
import android.util.Log
import android.util.Size
import io.github.thibaultbee.streampack.core.streamers.single.AudioConfig
import io.github.thibaultbee.streampack.core.streamers.single.VideoConfig
import io.github.thibaultbee.streampack.core.streamers.single.cameraSingleStreamer
import io.github.thibaultbee.streampack.core.interfaces.startStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import video.api.client.ApiVideoClient
import video.api.client.api.models.Environment
import video.api.client.api.models.LiveStreamCreationPayload
import video.api.client.api.models.VideoStatus

/**
 * Helper object for protocol-based streaming integration tests.
 * Provides common functionality for RTMP, SRT, and other protocol tests.
 */
object ProtocolStreamerTestHelper {
    const val TEST_TIMEOUT_MS = 200_000L
    const val LIVE_STREAM_DURATION_MS = 30_000L
    const val LIVE_STREAM_POLLING_MS = 1_000L
    const val VIDEO_WIDTH = 640
    const val VIDEO_HEIGHT = 360

    /**
     * Runs a complete protocol streaming test.
     * 
     * @param context The Android context
     * @param apiKey The api.video API key
     * @param protocolName The protocol name for logging (e.g., "RTMP", "SRT")
     * @param streamUrlBuilder Function to build the stream URL from the stream key
     * @param tag Log tag for this test
     */
    suspend fun runProtocolStreamingTest(
        context: Context,
        apiKey: String,
        protocolName: String,
        streamUrlBuilder: (streamKey: String) -> String,
        tag: String
    ) {
        // Create a live stream
        val apiClient = ApiVideoClient(apiKey, Environment.SANDBOX)
        val liveStreamEndpoint = apiClient.liveStreams()
        val liveStream = liveStreamEndpoint.create(
            LiveStreamCreationPayload().name("StreamPack $protocolName test")
        )

        try {
            // Run live stream
            val streamer = cameraSingleStreamer(context)
            streamer.setConfig(
                AudioConfig(),
                VideoConfig(startBitrate = 500_000, resolution = Size(VIDEO_WIDTH, VIDEO_HEIGHT))
            )
            streamer.startStream(streamUrlBuilder(liveStream.streamKey))
            
            // Wait for stream to be broadcasting
            var i = 0
            val numOfLoop = LIVE_STREAM_DURATION_MS / LIVE_STREAM_POLLING_MS
            withContext(Dispatchers.Default) {
                while (i < numOfLoop) {
                    i++
                    Log.d(
                        tag,
                        "Waiting for 1s (${i * LIVE_STREAM_POLLING_MS} ms/$LIVE_STREAM_DURATION_MS) ms"
                    )
                    delay(LIVE_STREAM_POLLING_MS)
                    val isBroadcasting =
                        liveStreamEndpoint.get(liveStream.liveStreamId).broadcasting!!
                    Log.i(tag, "Is broadcasting $isBroadcasting")
                    assertTrue(isBroadcasting)
                }
            }
            streamer.close()
            streamer.release()

            Log.i(tag, "Live stream is completed")

            // Assert video is available
            // Wait for video to be available
            withContext(Dispatchers.Default) {
                delay(10000)
            }
            val videoEndpoint = apiClient.videos()
            val videoPages = videoEndpoint.list().liveStreamId(liveStream.liveStreamId).execute()

            val videos = videoPages.items
            assertEquals(1, videos.size)
            val video = videos.first()
            val status = withContext(Dispatchers.Default) {
                while (true) {
                    val videoStatus = videoEndpoint.getStatus(video.videoId)
                    Log.i(tag, "Video status: $videoStatus")
                    if (videoStatus.encoding?.playable == true) {
                        return@withContext videoStatus
                    }
                    delay(1000)
                }
            }
            status as VideoStatus

            // Verify status
            assertEquals(VIDEO_WIDTH, status.encoding!!.metadata!!.height)
            assertEquals(VIDEO_HEIGHT, status.encoding!!.metadata!!.width)
            assertEquals(44100, status.encoding!!.metadata!!.samplerate)
            assertTrue(status.encoding!!.metadata!!.framerate!!.toInt() >= 0)
            assertTrue(status.encoding!!.metadata!!.bitrate!!.toInt() >= 0)
            assertTrue(status.encoding!!.metadata!!.duration!!.toInt() >= 0)
        } catch (e: Exception) {
            Log.e(tag, "$protocolName test failed due to ${e.message}", e)
            throw e
        } finally {
            // Delete live
            liveStreamEndpoint.delete(liveStream.liveStreamId)
        }
    }
}
