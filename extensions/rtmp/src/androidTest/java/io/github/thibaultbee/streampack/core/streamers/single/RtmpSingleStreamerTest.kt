package io.github.thibaultbee.streampack.core.streamers.single

import android.Manifest
import android.content.Context
import android.util.Log
import android.util.Size
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.cameras
import io.github.thibaultbee.streampack.core.interfaces.startStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import video.api.client.ApiVideoClient
import video.api.client.api.models.Environment
import video.api.client.api.models.LiveStreamCreationPayload
import video.api.client.api.models.VideoStatus
import kotlin.time.Duration.Companion.milliseconds

@LargeTest
class RtmpSingleStreamerTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().context
    private val arguments = InstrumentationRegistry.getArguments()
    private var apiKey: String? = null

    @get:Rule
    val runtimePermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

    @Before
    fun setUp() {
        assumeTrue(context.cameras.isNotEmpty())
        apiKey = arguments.getString("INTEGRATION_TESTS_API_KEY")
    }

    @Test
    fun writeToRtmp() = runTest(timeout = TEST_TIMEOUT_MS.milliseconds) {
        assumeTrue("Required API key", apiKey != null)
        assumeTrue("API key not set", apiKey != "null")

        // Create a live stream
        val apiClient = ApiVideoClient(requireNotNull(apiKey), Environment.SANDBOX)
        val liveStreamEndpoint = apiClient.liveStreams()
        val liveStream =
            liveStreamEndpoint.create(LiveStreamCreationPayload().name("StreamPack RTMP test"))

        try {
            // Run live stream
            val streamer = cameraSingleStreamer(context)
            streamer.setConfig(
                AudioConfig(),
                VideoConfig(startBitrate = 500_000, resolution = Size(VIDEO_WIDTH, VIDEO_HEIGHT))
            )
            streamer.startStream("rtmps://broadcast.api.video:1936/s/${liveStream.streamKey}")
            var i = 0
            val numOfLoop = LIVE_STREAM_DURATION_MS / LIVE_STREAM_POLLING_MS
            withContext(Dispatchers.Default) {
                while (i < numOfLoop) {
                    i++
                    Log.d(
                        TAG,
                        "Waiting for 1s (${i * LIVE_STREAM_POLLING_MS} ms/$LIVE_STREAM_DURATION_MS) ms"
                    )
                    delay(LIVE_STREAM_POLLING_MS)
                    val isBroadcasting =
                        liveStreamEndpoint.get(liveStream.liveStreamId).broadcasting!!
                    Log.i(TAG, "Is broadcasting $isBroadcasting")
                    assertTrue(isBroadcasting)
                }
            }
            streamer.close()
            streamer.release()

            Log.i(TAG, "Live stream is completed")

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
                    Log.i(TAG, "Video status: $videoStatus")
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
            Log.e(TAG, " RTMP test failed due to ${e.message}", e)
            throw e
        } finally {
            // Delete live
            liveStreamEndpoint.delete(liveStream.liveStreamId)
        }
    }

    companion object {
        private const val TAG = "RTMPStreamerTest"

        private const val TEST_TIMEOUT_MS = 200_000L
        private const val LIVE_STREAM_DURATION_MS = 30_000L
        private const val LIVE_STREAM_POLLING_MS = 1_000L

        private const val VIDEO_WIDTH = 640
        private const val VIDEO_HEIGHT = 360
    }
}