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
package com.github.thibaultbee.streampack.internal.muxers.ts.utils

import com.github.thibaultbee.streampack.internal.data.Packet
import com.github.thibaultbee.streampack.internal.muxers.IMuxerListener

object MuxerConst {
    const val PAT_PACKET_PERIOD = 40
    const val SDT_PACKET_PERIOD = 200

    /**
     * Number of MPEG-TS packet stream in output [Packet] returns by [IMuxerListener.onOutputFrame]
     */
    const val MAX_OUTPUT_PACKET_NUMBER = 7
}