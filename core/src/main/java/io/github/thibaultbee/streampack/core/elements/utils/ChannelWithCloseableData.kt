/*
 * Copyright (C) 2025 Thibault B.
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
package io.github.thibaultbee.streampack.core.elements.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import java.io.Closeable

/**
 * A channel that sends and receives data along with a close action to be executed when the data is no longer needed.
 *
 * @param T The type of data to be sent and received.
 * @param capacity The capacity of the channel.
 * @param onBufferOverflow The behavior when the buffer overflows.
 */
class ChannelWithCloseableData<T>(
    capacity: Int = RENDEZVOUS,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND
) : ReceiveChannel<ChannelWithCloseableData.CloseableData<T>> {
    private val channel =
        Channel<CloseableData<T>>(capacity, onBufferOverflow, onUndeliveredElement = { it.close() })

    /**
     * Sends data along with a close action to the channel.
     *
     * @param data The data to be sent.
     * @param onClose The action to be executed when the data is no longer needed.
     */
    suspend fun send(data: T, onClose: (() -> Unit) = {}) {
        channel.send(CloseableData(data, onClose))
    }

    @DelicateCoroutinesApi
    override val isClosedForReceive: Boolean
        get() = channel.isClosedForReceive

    @ExperimentalCoroutinesApi
    override val isEmpty: Boolean
        get() = channel.isEmpty
    override val onReceive = channel.onReceive
    override val onReceiveCatching = channel.onReceiveCatching

    /**
     * Receives data along with its close action from the channel.
     *
     * @return The received data along with its close action.
     */
    override suspend fun receive() = channel.receive()

    override suspend fun receiveCatching() = channel.receiveCatching()

    override fun tryReceive() = channel.tryReceive()

    override fun iterator() = channel.iterator()

    override fun cancel(cause: CancellationException?) =
        channel.cancel(cause)

    @Deprecated(
        "Since 1.2.0, binary compatibility with versions <= 1.1.x",
        level = DeprecationLevel.HIDDEN
    )
    override fun cancel(cause: Throwable?): Boolean {
        TODO("Not yet implemented")
    }

    class CloseableData<T>(
        val data: T,
        val onClose: () -> Unit
    ) : Closeable {
        override fun close() {
            onClose()
        }
    }
}

/**
 * Receives data from the channel and uses it in a [block], ensuring that the data is properly closed after use.
 *
 * @param block The block of code to execute with the received data.
 * @return The result of the block execution.
 */
suspend inline fun <T, R> ChannelWithCloseableData<T>.useReceive(block: (T) -> R): R {
    return receive().use {
        block(it.data)
    }
}

suspend inline fun <T> ChannelWithCloseableData<T>.useConsumeEach(action: (T) -> Unit): Unit =
    consumeEach { closeableData ->
        closeableData.use {
            action(it.data)
        }
    }
