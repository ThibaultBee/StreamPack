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
package io.github.thibaultbee.streampack.ext.rtmp.data

import io.github.thibaultbee.streampack.ext.rtmp.data.mediadescriptor.RtmpMediaDescriptor
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class RtmpMediaDescriptorTest {
    @Test
    fun fromUrl() {
        val url = "rtmp://broadcast.host.com:1234/app/streamKey"
        val connection = RtmpMediaDescriptor.fromUrl(url)
        assertEquals("rtmp", connection.scheme)
        assertEquals("broadcast.host.com", connection.host)
        assertEquals(1234, connection.port)
        assertEquals("app", connection.app)
        assertEquals("streamKey", connection.streamKey)
    }

    @Test
    fun fromRtmpsUrl() {
        val url = "rtmps://broadcast.host.com:1234/app/streamKey"
        val connection = RtmpMediaDescriptor.fromUrl(url)
        assertEquals("rtmps", connection.scheme)
        assertEquals("broadcast.host.com", connection.host)
        assertEquals(1234, connection.port)
        assertEquals("app", connection.app)
        assertEquals("streamKey", connection.streamKey)
    }

    @Test
    fun fromUrlWithDefaultPort() {
        val url = "rtmp://broadcast.host.com/app/streamKey"
        val connection = RtmpMediaDescriptor.fromUrl(url)
        assertEquals("rtmp", connection.scheme)
        assertEquals("broadcast.host.com", connection.host)
        assertEquals(1935, connection.port)
        assertEquals("app", connection.app)
        assertEquals("streamKey", connection.streamKey)
    }

    @Test
    fun fromRtmpsUrlWithDefaultPort() {
        val url = "rtmps://broadcast.host.com/app/streamKey"
        val connection = RtmpMediaDescriptor.fromUrl(url)
        assertEquals("rtmps", connection.scheme)
        assertEquals("broadcast.host.com", connection.host)
        assertEquals(443, connection.port)
        assertEquals("app", connection.app)
        assertEquals("streamKey", connection.streamKey)
    }

    @Test
    fun fromUrlWithoutApp() {
        val url = "rtmp://broadcast.host.com:1234/streamKey"
        val connection = RtmpMediaDescriptor.fromUrl(url)
        assertEquals("rtmp", connection.scheme)
        assertEquals("broadcast.host.com", connection.host)
        assertEquals(1234, connection.port)
        assertEquals(null, connection.app)
        assertEquals("streamKey", connection.streamKey)
    }

    @Test
    fun fromUrlWithBadScheme() {
        val url = "rtp://broadcast.host.com:1234/app/streamKey"
        try {
            RtmpMediaDescriptor.fromUrl(url)
            Assert.fail("Should throw an exception")
        } catch (_: Throwable) {
        }
    }
}