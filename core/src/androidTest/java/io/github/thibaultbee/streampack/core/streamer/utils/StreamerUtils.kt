package io.github.thibaultbee.streampack.core.streamer.utils

import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.streamers.single.SingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.startStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.junit.Assert.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object StreamerUtils {
    suspend fun runStream(
        streamer: SingleStreamer,
        descriptor: MediaDescriptor,
        duration: Duration,
        pollDuration: Duration = 1.seconds
    ) {
        streamer.startStream(descriptor)
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