/*
 * Copyright (C) 2026 Thibault B.
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

import android.Manifest
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSource
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import io.github.thibaultbee.streampack.core.pipelines.DispatcherProvider
import io.github.thibaultbee.streampack.core.pipelines.IDispatcherProvider
import io.github.thibaultbee.streampack.core.pipelines.utils.HandlerThreadExecutor
import io.github.thibaultbee.streampack.core.streamers.dual.DualStreamer
import io.github.thibaultbee.streampack.core.streamers.single.SingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.cameraSingleStreamer
import io.github.thibaultbee.streampack.core.utils.InternalStreamPackApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

@OptIn(InternalStreamPackApi::class)
class StreamerReleaseTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @Test
    fun testReleaseRightAfterCreation() = runTest {
        repeat(50) {
            SingleStreamer(context).release()
            DualStreamer(context).release()
        }
    }

    @Test
    fun testFactoryReleasesTheStreamerWhenItFails() = runTest {
        val dispatcherProvider = HandlerRecordingDispatcherProvider()
        try {
            cameraSingleStreamer(
                context,
                audioSourceFactory = FailingAudioSourceFactory(),
                dispatcherProvider = dispatcherProvider
            )
            fail("Factory should have thrown")
        } catch (_: FailingAudioSourceFactory.Failure) {
        }

        assertTrue(dispatcherProvider.handlerExecutors.isNotEmpty())
        dispatcherProvider.handlerExecutors.forEach {
            val thread = it.handler.looper.thread
            thread.join(5_000)
            assertFalse("Thread still running: ${thread.name}", thread.isAlive)
        }
    }

    private class FailingAudioSourceFactory : IAudioSource.Factory {
        class Failure : Exception()

        override suspend fun create(context: Context): IAudioSourceInternal = throw Failure()

        override fun isSourceEquals(source: IAudioSourceInternal?) = false
    }

    /** Keeps the handler threads it creates. */
    private class HandlerRecordingDispatcherProvider(
        private val dispatcherProvider: DispatcherProvider = DispatcherProvider()
    ) : IDispatcherProvider by dispatcherProvider {
        val handlerExecutors = CopyOnWriteArrayList<HandlerThreadExecutor>()

        override fun createVideoHandlerExecutor(componentName: String) =
            dispatcherProvider.createVideoHandlerExecutor(componentName)
                .also { handlerExecutors.add(it) }
    }
}
