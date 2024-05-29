package io.github.thibaultbee.streampack.ext.rtmp.internal.endpoints.composites.sinks.utils

import io.github.thibaultbee.streampack.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.data.mediadescriptor.RtmpMediaDescriptor
import io.github.thibaultbee.streampack.data.mediadescriptor.UriMediaDescriptor

fun MediaDescriptor.toRtmpUriMediaDescriptor(): UriMediaDescriptor {
    return when (this) {
        is UriMediaDescriptor -> this
        is RtmpMediaDescriptor -> UriMediaDescriptor(this.uri)
        else -> throw IllegalArgumentException("Unsupported MediaDescriptor: ${this::class.java.simpleName}")
    }
}