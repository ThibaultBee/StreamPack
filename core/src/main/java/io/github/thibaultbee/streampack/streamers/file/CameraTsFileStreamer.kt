/*
 * Copyright (C) 2021 Thibault B.
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
package io.github.thibaultbee.streampack.streamers.file

import android.content.Context
import io.github.thibaultbee.streampack.internal.muxers.ts.TSMuxer
import io.github.thibaultbee.streampack.internal.muxers.ts.data.TsServiceInfo
import io.github.thibaultbee.streampack.listeners.OnErrorListener
import io.github.thibaultbee.streampack.logger.ILogger
import io.github.thibaultbee.streampack.logger.StreamPackLogger
import io.github.thibaultbee.streampack.utils.Utils
import java.io.File

/**
 * [BaseCameraFileStreamer] that sends microphone and video frames to a TS [File].
 *
 * @param context application context
 * @param logger a [ILogger] implementation
 * @param enableAudio [Boolean.true] to capture audio. False to disable audio capture.
 * @param tsServiceInfo MPEG-TS service description
 * @param initialOnErrorListener initialize [OnErrorListener]
 */
class CameraTsFileStreamer(
    context: Context,
    logger: ILogger = StreamPackLogger(),
    enableAudio: Boolean = true,
    tsServiceInfo: TsServiceInfo = Utils.defaultTsServiceInfo,
    initialOnErrorListener: OnErrorListener? = null
) : BaseCameraFileStreamer(
    context = context,
    logger = logger,
    muxer = TSMuxer().apply { addService(tsServiceInfo) },
    enableAudio = enableAudio,
    initialOnErrorListener = initialOnErrorListener
)