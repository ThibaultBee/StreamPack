package io.github.thibaultbee.streampack.core.elements.endpoints.composites.sinks

import androidx.test.platform.app.InstrumentationRegistry
import io.github.thibaultbee.streampack.ext.rtmp.elements.endpoints.composites.sinks.RtmpSink
import io.github.thibaultbee.streampack.ext.srt.elements.endpoints.composites.sinks.SrtSink
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Test class for [ISink] state.
 */
@RunWith(Parameterized::class)
class SinkStateTest(private val endpoint: ISinkInternal) {
    @Test
    fun closeTest() = runTest {
        endpoint.close()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(
            name = "Sink: {0}"
        )
        fun getMediaDescriptor(): Iterable<ISinkInternal> {
            val context = InstrumentationRegistry.getInstrumentation().context

            return arrayListOf(
                FileSink(),
                ContentSink(context),
                ChunkedFileOutputStreamSink(1000),
                FakeSink(),
                SrtSink(),
                RtmpSink()
            )
        }
    }
}