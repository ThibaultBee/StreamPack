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
package io.github.thibaultbee.streampack.ext.srt.data

import android.net.Uri
import java.security.InvalidParameterException


/**
 * SRT connection parameters
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
 */
data class SrtConnectionDescriptor(
    val host: String,
    val port: Int,
    val streamId: String? = null,
    val passPhrase: String? = null,
    val latency: Int? = null,
    val connectionTimeout: Int? = null,
) {
    init {
        require(host.isNotBlank()) { "Invalid host $host" }
        require(
            host.startsWith(SRT_PREFIX).not()
        ) { "Invalid host $host: must not start with prefix srt://" }
        require(port > 0) { "Invalid port $port" }
        require(port < 65536) { "Invalid port $port" }
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
         * Creates a SRT connection from an URL
         *
         * @param url server url (syntax: srt://host:port?streamid=streamId&passphrase=passPhrase)
         * @return SRT connection
         */
        fun fromUrl(url: String): SrtConnectionDescriptor {
            val uri = Uri.parse(url)
            if (uri.scheme != SRT_SCHEME) {
                throw InvalidParameterException("URL $url is not an srt URL")
            }
            val host = uri.host
                ?: throw InvalidParameterException("Failed to parse URL $url: unknown host")
            val port = uri.port

            val streamId = uri.getQueryParameter(STREAM_ID_QUERY_PARAMETER)
            val passPhrase = uri.getQueryParameter(PASS_PHRASE_QUERY_PARAMETER)
            val latency = uri.getQueryParameter(LATENCY_QUERY_PARAMETER)?.toInt()
            val connectionTimeout =
                uri.getQueryParameter(CONNECTION_TIMEOUT_QUERY_PARAMETER)?.toInt()


            val mode = uri.getQueryParameter(MODE_QUERY_PARAMETER)
            if (mode != null) {
                require(mode == "caller") { "Failed to parse URL $url: invalid mode: $mode" }
            }
            val transtype = uri.getQueryParameter(TRANSTYPE_QUERY_PARAMETER)
            if (transtype != null) {
                require(transtype == "live") { "Failed to parse URL $url: invalid transtype: $transtype" }
            }

            val unknownParameters =
                uri.queryParameterNames.find { queryParameterList.contains(it).not() }
            if (unknownParameters != null) {
                throw InvalidParameterException("Failed to parse URL $url: unknown parameter(s): $unknownParameters")
            }

            return SrtConnectionDescriptor(
                host,
                port,
                streamId,
                passPhrase,
                latency = latency,
                connectionTimeout = connectionTimeout
            )
        }

        /**
         * Creates a SRT connection from an URL and given parameters.
         * Query parameters are ignored.
         *
         * @param url server url (syntax: srt://host:port)
         * @param streamId SRT stream ID
         * @param passPhrase SRT passPhrase
         * @return SRT connection
         */
        fun fromUrlAndParameters(
            url: String,
            streamId: String? = null,
            passPhrase: String? = null
        ): SrtConnectionDescriptor {
            val uri = Uri.parse(url)
            if (uri.scheme != SRT_SCHEME) {
                throw InvalidParameterException("URL $url is not an srt URL")
            }
            val host = uri.host
                ?: throw InvalidParameterException("Failed to parse URL $url: unknown host")
            val port = uri.port

            return SrtConnectionDescriptor(host, port, streamId, passPhrase)
        }
    }
}