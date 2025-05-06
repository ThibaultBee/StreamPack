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
package io.github.thibaultbee.streampack.services.utils

import android.content.Context
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.elements.utils.extensions.displayRotation
import io.github.thibaultbee.streampack.core.interfaces.IStreamer
import io.github.thibaultbee.streampack.core.streamers.dual.DualStreamer
import io.github.thibaultbee.streampack.core.streamers.dual.IDualStreamer
import io.github.thibaultbee.streampack.core.streamers.single.ISingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.SingleStreamer
import io.github.thibaultbee.streampack.services.StreamerService


/**
 * A streamer factory to pass as a parameter of a [StreamerService].
 */
interface StreamerFactory<T : IStreamer> {
    /**
     * Creates a streamer.
     *
     * @param context the application context
     * @return the streamer
     */
    fun create(context: Context): T
}

/**
 * The streamer factory to create a streamer with a single output: [ISingleStreamer].
 *
 * @param withAudio true if the streamer have an audio source
 * @param withVideo true if the streamer have a video source
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 */
open class SingleStreamerFactory(
    private val withAudio: Boolean,
    private val withVideo: Boolean,
    @RotationValue private val defaultRotation: Int? = null
) :
    StreamerFactory<ISingleStreamer> {
    override fun create(context: Context): ISingleStreamer {
        return SingleStreamer(
            context,
            withAudio,
            withVideo,
            defaultRotation = defaultRotation ?: context.displayRotation
        )
    }
}

/**
 * The streamer factory to create a streamer with a 2 outputs: [IDualStreamer].
 *
 * @param withAudio true if the streamer have an audio source
 * @param withVideo true if the streamer have a video source
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 */
open class DualStreamerFactory(
    private val withAudio: Boolean,
    private val withVideo: Boolean,
    @RotationValue private val defaultRotation: Int? = null
) :
    StreamerFactory<IDualStreamer> {
    override fun create(context: Context): IDualStreamer {
        return DualStreamer(
            context,
            withAudio,
            withVideo,
            defaultRotation = defaultRotation ?: context.displayRotation
        )
    }
}