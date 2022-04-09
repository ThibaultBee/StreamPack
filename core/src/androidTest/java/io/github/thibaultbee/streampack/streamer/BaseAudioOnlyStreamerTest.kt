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
package io.github.thibaultbee.streampack.streamer

import io.github.thibaultbee.streampack.internal.endpoints.FakeEndpoint
import io.github.thibaultbee.streampack.internal.muxers.flv.FlvMuxer
import io.github.thibaultbee.streampack.internal.muxers.ts.TSMuxer
import io.github.thibaultbee.streampack.streamer.testcases.AudioOnlyStreamerTestCase
import io.github.thibaultbee.streampack.streamers.bases.BaseAudioOnlyStreamer
import io.github.thibaultbee.streampack.utils.AndroidUtils

class TsAudioOnlyStreamerTest : AudioOnlyStreamerTestCase() {
    override val streamer = BaseAudioOnlyStreamer(
        context,
        logger,
        TSMuxer().apply { addService(AndroidUtils.fakeServiceInfo()) },
        FakeEndpoint(logger),
    )
}

class FlvAudioOnlyStreamerTest : AudioOnlyStreamerTestCase() {
    override val streamer = BaseAudioOnlyStreamer(
        context,
        logger,
        FlvMuxer(context, writeToFile = false),
        FakeEndpoint(logger),
    )
}