package io.github.thibaultbee.streampack.core.streamer.file

import io.github.thibaultbee.streampack.core.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.streamers.DefaultStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.startStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.junit.Assert.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object StreamerUtils {
    suspend fun runStream(
        streamer: DefaultStreamer,
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
                assertTrue(streamer.isStreaming.value)
            }
        }
        streamer.stopStream()
        streamer.close()
    }
}