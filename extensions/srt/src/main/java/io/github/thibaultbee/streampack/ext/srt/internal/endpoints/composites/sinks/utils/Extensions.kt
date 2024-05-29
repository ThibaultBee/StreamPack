package io.github.thibaultbee.streampack.ext.srt.internal.endpoints.composites.sinks.utils

import io.github.thibaultbee.streampack.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.data.mediadescriptor.SrtMediaDescriptor
import io.github.thibaultbee.streampack.data.mediadescriptor.UriMediaDescriptor

fun MediaDescriptor.toSrtConnectionDescriptor(): SrtMediaDescriptor {
    return when (this) {
        is SrtMediaDescriptor -> this
        is UriMediaDescriptor -> SrtMediaDescriptor.fromUri(uri)
        else -> throw IllegalArgumentException("Unsupported MediaDescriptor: ${this::class.java.simpleName}")
    }
}