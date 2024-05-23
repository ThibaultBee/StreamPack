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

import io.github.thibaultbee.streampack.ext.srt.data.mediadescriptor.SrtMediaDescriptor
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class SrtMediaDescriptorTest {
    @Test
    fun fromUrl() {
        val url = "srt://broadcast.host.com:1234"
        val connection = SrtMediaDescriptor.fromUrl(url)
        assertEquals("broadcast.host.com", connection.host)
        assertEquals(1234, connection.port)
    }

    @Test
    fun fromIp() {
        val url = "srt://192.168.1.12:1234"
        val connection = SrtMediaDescriptor.fromUrl(url)
        assertEquals("192.168.1.12", connection.host)
        assertEquals(1234, connection.port)
    }

    @Test
    fun fromUrlWithParameters() {
        val url = "srt://host.com:1234?streamid=streamId&passphrase=passPhrase"
        val connection = SrtMediaDescriptor.fromUrl(url)
        assertEquals("host.com", connection.host)
        assertEquals(1234, connection.port)
        assertEquals("streamId", connection.streamId)
        assertEquals("passPhrase", connection.passPhrase)
    }

    @Test
    fun fromUrlWithBadScheme() {
        val url = "srtp://broadcast.host.com:1234"
        try {
            SrtMediaDescriptor.fromUrl(url)
            fail("Should throw an exception")
        } catch (_: Exception) {
        }
    }

    @Test
    fun fromUrlWithUnknownParam() {
        val url = "srt://host.com:1234?streamid=streamId&passphrase=passPhrase&unknown=unknown"
        try {
            SrtMediaDescriptor.fromUrl(url)
        } catch (e: Exception) {
            assertEquals("Failed to parse URL $url: unknown parameter(s): unknown", e.message)
        }
    }
}