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
package io.github.thibaultbee.streampack.core.elements.utils

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelWithCloseableDataTest {
    private val dropped = mutableListOf<Int>()
    private val closed = mutableListOf<Int>()

    /** Elements are their own size in bytes. */
    private fun budgeted(maxBytes: Long) = ChannelWithCloseableData<Int>(
        Channel.UNLIMITED,
        onUndeliveredElement = { dropped += it },
        sizeOf = { it }
    ).apply { this.maxBytes = maxBytes }

    private suspend fun ChannelWithCloseableData<Int>.sendAll(vararg sizes: Int) =
        sizes.forEach { size -> send(size) { closed += size } }

    private fun ChannelWithCloseableData<Int>.drain() =
        generateSequence { tryReceive().getOrNull()?.use { it.data } }.toList()

    @Test
    fun `many small elements fit within the byte budget`() = runTest {
        val channel = budgeted(1_000)
        channel.sendAll(*IntArray(100) { 10 })

        assertEquals(emptyList<Int>(), dropped)
        assertEquals(100, channel.drain().size)
    }

    @Test
    fun `oldest elements are dropped and closed while over the budget`() = runTest {
        val channel = budgeted(100)
        channel.sendAll(40, 41, 42, 43)

        assertEquals(listOf(40, 41), dropped)
        assertEquals(listOf(40, 41), closed)
        assertEquals(listOf(42, 43), channel.drain())
    }

    @Test
    fun `an element larger than the budget is kept beside the ones before it`() = runTest {
        val channel = budgeted(100)
        channel.sendAll(10, 20, 500)

        assertEquals(emptyList<Int>(), dropped)
        assertEquals(listOf(10, 20, 500), channel.drain())
    }

    @Test
    fun `a smaller later element does not drop a queued element larger than the budget`() = runTest {
        val channel = budgeted(100)
        channel.sendAll(500, 30, 40)

        assertEquals(emptyList<Int>(), dropped)
        assertEquals(listOf(500, 30, 40), channel.drain())
    }

    @Test
    fun `an element larger than the budget is dropped as the oldest once the rest exceed it`() = runTest {
        val channel = budgeted(100)
        channel.sendAll(500, 60, 60)

        assertEquals(listOf(500, 60), dropped)
        assertEquals(listOf(60), channel.drain())
    }

    @Test
    fun `a received element no longer counts`() = runTest {
        val channel = budgeted(100)
        channel.sendAll(90)
        val inFlight = channel.receive()
        channel.sendAll(90)
        assertEquals(emptyList<Int>(), dropped)
        val iterated = channel.iterator().also { it.hasNext() }.next()
        channel.sendAll(90)

        assertEquals(emptyList<Int>(), dropped)
        inFlight.close()
        iterated.close()
        assertEquals(listOf(90), channel.drain())
    }

    @Test
    fun `cleared and dropped elements no longer count`() = runTest {
        val channel = budgeted(100)
        channel.sendAll(60, 60)
        channel.clear()
        channel.sendAll(90)

        assertEquals(listOf(60), dropped)
        assertEquals(listOf(90), channel.drain())
    }

    @Test
    fun `without sizeOf the element capacity still drops the oldest`() = runTest {
        val channel = ChannelWithCloseableData<Int>(
            2,
            BufferOverflow.DROP_OLDEST,
            onUndeliveredElement = { dropped += it })
        channel.sendAll(1, 2, 3)

        assertEquals(listOf(1), dropped)
        assertEquals(listOf(2, 3), channel.drain())
    }
}
