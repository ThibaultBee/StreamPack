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
package io.github.thibaultbee.streampack.core.internal.sources.audio

import io.github.thibaultbee.streampack.core.data.AudioConfig
import io.github.thibaultbee.streampack.core.internal.interfaces.Configurable
import io.github.thibaultbee.streampack.core.internal.interfaces.Releaseable
import io.github.thibaultbee.streampack.core.internal.interfaces.Streamable
import io.github.thibaultbee.streampack.core.internal.sources.IFrameSource

interface IAudioSourceInternal : IAudioSource, IFrameSource<AudioConfig>, Streamable,
    Configurable<AudioConfig>, Releaseable

interface IAudioSource {
    /**
     * [Boolean.true] to mute [IAudioSourceInternal], [Boolean.false] to unmute.
     */
    var isMuted: Boolean
}