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
package io.github.thibaultbee.streampack.ext.srt.data.mediadescriptor

import android.net.Uri
import io.github.thibaultbee.streampack.core.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.data.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.data.mediadescriptor.createDefaultTsServiceInfo
import io.github.thibaultbee.streampack.core.internal.endpoints.MediaContainerType
import io.github.thibaultbee.streampack.core.internal.endpoints.MediaSinkType
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.ts.data.TSServiceInfo
import java.security.InvalidParameterException

/**
 * Creates a SRT connection descriptor from an [descriptor].
 * If the descriptor is already a [SrtMediaDescriptor], it will be returned as is.
 * If the descriptor is an [UriMediaDescriptor], it will be converted to a [SrtMediaDescriptor] with default [TSServiceInfo].
 * Otherwise, an [InvalidParameterException] will be thrown.
 */
fun SrtMediaDescriptor(descriptor: MediaDescriptor) =
    when (descriptor) {
        is SrtMediaDescriptor -> descriptor
        is UriMediaDescriptor -> {
            val serviceInfo = descriptor.getCustomData(TSServiceInfo::class.java)
                ?: createDefaultTsServiceInfo()
            SrtMediaDescriptor(descriptor.uri, serviceInfo)
        }

        else -> throw InvalidParameterException("Invalid descriptor ${descriptor::class.java.simpleName}")
    }


/**
 * Creates a SRT connection descriptor from an [Uri]
 *
 * @param uri srt server uri
 * @param serviceInfo TS service information
 */
fun SrtMediaDescriptor(uri: Uri, serviceInfo: TSServiceInfo = createDefaultTsServiceInfo()) =
    SrtMediaDescriptor.fromUri(uri, serviceInfo = serviceInfo)

/**
 * A convenient class for SRT connection parameters.
 *
 * If the field is null, it will be ignored. The default SRT parameters will be used (see [default SRT options](https://github.com/Haivision/srt/blob/master/docs/API/API-socket-options.md))
 * See
 *
 * @param host server ip
 * @param port server port
 * @param streamId SRT stream ID
 * @param passPhrase SRT passPhrase
 * @param latency SRT latency in ms
 * @param connectionTimeout SRT connection timeout in ms
 * @param serviceInfo TS service information
 */
class SrtMediaDescriptor(
    val host: String,
    val port: Int,
    val streamId: String? = null,
    val passPhrase: String? = null,
    val latency: Int? = null,
    val connectionTimeout: Int? = null,
    serviceInfo: TSServiceInfo = createDefaultTsServiceInfo()
) : MediaDescriptor(
    Type(MediaContainerType.TS, MediaSinkType.SRT),
    listOf(serviceInfo)
) {
    init {
        require(host.isNotBlank()) { "Invalid host $host" }
        require(
            host.startsWith(SRT_PREFIX).not()
        ) { "Invalid host $host: must not start with prefix srt://" }
        require(port > 0) { "Invalid port $port" }
        require(port < 65536) { "Invalid port $port" }
    }

    override val uri: Uri = buildUri()

    private fun buildUri(): Uri {
        val uriBuilder = Uri.Builder()
            .scheme(SRT_SCHEME)
            .encodedAuthority("$host:$port")
        streamId?.let { uriBuilder.appendQueryParameter(STREAM_ID_QUERY_PARAMETER, it) }
        passPhrase?.let { uriBuilder.appendQueryParameter(PASS_PHRASE_QUERY_PARAMETER, it) }
        latency?.let { uriBuilder.appendQueryParameter(LATENCY_QUERY_PARAMETER, it.toString()) }
        connectionTimeout?.let {
            uriBuilder.appendQueryParameter(
                CONNECTION_TIMEOUT_QUERY_PARAMETER,
                it.toString()
            )
        }
        return uriBuilder.build()
    }

    companion object {
        private const val SRT_SCHEME = "srt"
        private const val SRT_PREFIX = "$SRT_SCHEME://"

        private const val STREAM_ID_QUERY_PARAMETER = "streamid"
        private const val PASS_PHRASE_QUERY_PARAMETER = "passphrase"
        private const val LATENCY_QUERY_PARAMETER = "latency"
        private const val CONNECTION_TIMEOUT_QUERY_PARAMETER = "connect_timeout"
        private const val MODE_QUERY_PARAMETER = "mode"
        private const val TRANSTYPE_QUERY_PARAMETER = "transtype"

        private val queryParameterList = listOf(
            STREAM_ID_QUERY_PARAMETER,
            PASS_PHRASE_QUERY_PARAMETER,
            LATENCY_QUERY_PARAMETER,
            CONNECTION_TIMEOUT_QUERY_PARAMETER,
            MODE_QUERY_PARAMETER,
            TRANSTYPE_QUERY_PARAMETER
        )

        /**
         * Creates a SRT connection descriptor from an URL
         *
         * @param url server url (syntax: srt://host:port?streamid=streamId&passphrase=passPhrase)
         * @return SRT connection descriptor
         */
        fun fromUrl(url: String, serviceInfo: TSServiceInfo = createDefaultTsServiceInfo()) =
            fromUri(Uri.parse(url), serviceInfo)

        /**
         * Creates a SRT connection descriptor from an Uri
         *
         * @param uri server uri
         * @return SRT connection descriptor
         */
        fun fromUri(
            uri: Uri,
            serviceInfo: TSServiceInfo = createDefaultTsServiceInfo()
        ): SrtMediaDescriptor {
            if (uri.scheme != SRT_SCHEME) {
                throw InvalidParameterException("URL $uri is not an srt URL")
            }
            val host = uri.host
                ?: throw InvalidParameterException("Failed to parse URL $uri: unknown host")
            val port = uri.port

            val streamId = uri.getQueryParameter(STREAM_ID_QUERY_PARAMETER)
            val passPhrase = uri.getQueryParameter(PASS_PHRASE_QUERY_PARAMETER)
            val latency = uri.getQueryParameter(LATENCY_QUERY_PARAMETER)?.toInt()
            val connectionTimeout =
                uri.getQueryParameter(CONNECTION_TIMEOUT_QUERY_PARAMETER)?.toInt()

            val mode = uri.getQueryParameter(MODE_QUERY_PARAMETER)
            if (mode != null) {
                require(mode == "caller") { "Failed to parse URL $uri: invalid mode: $mode" }
            }
            val transtype = uri.getQueryParameter(TRANSTYPE_QUERY_PARAMETER)
            if (transtype != null) {
                require(transtype == "live") { "Failed to parse URL $uri: invalid transtype: $transtype" }
            }

            val unknownParameters =
                uri.queryParameterNames.find { queryParameterList.contains(it).not() }
            if (unknownParameters != null) {
                throw InvalidParameterException("Failed to parse URL $uri: unknown parameter(s): $unknownParameters")
            }

            return SrtMediaDescriptor(
                host,
                port,
                streamId,
                passPhrase,
                latency = latency,
                connectionTimeout = connectionTimeout,
                serviceInfo = serviceInfo
            )
        }
    }
}
