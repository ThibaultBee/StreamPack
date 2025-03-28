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
package io.github.thibaultbee.streampack.core.elements.sources.audio

import android.content.Context
import io.github.thibaultbee.streampack.core.elements.interfaces.Releasable
import io.github.thibaultbee.streampack.core.elements.interfaces.SuspendConfigurable
import io.github.thibaultbee.streampack.core.elements.interfaces.SuspendStreamable
import kotlinx.coroutines.flow.StateFlow

/**
 * The public interface for audio sources.
 */
interface IAudioSource

/**
 * The internal interface for audio sources.
 *
 * This interface extends [IAudioSource] and adds additional functionality for streaming and configuration.
 */
interface IAudioSourceInternal : IAudioSource, IAudioFrameSourceInternal, SuspendStreamable,
    SuspendConfigurable<AudioSourceConfig>, Releasable {
    /**
     * Flow of the last streaming state.
     */
    val isStreamingFlow: StateFlow<Boolean>

    /**
     * A factory to build an [IAudioSourceInternal].
     */
    interface Factory {
        /**
         * Creates an [IAudioSourceInternal] instance.
         *
         * @return an [IAudioSourceInternal]
         */
        suspend fun create(context: Context): IAudioSourceInternal

        /**
         * Whether the source that will be created by [create] is equal to another source.
         */
        fun isSourceEquals(source: IAudioSourceInternal?): Boolean
    }
}
