package io.github.thibaultbee.streampack.core.configuration.mediadescriptor

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import io.github.thibaultbee.streampack.core.elements.endpoints.MediaContainerType
import io.github.thibaultbee.streampack.core.elements.endpoints.MediaSinkType
import java.io.File

/**
 * Creates a media descriptor in the media [android.content.ContentResolver].
 *
 * @param context the context to infer container type from content uri
 * @param contentValues the content values to create the media descriptor from
 * @param isVideo true to target the Video MediaStore, false to target the Audio MediaStore
 * @param customData custom data to attach to the media descriptor
 */
fun mediaStoreMediaDescriptor(
    context: Context,
    contentValues: ContentValues,
    isVideo: Boolean = true,
    customData: List<Any> = emptyList()
): UriMediaDescriptor {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (isVideo) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
        } else {
            if (isVideo) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
        }
        return UriMediaDescriptor(context, contentValues, collection, customData)
    } else {
        val filename = contentValues.getAsString(MediaStore.MediaColumns.DISPLAY_NAME)
            ?: throw IllegalArgumentException("ContentValues must contain DISPLAY_NAME for API < 26")

        val directoryType = if (isVideo) {
            Environment.DIRECTORY_MOVIES
        } else {
            Environment.DIRECTORY_MUSIC
        }
        val directory = Environment.getExternalStoragePublicDirectory(directoryType)
        directory.mkdirs()
        val file = File(directory, filename)
        val uri = Uri.fromFile(file)
        return UriMediaDescriptor(uri, customData)
    }
}

/**
 * Creates a media descriptor in the collection [android.content.ContentResolver].
 *
 * @param context the context to infer container type from content uri
 * @param contentValues the content values to create the media descriptor from
 * @param collection the collection to create the media descriptor from
 * @param customData custom data to attach to the media descriptor
 */
private fun UriMediaDescriptor(
    context: Context,
    contentValues: ContentValues,
    collection: Uri,
    customData: List<Any> = emptyList()
): UriMediaDescriptor {
    val contentResolver = context.contentResolver
    val uri = contentResolver.insert(collection, contentValues)
        ?: throw RuntimeException("Unable to create file: $contentValues in $collection")
    return UriMediaDescriptor(context, uri, customData)
}


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
    val uri = uriString.toUri()
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
    val uri = uriString.toUri()
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
open class UriMediaDescriptor
internal constructor(
    override val uri: Uri, containerType: MediaContainerType, customData: List<Any> = emptyList()
) : MediaDescriptor(
    Type(containerType, MediaSinkType.inferFromUri(uri)), customData
)