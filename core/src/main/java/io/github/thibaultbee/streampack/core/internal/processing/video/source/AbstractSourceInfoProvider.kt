/*
 * Copyright (C) 2024 Thibault B.
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
package io.github.thibaultbee.streampack.core.internal.processing.video.source

import androidx.annotation.IntRange
import io.github.thibaultbee.streampack.core.internal.utils.RotationValue

abstract class AbstractSourceInfoProvider : ISourceInfoProvider {
    protected val listeners = mutableSetOf<ISourceInfoListener>()

    override val isMirror = false

    @IntRange(from = 0, to = 359)
    override val rotationDegrees = 0

    @IntRange(from = 0, to = 359)
    override fun getRelativeRotationDegrees(
        @RotationValue targetRotation: Int, requiredMirroring: Boolean
    ): Int {
        return 0
    }

    override fun addListener(listener: ISourceInfoListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: ISourceInfoListener) {
        listeners.remove(listener)
    }

    override fun removeAllListeners() {
        listeners.clear()
    }
}