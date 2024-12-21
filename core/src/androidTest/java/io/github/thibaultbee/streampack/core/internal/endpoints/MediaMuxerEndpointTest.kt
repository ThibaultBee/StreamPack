package io.github.thibaultbee.streampack.core.internal.endpoints

import android.content.Context
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.UriMediaDescriptor
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MediaMuxerEndpointTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().context
    private val mediaMuxerEndpoint = MediaMuxerEndpoint(context)

    @Test
    fun releaseMustNotThrow() {
        mediaMuxerEndpoint.release()
    }

    @Test
    fun closeMustNotThrow() = runTest {
        mediaMuxerEndpoint.close()
    }

    @Test
    fun openNotAFileTest() = runTest {
        try {
            mediaMuxerEndpoint.open(UriMediaDescriptor(Uri.parse("aaa://a/b/c/d")))
        } catch (_: Throwable) {
        }
    }

}