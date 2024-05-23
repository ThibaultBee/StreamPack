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
package io.github.thibaultbee.streampack.app.models

enum class EndpointType(val id: Int) {
    TS_FILE(0),
    FLV_FILE(1),
    SRT(2),
    RTMP(3),
    MP4_FILE(4),
    WEBM_FILE(5),
    OGG_FILE(6),
    THREEGP_FILE(7);

    companion object {
        fun fromId(id: Int): EndpointType = entries.first { it.id == id }
    }
}
