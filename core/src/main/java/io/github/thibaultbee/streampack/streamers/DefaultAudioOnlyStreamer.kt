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
package io.github.thibaultbee.streampack.streamers

import android.content.Context
import io.github.thibaultbee.streampack.internal.endpoints.DynamicEndpoint
import io.github.thibaultbee.streampack.internal.endpoints.IEndpoint
import io.github.thibaultbee.streampack.internal.sources.audio.MicrophoneSource

/**
 * A [DefaultStreamer] that sends only microphone frames.
 *
 * @param context application context
 * @param internalEndpoint the [IEndpoint] implementation
 */
open class DefaultAudioOnlyStreamer(
    context: Context,
    internalEndpoint: IEndpoint = DynamicEndpoint(context)
) : DefaultStreamer(
    context = context,
    internalVideoSource = null,
    internalAudioSource = MicrophoneSource(),
    internalEndpoint = internalEndpoint
)