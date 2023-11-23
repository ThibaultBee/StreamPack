/*
 * Copyright (C) 2023 Thibault B.
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
package io.github.thibaultbee.streampack.internal.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncQueueTest {
    @Test
    fun `test sync 1 element`() {
        val syncQueue =
            SyncQueue({ o1, o2 -> o1.compareTo(o2) }, object : SyncQueue.Listener<Int> {
                override fun onElement(element: Int) {
                    assertEquals(1, element)
                }
            })
        syncQueue.add(1, true)
        assertEquals(0, syncQueue.size)
    }

    @Test
    fun `test already sorted elements`() {
        var i = 1
        val syncQueue =
            SyncQueue({ o1, o2 -> o1.compareTo(o2) }, object : SyncQueue.Listener<Int> {
                override fun onElement(element: Int) {
                    assertEquals(i++, element)
                }
            })
        syncQueue.add(1)
        syncQueue.add(2)
        syncQueue.add(3)
        syncQueue.add(4, isSync = true)

        assertEquals(5, i)
        assertEquals(0, syncQueue.size)
    }

    @Test
    fun `test equals elements`() {
        val syncQueue =
            SyncQueue({ o1, o2 -> o1.compareTo(o2) }, object : SyncQueue.Listener<Int> {
                override fun onElement(element: Int) {
                    assertEquals(1, element)
                }
            })
        syncQueue.add(1)
        syncQueue.add(1, isSync = true)

        assertEquals(0, syncQueue.size)
    }

    @Test
    fun `test not sorted elements`() {
        var i = 1
        val syncQueue =
            SyncQueue({ o1, o2 -> o1.compareTo(o2) }, object : SyncQueue.Listener<Int> {
                override fun onElement(element: Int) {
                    assertEquals(i++, element)
                }
            })
        syncQueue.add(1)
        syncQueue.add(2)
        syncQueue.add(3)
        syncQueue.add(5)
        syncQueue.add(6)
        syncQueue.add(4, isSync = true)

        assertEquals(5, i)
        assertEquals(2, syncQueue.size)
    }
}