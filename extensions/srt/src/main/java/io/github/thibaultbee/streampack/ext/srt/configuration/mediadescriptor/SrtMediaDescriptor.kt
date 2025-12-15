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
package io.github.thibaultbee.streampack.ext.srt.configuration.mediadescriptor

import android.net.Uri
import io.github.thibaultbee.srtdroid.core.models.SrtUrl
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.createDefaultTsServiceInfo
import io.github.thibaultbee.streampack.core.elements.endpoints.MediaContainerType
import io.github.thibaultbee.streampack.core.elements.endpoints.MediaSinkType
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.ts.data.TSServiceInfo
import java.security.InvalidParameterException
import androidx.core.net.toUri

/**
 * Creates a SRT connection descriptor from an [descriptor].
 *
 * If the descriptor is already a [SrtMediaDescriptor], it will be returned as is.
 * If the descriptor is an [UriMediaDescriptor], it will be converted to a [SrtMediaDescriptor] with default [TSServiceInfo].
 * Otherwise, an [InvalidParameterException] will be thrown.
 */
fun SrtMediaDescriptor(descriptor: MediaDescriptor): SrtMediaDescriptor =
    when (descriptor) {
        is SrtMediaDescriptor -> descriptor
        is UriMediaDescriptor -> {
            val serviceInfo = descriptor.getCustomData(TSServiceInfo::class.java)
                ?: createDefaultTsServiceInfo()
            SrtMediaDescriptor(descriptor.uri, serviceInfo)
        }

        else -> throw InvalidParameterException("Invalid descriptor ${descriptor::class.java.simpleName} for SRT")
    }


/**
 * Creates a SRT connection descriptor from an [String]
 *
 * @param uriString the srt server uri
 * @param serviceInfo the TS service information
 */
fun SrtMediaDescriptor(
    uriString: String,
    serviceInfo: TSServiceInfo = createDefaultTsServiceInfo()
) =
    SrtMediaDescriptor.fromUrl(uriString, serviceInfo = serviceInfo)

/**
 * Creates a SRT connection descriptor from an [Uri]
 *
 * @param uri the srt server uri
 * @param serviceInfo the TS service information
 */
fun SrtMediaDescriptor(uri: Uri, serviceInfo: TSServiceInfo = createDefaultTsServiceInfo()) =
    SrtMediaDescriptor.fromUri(uri, serviceInfo = serviceInfo)

/**
 * Creates a SRT connection descriptor from parameters
 *
 * @param host the server ip or hostname
 * @param port the server port
 * @param streamId the SRT stream ID
 * @param passPhrase the SRT passphrase
 * @param latency the SRT latency in ms
 * @param connectionTimeout the SRT connection timeout in ms
 * @param serviceInfo the TS service information
 */
fun SrtMediaDescriptor(
    host: String,
    port: Int,
    streamId: String? = null,
    passPhrase: String? = null,
    latency: Int? = null,
    connectionTimeout: Int? = null,
    serviceInfo: TSServiceInfo = createDefaultTsServiceInfo()
) = SrtMediaDescriptor(
    SrtUrl(
        hostname = host,
        port = port,
        connectTimeoutInMs = connectionTimeout,
        null,
        null,
        null,
        null,
        latencyInMs = latency,
        null,
        null,
        null,
        null,
        null,
        null,
        passphrase = passPhrase,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        streamId = streamId,
        null,
        null,
        null,
        null,
        null
    ), serviceInfo
)

/**
 * A convenient implementation of [MediaDescriptor] for SRT connection parameters.
 *
 * @param srtUrl the SRT url
 * @param serviceInfo the TS service information
 */
class SrtMediaDescriptor(
    val srtUrl: SrtUrl,
    serviceInfo: TSServiceInfo = createDefaultTsServiceInfo()
) : MediaDescriptor(
    Type(MediaContainerType.TS, MediaSinkType.SRT),
    listOf(serviceInfo)
) {
    override val uri: Uri = srtUrl.srtUri.toString().toUri()

    /**
     * The SRT host
     */
    val host by lazy { srtUrl.hostname }

    /**
     * The SRT port
     */
    val port by lazy { srtUrl.port }

    /**
     * The SRT stream ID
     */
    val streamId by lazy { srtUrl.streamId }

    /**
     * The SRT passphrase
     */
    val passphrase by lazy { srtUrl.passphrase }

    companion object {
        /**
         * Creates a SRT connection descriptor from an URL
         *
         * @param url server url (syntax: srt://host:port?streamid=streamId&passphrase=passPhrase)
         * @return SRT connection descriptor
         */
        internal fun fromUrl(
            url: String,
            serviceInfo: TSServiceInfo = createDefaultTsServiceInfo()
        ) =
            fromUri(url.toUri(), serviceInfo)

        /**
         * Creates a SRT connection descriptor from an Uri
         *
         * @param uri server uri
         * @return SRT connection descriptor
         */
        internal fun fromUri(
            uri: Uri,
            serviceInfo: TSServiceInfo = createDefaultTsServiceInfo()
        ): SrtMediaDescriptor {
            val srtUrl = SrtUrl(uri)

            return SrtMediaDescriptor(
                srtUrl = srtUrl,
                serviceInfo = serviceInfo
            )
        }
    }
}
