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
package io.github.thibaultbee.streampack.core.internal.utils.av.video.hevc

enum class HEVCProfile(val value: Short) {
    MAIN(1),
    MAIN_10(2),
    MAIN_STILL_PICTURE(3),
    REXT(4),
    HIGH_THROUGHPUT(5),
    MULTIVIEW_MAIN(6),
    SCALABLE_MAIN(7),
    THREED_MAIN(8),
    SCREEN_EXTENDED(9),
    SCALABLE_REXT(10),
    HIGH_THROUGHPUT_SCREEN_EXTENDED(11);

    companion object {
        fun entryOf(profileIdc: Short) =
            entries.first { it.value == profileIdc }
    }
}