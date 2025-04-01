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
package io.github.thibaultbee.streampack.core.streamers

import io.github.thibaultbee.streampack.core.interfaces.ICloseableStreamer
import io.github.thibaultbee.streampack.core.interfaces.IWithAudioSource
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoRotation
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoSource


interface IConfigurableAudioStreamer<T> {
    /**
     * Configures only audio settings.
     *
     * @param audioConfig Audio configuration to set
     *
     * @throws [Throwable] if configuration can not be applied.
     */
    suspend fun setAudioConfig(audioConfig: T)
}

interface IConfigurableVideoStreamer<T> {
    /**
     * Configures only video settings.
     *
     * @param videoConfig Video configuration to set
     *
     * @throws [Throwable] if configuration can not be applied.
     */
    suspend fun setVideoConfig(videoConfig: T)
}

interface IVideoStreamer<T> : IConfigurableVideoStreamer<T>, IWithVideoSource,
    IWithVideoRotation, ICloseableStreamer

interface IAudioStreamer<T> : IConfigurableAudioStreamer<T>, IWithAudioSource, ICloseableStreamer