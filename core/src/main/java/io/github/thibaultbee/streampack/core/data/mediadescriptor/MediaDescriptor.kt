package io.github.thibaultbee.streampack.core.data.mediadescriptor

import android.content.Context
import android.net.Uri
import io.github.thibaultbee.streampack.core.data.mediadescriptor.MediaDescriptor.Type.Companion.getContainerType
import io.github.thibaultbee.streampack.core.internal.endpoints.MediaContainerType
import io.github.thibaultbee.streampack.core.internal.endpoints.MediaSinkType


abstract class MediaDescriptor(
    val type: Type,
    val customData: List<Any> = emptyList()
) {
    abstract val uri: Uri

    internal fun hasCustomData(clazz: Class<*>): Boolean {
        return customData.any { clazz.isInstance(it) }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getCustomData(clazz: Class<T>): T? {
        return customData.firstOrNull { clazz.isInstance(it) } as T?
    }

    data class Type(
        val containerType: MediaContainerType,
        val sinkType: MediaSinkType,
    ) {
        companion object {
            fun getContainerType(context: Context?, uri: Uri) =
                getContainerType(context, MediaSinkType.inferFromUri(uri), uri)

            private fun getContainerType(
                context: Context?,
                mediaSinkType: MediaSinkType,
                uri: Uri
            ) =
                when (mediaSinkType) {
                    MediaSinkType.FILE -> MediaContainerType.inferFromFileUri(uri)
                    MediaSinkType.CONTENT -> {
                        require(context != null) { "Context is required to infer container type from content uri" }
                        MediaContainerType.inferFromContentUri(context, uri)
                    }

                    MediaSinkType.SRT -> MediaContainerType.TS
                    MediaSinkType.RTMP -> MediaContainerType.FLV
                }
        }
    }
}

fun MediaType(uri: Uri) =
    MediaDescriptor.Type(
        getContainerType(null, uri),
        MediaSinkType.inferFromUri(uri)
    )

fun MediaType(context: Context, uri: Uri) =
    MediaDescriptor.Type(
        getContainerType(context, uri),
        MediaSinkType.inferFromUri(uri)
    )

