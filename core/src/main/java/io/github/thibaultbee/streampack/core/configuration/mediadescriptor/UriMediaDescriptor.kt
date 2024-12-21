package io.github.thibaultbee.streampack.core.configuration.mediadescriptor

import android.content.Context
import android.net.Uri
import io.github.thibaultbee.streampack.core.internal.endpoints.MediaContainerType
import io.github.thibaultbee.streampack.core.internal.endpoints.MediaSinkType


/**
 * Creates a media descriptor from an [String].
 *
 * Use this function when you have a content uri and a context to infer container type.
 *
 * @param context the context to infer container type from content uri
 * @param uriString the uri to create the media descriptor from
 * @param customData custom data to attach to the media descriptor
 */
fun UriMediaDescriptor(
    context: Context,
    uriString: String,
    customData: List<Any> = emptyList()
): UriMediaDescriptor {
    val uri = Uri.parse(uriString)
    return UriMediaDescriptor(uri, MediaDescriptor.Type.getContainerType(context, uri), customData)
}

/**
 * Creates a media descriptor from a [String].
 *
 * Use this function if your [uriString] is not a content uri.
 *
 * @param uriString the uri to create the media descriptor from
 * @param customData custom data to attach to the media descriptor
 */
fun UriMediaDescriptor(uriString: String, customData: List<Any> = emptyList()): UriMediaDescriptor {
    val uri = Uri.parse(uriString)
    return UriMediaDescriptor(uri, MediaDescriptor.Type.getContainerType(null, uri), customData)
}

/**
 * Creates a media descriptor from an [Uri].
 *
 * Use this function when you have a content uri and a context to infer container type.
 *
 * @param context the context to infer container type from content uri
 * @param uri the uri to create the media descriptor from
 * @param customData custom data to attach to the media descriptor
 */
fun UriMediaDescriptor(context: Context, uri: Uri, customData: List<Any> = emptyList()) =
    UriMediaDescriptor(
        uri, MediaDescriptor.Type.getContainerType(context, uri), customData
    )

/**
 * Creates a media descriptor from an [Uri]
 *
 * Use this function if your [uri] is not a content uri.
 *
 * @param uri the uri to create the media descriptor from
 * @param customData custom data to attach to the media descriptor
 */
fun UriMediaDescriptor(uri: Uri, customData: List<Any> = emptyList()) = UriMediaDescriptor(
    uri, MediaDescriptor.Type.getContainerType(null, uri), customData
)

/**
 * A Media descriptor for [Uri].
 */
class UriMediaDescriptor
internal constructor(
    override val uri: Uri, containerType: MediaContainerType, customData: List<Any> = emptyList()
) : MediaDescriptor(
    Type(containerType, MediaSinkType.inferFromUri(uri)), customData
)