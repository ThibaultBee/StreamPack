/*
 * Copyright (C) 2026 Thibault B.
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
package io.github.thibaultbee.streampack.core.elements.encoders.mediacodec

import io.github.thibaultbee.streampack.core.elements.data.Frame
import io.github.thibaultbee.streampack.core.elements.data.RawFrame
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.EncoderMode
import io.github.thibaultbee.streampack.core.elements.encoders.IEncoderInternal
import io.github.thibaultbee.streampack.core.logger.ILogger
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.utils.InternalStreamPackApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.math.min

/**
 * An [EncoderMode.ASYNC] audio encoder whose frame request blocks its thread, as an
 * `AudioRecord` read does.
 */
@OptIn(InternalStreamPackApi::class)
class MediaCodecEncoderAsyncInputTest {
    private val audioConfig = AudioCodecConfig()
    private val bytesPerFrame = SAMPLES_PER_FRAME *
            AudioCodecConfig.getNumberOfChannels(audioConfig.channelConfig) *
            AudioCodecConfig.getNumOfBytesPerSample(audioConfig.byteFormat)
    private val frameDurationUs = SAMPLES_PER_FRAME * 1_000_000L / audioConfig.sampleRate

    private val processDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val inputDispatcher = HoldingDispatcher()
    private val consumerScope = CoroutineScope(Dispatchers.Default)

    private val outputs = AtomicInteger()
    private val errors = CopyOnWriteArrayList<String>()
    private val outputChannel = Channel<Frame>(Channel.UNLIMITED)
    private val fill = GatedFill()

    private val previousLogger = Logger.logger
    private lateinit var encoder: MediaCodecEncoder

    @Before
    fun setUp() {
        Logger.logger = object : ILogger by previousLogger {
            override fun e(tag: String, message: String, tr: Throwable?) {
                if (tag.startsWith("AudioEncoder")) {
                    errors.add(message)
                }
                previousLogger.e(tag, message, tr)
            }
        }
        consumerScope.launch {
            for (frame in outputChannel) {
                frame.close()
            }
        }
        encoder = MediaCodecEncoder(
            AudioEncoderConfig(audioConfig, EncoderMode.ASYNC),
            object : IEncoderInternal.IListener {
                // Counts at send, which the encoder does under its mutex.
                override val outputChannel =
                    object : SendChannel<Frame> by this@MediaCodecEncoderAsyncInputTest.outputChannel {
                        override suspend fun send(element: Frame) {
                            outputs.incrementAndGet()
                            this@MediaCodecEncoderAsyncInputTest.outputChannel.send(element)
                        }
                    }
                override fun onError(t: Throwable) {
                    errors.add("onError: $t")
                }
            },
            Dispatchers.Default,
            processDispatcher,
            inputDispatcher
        )
        (encoder.input as IEncoderInternal.IAsyncByteBufferInput).listener = fill
    }

    @After
    fun tearDown() {
        fill.openGate()
        runBlocking {
            withTimeout(TIMEOUT_MS) { encoder.release() }
        }
        inputDispatcher.close()
        processDispatcher.close()
        consumerScope.cancel()
        Logger.logger = previousLogger
    }

    @Test
    fun outputIsDeliveredWhileAFrameRequestIsBlocked() = runBlocking {
        withTimeout(TIMEOUT_MS) {
            encoder.configure()
            encoder.startStream()
            awaitOutputs(WARM_UP_OUTPUTS)

            fill.blockNextRequest(unpaced = UNPACED_REQUESTS)
            val atBlock = outputs.get()
            delay(BLOCK_MS)
            val duringBlock = outputs.get() - atBlock
            fill.openGate()

            assertTrue(
                "no output was delivered while a frame request was blocked",
                duringBlock > 0
            )
            encoder.stopStream()
            assertEquals("encoder errors", emptyList<String>(), errors)
        }
    }

    @Test
    fun stopStreamWaitsForTheInFlightRequestAndDropsItsFrame() = runBlocking {
        withTimeout(TIMEOUT_MS) {
            encoder.configure()
            encoder.startStream()
            awaitOutputs(WARM_UP_OUTPUTS)

            val heldFrame = fill.blockNextRequest()
            val stop = async(Dispatchers.Default) { encoder.stopStream() }
            delay(BLOCK_MS)
            assertFalse(
                "stopStream returned while a frame request was in flight",
                stop.isCompleted
            )

            fill.openGate()
            stop.await()
            assertTrue("the held frame was not closed when stopStream returned", heldFrame.isClosed)
            assertEquals("encoder errors", emptyList<String>(), errors)
        }
    }

    @Test
    fun restartDropsThePreviousSessionRequests() = runBlocking {
        withTimeout(TIMEOUT_MS) {
            encoder.configure()
            encoder.startStream()
            awaitOutputs(WARM_UP_OUTPUTS)

            fill.blockNextRequest()
            // Requests the codec issues from now on stay unstarted until after the restart.
            inputDispatcher.holdNewCoroutines()
            val stop = async(Dispatchers.Default) { encoder.stopStream() }
            delay(BLOCK_MS)
            fill.openGate()
            stop.await()
            delay(SETTLE_MS)
            inputDispatcher.stopHolding()
            val heldRequests = inputDispatcher.heldCount
            assertTrue("no request was held across the restart", heldRequests > 0)

            encoder.reset()
            encoder.startStream()
            delay(BLOCK_MS)
            val requestsBeforeRelease = fill.requests.get()
            val outputsBeforeRelease = outputs.get()
            inputDispatcher.releaseHeld()
            delay(RESTART_RUN_MS)

            assertEquals(
                "requests of the previous session were filled after the restart",
                0,
                fill.previousSessionRequests.get()
            )
            assertEquals("encoder errors", emptyList<String>(), errors)
            assertTrue(
                "the new session stopped requesting input",
                fill.requests.get() - requestsBeforeRelease >= RESTART_MIN_FRAMES
            )
            assertTrue(
                "the new session stopped producing output",
                outputs.get() - outputsBeforeRelease >= RESTART_MIN_FRAMES
            )
            encoder.stopStream()
        }
    }

    @Test
    fun failingRequestIsRetriedOncePerFrame() = runBlocking {
        withTimeout(TIMEOUT_MS) {
            encoder.configure()
            encoder.startStream()
            awaitOutputs(WARM_UP_OUTPUTS)

            fill.failing = true
            delay(BLOCK_MS)
            val requestsBefore = fill.requests.get()
            delay(FAILING_RUN_MS)
            val failedRequests = fill.requests.get() - requestsBefore
            fill.failing = false

            val maxRequests = 2 * FAILING_RUN_MS * 1000 / frameDurationUs
            assertTrue(
                "a failing frame request was retried $failedRequests times in $FAILING_RUN_MS ms",
                failedRequests <= maxRequests
            )
            val outputsBefore = outputs.get()
            delay(FAILING_RUN_MS)
            assertTrue(
                "no output after the input recovered",
                outputs.get() - outputsBefore >= RESTART_MIN_FRAMES
            )
            encoder.stopStream()
        }
    }

    private suspend fun awaitOutputs(count: Int) {
        while (outputs.get() < count) {
            delay(10)
        }
    }

    private class TestRawFrame(
        override val rawBuffer: ByteBuffer,
        override val timestampInUs: Long
    ) : RawFrame {
        @Volatile
        var isClosed = false
            private set

        override fun close() {
            isClosed = true
        }
    }

    private inner class GatedFill :
        IEncoderInternal.IAsyncByteBufferInput.OnFrameRequestedListener {
        val requests = AtomicInteger()
        val previousSessionRequests = AtomicInteger()
        private var timestampInUs = 0L

        @Volatile
        var failing = false

        @Volatile
        private var armed = false
        private var gate: CountDownLatch? = null
        private var blocked = CountDownLatch(1)

        @Volatile
        private var heldFrame: TestRawFrame? = null

        @Volatile
        private var unpacedRequests = 0

        /**
         * Returns the next [unpaced] requests at once, so the codec holds unencoded input, then
         * blocks the following one until [openGate]. Returns its frame once it is blocked.
         */
        fun blockNextRequest(unpaced: Int = 0): TestRawFrame {
            blocked = CountDownLatch(1)
            gate = CountDownLatch(1)
            unpacedRequests = unpaced
            armed = true
            assertTrue("no frame was requested", blocked.await(TIMEOUT_MS, TimeUnit.MILLISECONDS))
            return heldFrame!!
        }

        fun openGate() {
            gate?.countDown()
        }

        override suspend fun onFrameRequested(buffer: ByteBuffer): RawFrame {
            if (inputDispatcher.isHeld(currentCoroutineContext()[Job])) {
                previousSessionRequests.incrementAndGet()
            }
            requests.incrementAndGet()
            if (failing) {
                throw IllegalStateException("Audio source is not recording")
            }
            buffer.limit(min(buffer.limit(), buffer.position() + bytesPerFrame))
            val frame = TestRawFrame(buffer, timestampInUs)
            timestampInUs += frameDurationUs

            if (armed && unpacedRequests > 0) {
                unpacedRequests--
            } else if (armed) {
                armed = false
                heldFrame = frame
                blocked.countDown()
                gate!!.await()
            } else {
                Thread.sleep(frameDurationUs / 1000)
            }
            return frame
        }
    }

    /**
     * A single-thread dispatcher that can hold coroutines that have not started running, including
     * those already queued. Resumptions of a started coroutine are never held, so a lock it owns is
     * always released.
     */
    private class HoldingDispatcher : CoroutineDispatcher(), AutoCloseable {
        private val executor = Executors.newSingleThreadExecutor()
        private val started = Collections.newSetFromMap(IdentityHashMap<Job, Boolean>())
        private val heldJobs = Collections.newSetFromMap(IdentityHashMap<Job, Boolean>())
        private val held = mutableListOf<Runnable>()
        private var holding = false

        val heldCount: Int
            get() = synchronized(this) { held.size }

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            val job = context[Job]
            executor.execute(object : Runnable {
                override fun run() {
                    val shouldRun = synchronized(this@HoldingDispatcher) {
                        if (holding && job != null && job !in started) {
                            heldJobs.add(job)
                            held.add(this)
                            false
                        } else {
                            job?.let { started.add(it) }
                            true
                        }
                    }
                    if (shouldRun) {
                        block.run()
                    }
                }
            })
        }

        fun holdNewCoroutines() = synchronized(this) { holding = true }

        fun stopHolding() = synchronized(this) { holding = false }

        fun releaseHeld() {
            val blocks = synchronized(this) { held.toList().also { held.clear() } }
            blocks.forEach(executor::execute)
        }

        fun isHeld(job: Job?) = synchronized(this) { job != null && job in heldJobs }

        override fun close() {
            stopHolding()
            releaseHeld()
            executor.shutdown()
        }
    }

    companion object {
        private const val SAMPLES_PER_FRAME = 1024
        private const val WARM_UP_OUTPUTS = 20
        private const val BLOCK_MS = 300L
        private const val SETTLE_MS = 50L
        private const val UNPACED_REQUESTS = 3
        private const val RESTART_RUN_MS = 1_000L
        private const val FAILING_RUN_MS = 1_000L
        private const val RESTART_MIN_FRAMES = 10
        private const val TIMEOUT_MS = 10_000L
    }
}
