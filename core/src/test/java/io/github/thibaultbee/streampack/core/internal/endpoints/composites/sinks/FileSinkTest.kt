/*
 * Copyright (C) 2024 Thibault B.
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
package io.github.thibaultbee.streampack.core.internal.endpoints.composites.sinks

import android.net.Uri
import io.github.thibaultbee.streampack.core.data.mediadescriptor.UriMediaDescriptor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.fail
import org.junit.Test

class FileSinkTest : AbstractFileSinkTest(FileSink()) {
    @Test
    fun `open not a file test`() = runTest {
        try {
            sink.open(UriMediaDescriptor(Uri.parse("aaa://a/b/c/d")))
            fail("Null file must not be openable")
        } catch (_: Exception) {
        }
    }
}