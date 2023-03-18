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
package io.github.thibaultbee.streampack.internal.data.orientation

import android.util.Size
import io.github.thibaultbee.streampack.internal.interfaces.IOrientationProvider
import io.github.thibaultbee.streampack.internal.utils.extensions.landscapize
import io.github.thibaultbee.streampack.internal.utils.extensions.portraitize
import io.github.thibaultbee.streampack.utils.OrientationUtils

class FixedOrientationProvider(override val orientation: Int) : IOrientationProvider {
    override fun orientedSize(size: Size): Size {
        return if (OrientationUtils.isPortrait(orientation)) {
            size.portraitize()
        } else {
            size.landscapize()
        }
    }
}