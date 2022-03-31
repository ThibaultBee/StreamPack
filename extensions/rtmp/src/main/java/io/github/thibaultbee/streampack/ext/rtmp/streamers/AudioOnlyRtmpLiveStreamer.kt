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
import io.github.thibaultbee.streampack.ext.rtmp.internal.endpoints.RtmpProducer
import io.github.thibaultbee.streampack.internal.muxers.flv.FlvMuxer
import io.github.thibaultbee.streampack.logger.ILogger
import io.github.thibaultbee.streampack.logger.StreamPackLogger
import io.github.thibaultbee.streampack.streamers.live.BaseAudioOnlyLiveStreamer

/**
 * A [BaseAudioOnlyLiveStreamer] that sends only microphone frames to a remote RTMP device.
 *
 * @param context application context
 * @param logger a [ILogger] implementation
 */
class AudioOnlyRtmpLiveStreamer(
    context: Context,
    logger: ILogger = StreamPackLogger()
) : BaseAudioOnlyLiveStreamer(
    context = context,
    logger = logger,
    muxer = FlvMuxer(context = context, writeToFile = false),
    endpoint = RtmpProducer(logger = logger)
)
