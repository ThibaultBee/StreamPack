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
package io.github.thibaultbee.streampack.core.elements.metrics

/**
 * Tracks [BasicEndpointMetrics] over time, providing both cumulative and instant (diff since last read) metrics.
 */
class EndpointMetricsTracker(private val metricsProvider: WithEndpointMetrics<*>) {
    private var lastMetrics: BasicEndpointMetrics? = null

    /**
     * The cumulative metrics since the start.
     */
    val cumulative: BasicEndpointMetrics
        get() = metricsProvider.metrics

    /**
     * The instant metrics (difference between the current and the previous read).
     * If no previous read exists, it equals the current metrics.
     * Avoid calling this from 2 different parts.
     */
    @get:Synchronized
    val instant: BasicEndpointMetrics
        get() {
            val current = metricsProvider.metrics
            val last = lastMetrics
            lastMetrics = current
            
            return if (last != null) {
                current - last
            } else {
                current
            }
        }

    val rawMetrics
        get() = metricsProvider.metrics.rawMetrics
}

/**
 * Creates an [EndpointMetricsTracker] for this [WithEndpointMetrics].
 */
fun WithEndpointMetrics<*>.createMetricsTracker() = EndpointMetricsTracker(this)
