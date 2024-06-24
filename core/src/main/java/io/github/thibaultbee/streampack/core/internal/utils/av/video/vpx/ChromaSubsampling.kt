/*
 * Copyright (C) 2023 Thibault B.
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
package io.github.thibaultbee.streampack.core.internal.utils.av.video.vpx

enum class ChromaSubsampling(val value: Byte) {
    YUV420_VERTICAL(0),
    YUV420_COLLOCATED_WITH_LUMA(1),
    YUV422(2),
    YUV444(3),
    YUV440(4);

    companion object {
        fun fromValue(value: Byte) =
            entries.first { it.value == value }
    }
}