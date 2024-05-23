package io.github.thibaultbee.streampack.internal.endpoints

import android.net.Uri

/**
 * Media scheme.
 * Use to determine the type of sink.
 */
enum class MediaSinkType(val schemes: Set<String>) {
    FILE("file"),
    SRT("srt"),
    RTMP("rtmp", "rtmps"),
    CONTENT("content");

    constructor(vararg values: String) : this(values.toSet())

    companion object {

        fun inferFromUri(uri: Uri) = inferFromScheme(uri.scheme)

        fun inferFromScheme(scheme: String?): MediaSinkType {
            if (isLocalFileScheme(scheme)) {
                return FILE
            }
            return entries.first { it.schemes.contains(scheme) }
        }

        private fun isLocalFileScheme(scheme: String?): Boolean {
            return scheme.isNullOrEmpty() || (FILE.schemes.contains(scheme))
        }
    }
}

