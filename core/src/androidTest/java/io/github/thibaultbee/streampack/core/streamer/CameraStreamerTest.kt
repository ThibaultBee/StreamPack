/*
 * Copyright (C) 2022 Thibault B.
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
package io.github.thibaultbee.streampack.core.streamer

import androidx.core.net.toUri
import io.github.thibaultbee.streampack.core.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.data.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.streamer.testcases.CameraStreamerTestCase
import io.github.thibaultbee.streampack.core.utils.FileUtils
import kotlinx.coroutines.test.runTest
import org.junit.Assert.fail
import org.junit.Test

class TsCameraStreamerTest : CameraStreamerTestCase() {
    override val descriptor: MediaDescriptor =
        UriMediaDescriptor(FileUtils.createCacheFile("video.ts").toUri())

    @Test
    override fun startPreviewTest() = runTest {
        try {
            streamer.startPreview(surface)
        } catch (t: Throwable) {
            fail("Must be possible to only start preview without exception: $t")
        }
    }
}

class FlvCameraStreamerTest : CameraStreamerTestCase() {
    override val descriptor: MediaDescriptor =
        UriMediaDescriptor(FileUtils.createCacheFile("video.flv").toUri())
}

class Mp4CameraStreamerTest : CameraStreamerTestCase() {
    override val descriptor: MediaDescriptor =
        UriMediaDescriptor(FileUtils.createCacheFile("video.mp4").toUri())
}