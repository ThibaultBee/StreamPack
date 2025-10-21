package io.github.thibaultbee.streampack.core.elements.endpoints

import androidx.test.platform.app.InstrumentationRegistry
import io.github.thibaultbee.streampack.ext.flv.elements.endpoints.FlvFileEndpoint
import kotlinx.coroutines.Dispatchers
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
            val defaultDispatcher = Dispatchers.Default
            val ioDispatcher = Dispatchers.IO

            return arrayListOf(
                DynamicEndpoint(context, defaultDispatcher, ioDispatcher),
                MediaMuxerEndpoint(context, ioDispatcher),
                FlvFileEndpoint(defaultDispatcher, ioDispatcher)
            )
        }
    }
}