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
package com.github.thibaultbee.streampack.data

import java.nio.ByteBuffer

data class Frame(
    var buffer: ByteBuffer,
    val mimeType: String,
    var pts: Long, // in µs
    var dts: Long? = null, // in µs
    val isKeyFrame: Boolean = false,
    val isCodecData: Boolean = false,
    val extra: ByteBuffer? = null
)