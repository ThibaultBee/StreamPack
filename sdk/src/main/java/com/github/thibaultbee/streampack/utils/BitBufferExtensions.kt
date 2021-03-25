package com.github.thibaultbee.streampack.utils

import com.github.thibaultbee.streampack.muxers.ts.descriptors.AdaptationField
import com.github.thibaultbee.streampack.muxers.ts.packets.PesHeader
import com.github.thibaultbee.streampack.muxers.ts.tables.TableHeader

fun BitBuffer.put(adaptationField: AdaptationField) {
    this.put(adaptationField.toByteBuffer())
}

fun BitBuffer.put(pesHeader: PesHeader) {
    this.put(pesHeader.toByteBuffer())
}

fun BitBuffer.put(tableHeader: TableHeader) {
    this.put(tableHeader.toByteBuffer())
}
