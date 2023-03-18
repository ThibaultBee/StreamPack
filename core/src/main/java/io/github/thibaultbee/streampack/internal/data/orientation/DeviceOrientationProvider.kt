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

import android.content.Context
import android.util.Size
import io.github.thibaultbee.streampack.internal.interfaces.IOrientationProvider
import io.github.thibaultbee.streampack.internal.utils.extensions.deviceOrientation
import io.github.thibaultbee.streampack.internal.utils.extensions.isDevicePortrait
import io.github.thibaultbee.streampack.internal.utils.extensions.landscapize
import io.github.thibaultbee.streampack.internal.utils.extensions.portraitize

class DeviceOrientationProvider(private val context: Context) : IOrientationProvider {
    override val orientation: Int
        get() {
            //TODO: this might not be working on all devices
            val deviceOrientation = context.deviceOrientation
            return if (deviceOrientation == 0) 270 else deviceOrientation - 90
        }

    override fun orientedSize(size: Size): Size {
        return if (context.isDevicePortrait) {
            size.portraitize()
        } else {
            size.landscapize()
        }
    }
}