package io.github.thibaultbee.streampack.core.internal.utils.av

import android.media.MediaFormat
import android.os.Build


enum class FourCCs(val value: FourCC) {
    AV1(
        FourCC(
            'a', 'v', '0', '1', if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaFormat.MIMETYPE_VIDEO_AV1
            } else {
                null
            }
        )
    ),
    VP9(FourCC('v', 'p', '0', '9', MediaFormat.MIMETYPE_VIDEO_VP9)),
    HEVC(FourCC('h', 'v', 'c', '1', MediaFormat.MIMETYPE_VIDEO_HEVC));

    companion object {
        fun fromMimeType(mimeType: String) =
            entries.first { it.value.mimeType == mimeType }
    }
}

data class FourCC(val a: Char, val b: Char, val c: Char, val d: Char, val mimeType: String?) {
    val code: Int
        get() = (a.code shl 24) or (b.code shl 16) or (c.code shl 8) or d.code

    override fun toString(): String {
        return "$a$b$c$d"
    }
}