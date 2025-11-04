package io.github.thibaultbee.streampack.core.streamers.single

import android.Manifest
import android.content.Context
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.cameras
import io.github.thibaultbee.streampack.core.streamer.utils.ProtocolStreamerTestHelper
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
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
    fun writeToRtmp() = runTest(timeout = ProtocolStreamerTestHelper.TEST_TIMEOUT_MS.milliseconds) {
        assumeTrue("Required API key", apiKey != null)
        assumeTrue("API key not set", apiKey != "null")

        ProtocolStreamerTestHelper.runProtocolStreamingTest(
            context = context,
            apiKey = requireNotNull(apiKey),
            protocolName = "RTMP",
            streamUrlBuilder = { streamKey -> 
                "rtmps://broadcast.api.video:1936/s/$streamKey"
            },
            tag = TAG
        )
    }

    companion object {
        private const val TAG = "RTMPStreamerTest"
    }
}