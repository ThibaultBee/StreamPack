package io.github.thibaultbee.streampack.core.streamer.utils

import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.interfaces.ICloseableStreamer
import io.github.thibaultbee.streampack.core.interfaces.startStream
import io.github.thibaultbee.streampack.core.streamers.dual.DualStreamer
import io.github.thibaultbee.streampack.core.streamers.single.SingleStreamer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.junit.Assert.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object StreamerUtils {
    suspend fun runSingleStream(
        streamer: SingleStreamer,
        descriptor: MediaDescriptor,
        duration: Duration,
        pollDuration: Duration = 1.seconds
    ) {
        streamer.startStream(descriptor)

        runStream(streamer, duration, pollDuration)
    }

    suspend fun runDualStream(
        streamer: DualStreamer,
        firstDescriptor: MediaDescriptor,
        secondDescriptor: MediaDescriptor,
        duration: Duration,
        pollDuration: Duration = 1.seconds
    ) {
        streamer.first.startStream(firstDescriptor)
        streamer.second.startStream(secondDescriptor)

        runStream(streamer, duration, pollDuration)
    }

    private suspend fun runStream(
        streamer: ICloseableStreamer,
        duration: Duration,
        pollDuration: Duration = 1.seconds
    ) {
        var i = 0
        val numOfLoop = duration / pollDuration
        withContext(Dispatchers.Default) {
            while (i < numOfLoop) {
                i++
                delay(pollDuration)
                assertTrue(streamer.isStreamingFlow.value)
            }
        }

        streamer.stopStream()
        streamer.close()
    }
}