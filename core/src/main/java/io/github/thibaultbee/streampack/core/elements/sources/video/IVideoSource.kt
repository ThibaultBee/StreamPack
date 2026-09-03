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
import io.github.thibaultbee.streampack.core.elements.interfaces.SuspendConfigurable
import io.github.thibaultbee.streampack.core.elements.interfaces.SuspendReleasable
import io.github.thibaultbee.streampack.core.elements.interfaces.SuspendStreamable
import io.github.thibaultbee.streampack.core.elements.processing.video.source.ISourceInfoProvider
import io.github.thibaultbee.streampack.core.pipelines.IVideoDispatcherProvider
import kotlinx.coroutines.flow.StateFlow
import io.github.thibaultbee.streampack.core.utils.InternalStreamPackApi

/**
 * The public interface for video sources.
 */
interface IVideoSource {
    /**
     * A factory to build an [IVideoSource].
     */
    interface Factory {
        /**
         * Creates an [IVideoSourceInternal] instance.
         *
         * @param context the application context
         * @return an [IVideoSourceInternal]
         */
        @InternalStreamPackApi
        suspend fun create(
            context: Context,
            dispatcherProvider: IVideoDispatcherProvider
        ): IVideoSourceInternal

        /**
         * Whether the source that will be created by [create] is equal to another source.
         */
        @InternalStreamPackApi
        fun isSourceEquals(source: IVideoSourceInternal?): Boolean
    }
}

/**
 * The internal interface for video sources.
 */
@InternalStreamPackApi
interface IVideoSourceInternal : IVideoSource,
    SuspendStreamable, SuspendConfigurable<VideoSourceConfig>, SuspendReleasable {
    /**
     * Orientation provider of the capture source.
     * It is used to orientate the frame according to the source orientation.
     */
    val infoProviderFlow: StateFlow<ISourceInfoProvider>

    /**
     * Flow of the last streaming state.
     */
    val isStreamingFlow: StateFlow<Boolean>
}

