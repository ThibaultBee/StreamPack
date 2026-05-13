/*
 * Copyright (C) 2023 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.ext.rtmp.configuration.mediadescriptor

import android.net.Uri
import androidx.core.net.toUri
import io.github.komedia.komuxer.rtmp.messages.command.ConnectObjectBuilder
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.elements.endpoints.MediaContainerType
import io.github.thibaultbee.streampack.core.elements.endpoints.MediaSinkType
import java.security.InvalidParameterException

/**
 * Creates a RTMP connection descriptor from an [descriptor].
 * If the descriptor is already a [RtmpMediaDescriptor], it will be returned as is.
 * If the descriptor is an [UriMediaDescriptor], it will be converted to a [RtmpMediaDescriptor].
 * Otherwise, an [InvalidParameterException] will be thrown.
 */
fun RtmpMediaDescriptor(descriptor: MediaDescriptor) =
    RtmpMediaDescriptor.fromMediaDescriptor(descriptor)

/**
 * Creates a RTMP connection descriptor from an [Uri]
 *
 * @param uri the server uri
 * @param connectInfo configures the RTMP connect command sent during the handshake
 */
fun RtmpMediaDescriptor(
    uri: Uri,
    connectInfo: (ConnectObjectBuilder.() -> Unit)? = null
) = RtmpMediaDescriptor.fromUri(uri, connectInfo)

/**
 * RTMP connection parameters
 *
 * @param scheme the RTMP scheme (rtmp, rtmps, rtmpt, rtmpe, rtmfp, rtmpte, rtmpts)
 * @param host the server ip
 * @param port the server port
 * @param app the application name
 * @param streamKey the stream key
 * @param connectInfo configures the RTMP connect command sent during the handshake
 */
class RtmpMediaDescriptor(
    val scheme: String, val host: String, val port: Int, val app: String?, val streamKey: String,
    val connectInfo: (ConnectObjectBuilder.() -> Unit)? = null
) : MediaDescriptor(
    Type(MediaContainerType.FLV, MediaSinkType.RTMP),
    emptyList()
) {
    init {
        require(scheme == RTMP_SCHEME || scheme == RTMPS_SCHEME || scheme == RTMPT_SCHEME || scheme == RTMPE_SCHEME || scheme == RTMFP_SCHEME || scheme == RTMPTE_SCHEME || scheme == RTMPTS_SCHEME) { "Invalid scheme $scheme" }
        require(host.isNotBlank()) { "Invalid host $host" }
        require(port > 0) { "Invalid port $port" }
        require(port < 65536) { "Invalid port $port" }
        app?.let { require(it.isNotBlank()) { "Invalid app $app" } }
        require(streamKey.isNotBlank()) { "Invalid streamKey $streamKey" }
    }

    override val uri: Uri = Uri.Builder()
        .scheme(scheme)
        .encodedAuthority("$host:$port")
        .apply {
            app?.let { appendEncodedPath(app) }
        }
        .appendEncodedPath(streamKey)
        .build()

    companion object {
        private const val RTMP_SCHEME = "rtmp"
        private const val RTMPS_SCHEME = "rtmps"
        private const val RTMPT_SCHEME = "rtmpt"
        private const val RTMPE_SCHEME = "rtmpe"
        private const val RTMFP_SCHEME = "rtmfp"
        private const val RTMPTE_SCHEME = "rtmpte"
        private const val RTMPTS_SCHEME = "rtmpts"

        private const val DEFAULT_PORT = 1935
        private const val SSL_DEFAULT_PORT = 443
        private const val HTTP_DEFAULT_PORT = 80

        /**
         * Creates a RTMP connection descriptor from an URL
         *
         * @param url the server url (syntax: rtmp://host:port/app/streamKey)
         * @param connectInfo configures the RTMP connect command sent during the handshake
         * @return RTMP connection descriptor
         */
        internal fun fromUrl(
            url: String,
            connectInfo: (ConnectObjectBuilder.() -> Unit)? = null
        ) = fromUri(url.toUri(), connectInfo)

        /**
         * Creates a RTMP connection descriptor from an Uri
         *
         * @param uri the server Uri
         * @param connectInfo configures the RTMP connect command sent during the handshake
         * @return RTMP connection descriptor
         */
        internal fun fromUri(
            uri: Uri,
            connectInfo: (ConnectObjectBuilder.() -> Unit)? = null
        ): RtmpMediaDescriptor = parse(uri, connectInfo)

        /**
         * Creates a RTMP connection descriptor from a generic [MediaDescriptor].
         *
         * If the descriptor is already a [RtmpMediaDescriptor], it is returned as is.
         * If the descriptor is an [UriMediaDescriptor], it will be converted to a [RtmpMediaDescriptor].
         * Otherwise, an [InvalidParameterException] will be thrown.
         *
         * @param descriptor the source descriptor
         * @return RTMP connection descriptor
         */
        internal fun fromMediaDescriptor(descriptor: MediaDescriptor): RtmpMediaDescriptor =
            when (descriptor) {
                is RtmpMediaDescriptor -> descriptor
                is UriMediaDescriptor -> {
                    parse(
                        descriptor.uri,
                        null
                    )
                }

                else -> throw InvalidParameterException("Invalid descriptor ${descriptor::class.java.simpleName} for RTMP")
            }

        private fun parse(
            uri: Uri,
            connectInfo: (ConnectObjectBuilder.() -> Unit)?
        ): RtmpMediaDescriptor {
            val scheme =
                uri.scheme ?: throw InvalidParameterException("Invalid scheme ${uri.scheme}")
            val host = uri.host ?: throw InvalidParameterException("Invalid host ${uri.host}")
            val port = if (uri.port > 0) {
                uri.port
            } else {
                when (scheme) {
                    RTMPS_SCHEME, RTMPTS_SCHEME -> SSL_DEFAULT_PORT
                    RTMPT_SCHEME, RTMPTE_SCHEME -> HTTP_DEFAULT_PORT
                    else -> DEFAULT_PORT
                }
            }
            if (uri.pathSegments.isEmpty()) {
                throw InvalidParameterException("Invalid path ${uri.path} expected /app/streamKey or /streamKey")
            } else {
                val streamKey = uri.lastPathSegment
                    ?: throw InvalidParameterException("Invalid streamKey ${uri.lastPathSegment}")
                if (uri.pathSegments.size == 1) {
                    return RtmpMediaDescriptor(scheme, host, port, null, streamKey, connectInfo)
                } else {
                    val app = uri.pathSegments.minus(uri.lastPathSegment).joinToString("/")
                    return RtmpMediaDescriptor(scheme, host, port, app, streamKey, connectInfo)
                }
            }
        }
    }
}
