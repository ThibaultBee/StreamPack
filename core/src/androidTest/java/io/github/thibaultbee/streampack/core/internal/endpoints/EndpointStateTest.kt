package io.github.thibaultbee.streampack.core.internal.endpoints

import androidx.test.platform.app.InstrumentationRegistry
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.CompositeEndpoint
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.flv.FlvMuxer
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.sinks.FileSink
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Test class for [IEndpoint] state.
 */
@RunWith(Parameterized::class)
class EndpointStateTest(private val endpoint: IEndpointInternal) {
    @Test
    fun closeTest() = runTest {
        endpoint.close()
    }

    @Test
    fun releaseTest() {
        endpoint.release()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(
            name = "Endpoint: {0}"
        )
        fun getMediaDescriptor(): Iterable<IEndpointInternal> {
            val context = InstrumentationRegistry.getInstrumentation().context

            return arrayListOf(
                DynamicEndpoint(context),
                MediaMuxerEndpoint(context),
                CompositeEndpoint(FlvMuxer(isForFile = false), FileSink())
            )
        }
    }
}