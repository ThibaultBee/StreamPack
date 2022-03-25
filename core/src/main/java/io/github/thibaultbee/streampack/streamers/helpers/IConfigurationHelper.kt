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
package io.github.thibaultbee.streampack.streamers.helpers

import android.util.Range

interface IConfigurationHelper {
    val video: IVideoConfigurationHelper
    val audio: IAudioConfigurationHelper
}

interface IVideoConfigurationHelper : IAVConfigurationHelper

interface IAudioConfigurationHelper : IAVConfigurationHelper {
    fun getSupportedInputChannelRange(mimeType: String): Range<Int>
    fun getSupportedSampleRates(mimeType: String): List<Int>
    fun getSupportedByteFormats(): List<Int>
}


interface IAVConfigurationHelper {
    val supportedEncoders: List<String>
    fun getSupportedBitrates(mimeType: String): Range<Int>
}