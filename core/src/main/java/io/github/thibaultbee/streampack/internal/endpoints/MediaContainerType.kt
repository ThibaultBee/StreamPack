package io.github.thibaultbee.streampack.internal.endpoints

import android.content.Context
import android.media.MediaFormat
import android.net.Uri

enum class MediaContainerType(val values: Set<String>) {
    MP4("mp4"),
    TS("ts"),
    FLV("flv");

    constructor(vararg values: String) : this(values.toSet())

    companion object {
        fun inferFromExtension(extension: String): MediaContainerType {
            return entries.first { it.values.contains(extension) }
        }

        fun inferFromFileUri(uri: Uri): MediaContainerType {
            val path = uri.path
            val extension = path?.substringAfterLast('.')
                ?: throw IllegalArgumentException("No extension found in uri: $uri")
            return inferFromExtension(extension)
        }

        fun inferFromContentUri(context: Context, uri: Uri): MediaContainerType {
            return when (val type = context.contentResolver.getType(uri)) {
                "video/mp4" -> MP4
                "video/x-flv" -> FLV
                "video/mp2ts" -> TS
                else -> throw UnsupportedOperationException("Unsupported content type: $type")
            }
        }
    }
}
