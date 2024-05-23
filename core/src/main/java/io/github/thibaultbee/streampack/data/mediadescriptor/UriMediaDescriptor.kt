package io.github.thibaultbee.streampack.data.mediadescriptor

import android.content.Context
import android.net.Uri
import io.github.thibaultbee.streampack.internal.endpoints.MediaContainerType
import io.github.thibaultbee.streampack.internal.endpoints.MediaSinkType

fun UriMediaDescriptor(url: String, customData: List<Any> = emptyList()) =
    UriMediaDescriptor(Uri.parse(url), customData)

class UriMediaDescriptor(val uri: Uri, customData: List<Any> = emptyList()) :
    MediaDescriptor(customData) {
    val sinkType = MediaSinkType.inferFromUri(uri)

    internal fun getContainerType(context: Context) =
        when (sinkType) {
            MediaSinkType.FILE -> MediaContainerType.inferFromFileUri(uri)
            MediaSinkType.CONTENT -> MediaContainerType.inferFromContentUri(context, uri)
            MediaSinkType.SRT -> MediaContainerType.TS
            MediaSinkType.RTMP -> MediaContainerType.FLV
        }
}
