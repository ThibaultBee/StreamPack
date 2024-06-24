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
package io.github.thibaultbee.streampack.core.data

import android.util.Range

/**
 * Bitrate regulator configuration. Use it to control bitrate
 */
data class BitrateRegulatorConfig(
    /**
     * Video encoder bitrate ranges in bits/s.
     */
    val videoBitrateRange: Range<Int> = Range(500, 10000),
    /**
     * Audio encoder bitrate ranges in bits/s.
     */
    val audioBitrateRange: Range<Int> = Range(128000, 128000)
)
