package io.github.thibaultbee.streampack.core.elements.endpoints

import android.content.Context
import android.net.Uri

enum class MediaContainerType(val values: Set<String>) {
    MP4("mp4"),
    TS("ts"),
    FLV("flv"),
    THREEGP("3gp", "3gpp"),
    WEBM("webm"),
    OGG("ogg");

    constructor(vararg values: String) : this(values.toSet())

    companion object {
        private fun inferFromExtension(extension: String): MediaContainerType {
            val type = entries.firstOrNull { it.values.contains(extension) }
            return type ?: throw IllegalArgumentException("Unsupported extension: $extension")
        }

        internal fun inferFromFileUri(uri: Uri): MediaContainerType {
            val extension = uri.toString().substringAfterLast('.')
            return inferFromExtension(extension)
        }

        internal fun inferFromContentUri(context: Context, uri: Uri): MediaContainerType {
            return when (val type = context.contentResolver.getType(uri)) {
                "video/mp4" -> MP4
                "video/x-flv" -> FLV
                "video/mp2ts" -> TS
                "video/webm" -> WEBM
                "video/ogg" -> OGG
                "video/3gpp" -> THREEGP
                "audio/ogg" -> OGG
                else -> throw UnsupportedOperationException("Unsupported content type: $type")
            }
        }
    }
}
