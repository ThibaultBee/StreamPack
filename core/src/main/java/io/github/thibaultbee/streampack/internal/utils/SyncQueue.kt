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

import java.util.PriorityQueue

/**
 * A synchronized queue that allows to add elements in order. Elements are stored till a sync
 * element is added. When a sync element is added, all elements that are comparatively lower are
 * sent to the listener.
 *
 * The purpose of this class is to allow to put elements in order.
 *
 * @param E the type of elements held in this collection
 * @param comparator the comparator that will be used to order the elements
 * @param listener the listener that will be called when a sync element is added
 */
class SyncQueue<E>(
    private val comparator: Comparator<in E>,
    private val listener: Listener<E>
) {
    private val priorityQueue: PriorityQueue<E> = PriorityQueue(8, comparator)

    val size: Int
        get() = priorityQueue.size

    /**
     * Sends all elements that are comparatively lower than [element] to the listener.
     * [element] is not outputted.
     *
     * @param element the element to compare
     */
    fun syncTo(element: E) {
        var polledElement: E? = pollIf(comparator, element)
        while (polledElement != null) {
            listener.onElement(polledElement)
            polledElement = pollIf(comparator, element)
        }
    }

    /**
     * Adds an element in order.
     * If [isSync] is true, all elements that are comparatively lower than [element] are sent to the
     * listener.
     *
     * @param element the element to add
     * @param isSync true if [element] is a sync element
     */
    fun add(element: E, isSync: Boolean = false) {
        if (isSync) {
            syncTo(element)
            // Send sync element
            listener.onElement(element)
        } else {
            synchronized(this) {
                priorityQueue.add(element)
            }
        }
    }

    /**
     * Adds all elements in order.
     *
     * @param elements the elements to add
     */
    fun add(elements: List<E>) {
        synchronized(this) {
            priorityQueue.addAll(elements)
        }
    }

    private fun pollIf(comparator: Comparator<in E>, comparableElement: E): E? {
        synchronized(this) {
            val element = priorityQueue.peek()
            if ((element != null) && comparator.compare(element, comparableElement) <= 0) {
                return priorityQueue.poll()
            }
            return null
        }
    }

    fun clear() {
        synchronized(this) {
            priorityQueue.clear()
        }
    }

    interface Listener<E> {
        /**
         * Called when element is polled.
         */
        fun onElement(element: E)
    }
}