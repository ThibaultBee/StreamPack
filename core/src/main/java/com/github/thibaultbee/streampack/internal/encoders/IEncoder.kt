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
package com.github.thibaultbee.streampack.internal.encoders

import com.github.thibaultbee.streampack.internal.interfaces.Streamable

interface IEncoder<T> : Streamable<T> {
    /**
     * Input and output of an async encoder
     */
    val encoderListener: IEncoderListener

    /**
     * Get encoder mime type
     * @return a string corresponding to a media mime type
     * @see List of audio/video mime type on <a href="https://developer.android.com/reference/android/media/MediaFormat">Android developer guide</a>
     */
    val mimeType: String?
}