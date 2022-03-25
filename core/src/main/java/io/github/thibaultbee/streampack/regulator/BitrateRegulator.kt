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
package io.github.thibaultbee.streampack.regulator

import io.github.thibaultbee.streampack.data.BitrateRegulatorConfig

/**
 * Base class of bitrate regulation implementation.
 * If you want to implement your custom bitrate regulator, it must inherit from this class.
 * The bitrate regulator object is created by streamers through the [IBitrateRegulatorFactory].
 *
 * @param bitrateRegulatorConfig bitrate regulation configuration
 * @param onVideoTargetBitrateChange call when you have to change video bitrate
 * @param onAudioTargetBitrateChange call when you have to change audio bitrate
 */
abstract class BitrateRegulator(
    protected val bitrateRegulatorConfig: BitrateRegulatorConfig,
    protected val onVideoTargetBitrateChange: ((Int) -> Unit),
    protected val onAudioTargetBitrateChange: ((Int) -> Unit)
)