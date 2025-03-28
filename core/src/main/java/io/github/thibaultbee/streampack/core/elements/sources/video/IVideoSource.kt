/*
 * Copyright (C) 2022 Thibault B.
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
package io.github.thibaultbee.streampack.core.elements.sources.video

import android.content.Context
import io.github.thibaultbee.streampack.core.elements.interfaces.Releasable
import io.github.thibaultbee.streampack.core.elements.interfaces.SuspendConfigurable
import io.github.thibaultbee.streampack.core.elements.interfaces.SuspendStreamable
import io.github.thibaultbee.streampack.core.elements.processing.video.source.ISourceInfoProvider
import kotlinx.coroutines.flow.StateFlow

/**
 * The public interface for video sources.
 */
interface IVideoSource

/**
 * The internal interface for video sources.
 */
interface IVideoSourceInternal : IVideoSource,
    SuspendStreamable, SuspendConfigurable<VideoSourceConfig>, Releasable {
    /**
     * Orientation provider of the capture source.
     * It is used to orientate the frame according to the source orientation.
     */
    val infoProviderFlow: StateFlow<ISourceInfoProvider>

    /**
     * Flow of the last streaming state.
     */
    val isStreamingFlow: StateFlow<Boolean>

    /**
     * A factory to build an [IVideoSourceInternal].
     */
    interface Factory {
        /**
         * Creates an [IVideoSourceInternal] instance.
         *
         * @param context the application context
         * @return an [IVideoSourceInternal]
         */
        suspend fun create(context: Context): IVideoSourceInternal

        /**
         * Whether the source that will be created by [create] is equal to another source.
         */
        fun isSourceEquals(source: IVideoSourceInternal?): Boolean
    }
}

