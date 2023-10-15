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
package io.github.thibaultbee.streampack.internal.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.utils.extensions.putString
import java.nio.ByteBuffer

class FileTypeBox(
    private val majorBrand: String = "isom",
    private val minorVersion: Int = 512,
    private val compatibleBrands: List<String> = listOf("isom", "iso6", "iso2", "avc1", "mp41")
) : Box("ftyp") {
    init {
        require(majorBrand.length == 4) { "majorBrand must be 4 characters long" }
        require(compatibleBrands.all { it.length == 4 }) { "compatibleBrands must be 4 characters long" }
    }

    override val size: Int = super.size + 8 + compatibleBrands.sumOf { it.length }

    override fun write(output: ByteBuffer) {
        super.write(output)
        output.putString(majorBrand)
        output.putInt(minorVersion)
        compatibleBrands.forEach { output.putString(it) }
    }
}