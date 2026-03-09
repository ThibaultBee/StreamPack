package io.github.thibaultbee.streampack.core.elements.endpoints

import android.net.Uri

/**
 * Media scheme.
 * Use to determine the type of sink.
 */
enum class MediaSinkType(val schemes: Set<String>) {
    FILE("file"),
    SRT("srt"),
    RTMP("rtmp", "rtmps", "rtmpt", "rtmpts"),
    CONTENT("content");

    constructor(vararg values: String) : this(values.toSet())

    companion object {

        fun inferFromUri(uri: Uri) = inferFromScheme(uri.scheme)

        fun inferFromScheme(scheme: String?): MediaSinkType {
            if (isLocalFileScheme(scheme)) {
                return FILE
            }
            if (scheme.isNullOrEmpty()) {
                throw IllegalArgumentException("Scheme cannot be empty")
            }
            return entries.firstOrNull { it.schemes.contains(scheme) }
                ?: throw IllegalArgumentException("Unknown scheme: $scheme")
        }

        private fun isLocalFileScheme(scheme: String?): Boolean {
            return scheme.isNullOrEmpty() || (FILE.schemes.contains(scheme))
        }
    }
}

/**
 * Whether the sink is local (ie. a local file) or not.
 */
val MediaSinkType.isLocal: Boolean
    get() = this == MediaSinkType.FILE || this == MediaSinkType.CONTENT

