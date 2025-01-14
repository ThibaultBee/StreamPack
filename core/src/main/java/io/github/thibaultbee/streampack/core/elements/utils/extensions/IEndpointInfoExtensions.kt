/*
 * Copyright (C) 2024 Thibault B.
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
package io.github.thibaultbee.streampack.core.elements.utils.extensions

import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpoint

/**
 * Returns the union of two [IEndpoint.IEndpointInfo]
 */
internal infix fun IEndpoint.IEndpointInfo.intersect(other: IEndpoint.IEndpointInfo): IEndpoint.IEndpointInfo {
    return object : IEndpoint.IEndpointInfo {
        override val audio = object : IEndpoint.IEndpointInfo.IAudioEndpointInfo {
            override val supportedEncoders: List<String>
                get() = this@intersect.audio.supportedEncoders.intersect(other.audio.supportedEncoders.toSet())
                    .toList()

            override val supportedSampleRates: List<Int>?
                get() {
                    val supportedSampleRates = this@intersect.audio.supportedSampleRates
                    val otherSupportedSampleRates = other.audio.supportedSampleRates
                    return if (supportedSampleRates == null) {
                        otherSupportedSampleRates
                    } else if (otherSupportedSampleRates == null) {
                        supportedSampleRates
                    } else {
                        supportedSampleRates.intersect(otherSupportedSampleRates).toList()
                    }
                }

            override val supportedByteFormats: List<Int>?
                get() {
                    val supportedByteFormats = this@intersect.audio.supportedByteFormats
                    val otherSupportedByteFormats = other.audio.supportedByteFormats
                    return if (supportedByteFormats == null) {
                        otherSupportedByteFormats
                    } else if (otherSupportedByteFormats == null) {
                        supportedByteFormats
                    } else {
                        supportedByteFormats.intersect(otherSupportedByteFormats).toList()
                    }
                }
        }
        override val video = object : IEndpoint.IEndpointInfo.IVideoEndpointInfo {
            override val supportedEncoders: List<String> =
                this@intersect.video.supportedEncoders.intersect(other.video.supportedEncoders.toSet())
                    .toList()
        }
    }
}