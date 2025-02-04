/*
 * Copyright (C) 2025 Thibault B.
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
package io.github.thibaultbee.streampack.core.utils

import android.Manifest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.cameras
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule

open class DeviceTest(
    private val withCamera: Boolean = true,
    private val withMicrophone: Boolean = true
) {
    protected val context = InstrumentationRegistry.getInstrumentation().context

    @get:Rule
    val runtimePermissionRule: GrantPermissionRule = run {
        val permissions = mutableListOf<String>()
        if (withCamera) {
            permissions.add(Manifest.permission.CAMERA)
        }
        if (withMicrophone) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        GrantPermissionRule.grant(*permissions.toTypedArray())
    }

    @Before
    open fun setUp() {
        if (withCamera) {
            assumeTrue(context.cameras.isNotEmpty())
        }
    }
}