package io.github.thibaultbee.streampack.core.internal.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.UriMediaDescriptor

object DescriptorUtils {
    fun createFileDescriptor(fileName: String): UriMediaDescriptor {
        return UriMediaDescriptor(FileUtils.createCacheFile(fileName).toUri())
    }

    fun createContentUri(context: Context, name: String): Uri {
        val videoDetails = ContentValues().apply {
            put(MediaStore.Video.Media.TITLE, name)
            put(
                MediaStore.Video.Media.DISPLAY_NAME,
                name
            )
        }

        val resolver = context.contentResolver
        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY
                )
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

        return resolver.insert(collection, videoDetails)
            ?: throw Exception("Unable to create video file: $name")
    }

    fun createContentDescriptor(context: Context, name: String): UriMediaDescriptor {
        return UriMediaDescriptor(context, createContentUri(context, name))
    }
}