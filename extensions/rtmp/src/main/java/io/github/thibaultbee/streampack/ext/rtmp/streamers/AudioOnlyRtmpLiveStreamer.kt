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
package io.github.thibaultbee.streampack.ext.rtmp.streamers

import android.content.Context
import io.github.thibaultbee.streampack.ext.rtmp.internal.endpoints.sinks.RtmpSink
import io.github.thibaultbee.streampack.internal.endpoints.composites.ConnectableCompositeEndpoint
import io.github.thibaultbee.streampack.internal.endpoints.muxers.flv.FlvMuxer
import io.github.thibaultbee.streampack.streamers.live.BaseAudioOnlyLiveStreamer

/**
 * A [BaseAudioOnlyLiveStreamer] that sends only microphone frames to a remote RTMP device.
 *
 * @param context application context
 */
class AudioOnlyRtmpLiveStreamer(
    context: Context
) : BaseAudioOnlyLiveStreamer(
    context = context,
    internalEndpoint = ConnectableCompositeEndpoint(FlvMuxer(writeToFile = false), RtmpSink())
)
