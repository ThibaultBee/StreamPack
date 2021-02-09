package com.github.thibaultbee.srtstreamer.utils

import com.github.thibaultbee.srtstreamer.mux.ts.descriptors.AdaptationField
import com.github.thibaultbee.srtstreamer.mux.ts.packets.PesHeader
import com.github.thibaultbee.srtstreamer.mux.ts.tables.TableHeader
import net.magik6k.bitbuffer.BitBuffer

/**
 * Number of remaining byte
 * @return This buffer
 */
fun BitBuffer.remaining(): Long {
    return (limit() - position()) / Byte.SIZE_BITS
}

/**
 * True if buffer has remaining bits/bytes to read
 * @return This buffer
 */
fun BitBuffer.hasRemaining(): Boolean {
    return position() < limit()
}

fun BitBuffer.put(adaptationField: AdaptationField) {
    this.put(adaptationField.asByteBuffer())
}

fun BitBuffer.put(pesHeader: PesHeader) {
    this.put(pesHeader.asByteBuffer())
}

fun BitBuffer.put(tableHeader: TableHeader) {
    this.put(tableHeader.asByteBuffer())
}
