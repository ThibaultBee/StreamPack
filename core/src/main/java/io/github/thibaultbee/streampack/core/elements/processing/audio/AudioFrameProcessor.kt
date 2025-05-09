/*
 * Copyright (C) 2025 Thibault B.
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
package io.github.thibaultbee.streampack.core.elements.processing.audio

import io.github.thibaultbee.streampack.core.elements.data.RawFrame
import io.github.thibaultbee.streampack.core.elements.processing.IFrameProcessor

/**
 * Audio frame processor.
 *
 * Only supports mute effect for now.
 */
class AudioFrameProcessor : IFrameProcessor<RawFrame>,
    IAudioFrameProcessor {
    override var isMuted = false
    private val muteEffect = MuteEffect()

    override fun processFrame(frame: RawFrame): RawFrame {
        if (isMuted) {
            return muteEffect.processFrame(frame)
        }
        return frame
    }
}