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
package io.github.thibaultbee.streampack.core.streamers.single

import android.content.Context
import io.github.thibaultbee.streampack.core.internal.endpoints.DynamicEndpoint
import io.github.thibaultbee.streampack.core.internal.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.internal.sources.audio.IAudioSourceInternal
import io.github.thibaultbee.streampack.core.internal.sources.audio.audiorecord.MicrophoneSource.Companion.buildDefaultMicrophoneSource
import io.github.thibaultbee.streampack.core.streamers.DefaultStreamer

/**
 * A [DefaultStreamer] that sends only microphone frames.
 *
 * @param context application context
 * @param internalEndpoint the [IEndpointInternal] implementation
 */
open class AudioOnlySingleStreamer(
    context: Context,
    audioSourceInternal: IAudioSourceInternal? = buildDefaultMicrophoneSource(),
    internalEndpoint: IEndpointInternal = DynamicEndpoint(context)
) : DefaultStreamer(
    context = context,
    videoSourceInternal = null,
    audioSourceInternal = audioSourceInternal,
    endpointInternal = internalEndpoint
)