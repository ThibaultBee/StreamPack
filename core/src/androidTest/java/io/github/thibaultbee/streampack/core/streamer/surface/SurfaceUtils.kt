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
package io.github.thibaultbee.streampack.core.streamer.surface

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.test.core.app.ActivityScenario
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlin.coroutines.suspendCoroutine

object SurfaceUtils {
    private const val TAG = "SurfaceUtils"

    private fun createSurfaceViewAsync(
        scenario: ActivityScenario<SurfaceViewTestActivity>,
        onSurfaceCreated: (SurfaceView) -> Unit
    ) {
        scenario.onActivity {
            val callback =
                object : SurfaceHolder.Callback {
                    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
                        Logger.i(TAG, "Surface created")
                        onSurfaceCreated(it.mSurfaceView)
                    }

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int
                    ) {
                        Logger.i(TAG, "Surface changed")
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        Logger.i(TAG, "Surface destroyed")
                    }
                }

            it.addSurface(it.mSurfaceView, callback)
        }
    }

    suspend fun createSurfaceView(scenario: ActivityScenario<SurfaceViewTestActivity>): SurfaceView {
        return suspendCoroutine {
            createSurfaceViewAsync(scenario) { surfaceView ->
                it.resumeWith(Result.success(surfaceView))
            }
        }
    }
}