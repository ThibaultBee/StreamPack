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

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout

class SurfaceViewTestActivity : Activity() {
    lateinit var mSurfaceView: SurfaceView
    lateinit var mFrameLayout: FrameLayout
    lateinit var mLayoutParams: FrameLayout.LayoutParams

    companion object {
        var DEFAULT_WIDTH = 100
        var DEFAULT_HEIGHT = 100
    }

    private var mDestroyCallback: (() -> Unit)? = null

    fun setDestroyCallback(callback: () -> Unit) {
        mDestroyCallback = callback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mLayoutParams =
            FrameLayout.LayoutParams(DEFAULT_WIDTH, DEFAULT_HEIGHT, Gravity.LEFT or Gravity.TOP)
        mLayoutParams.topMargin = 100
        mLayoutParams.leftMargin = 100

        mFrameLayout = FrameLayout(this)
        mSurfaceView = SurfaceView(this)
        mSurfaceView.holder.setFixedSize(DEFAULT_WIDTH, DEFAULT_HEIGHT)
        setContentView(mFrameLayout)
    }

    fun addSurface(surfaceView: SurfaceView, callback: SurfaceHolder.Callback) {
        surfaceView.holder.addCallback(callback)
        mFrameLayout.addView(surfaceView, mLayoutParams)
    }

    fun getSurfaceView(): SurfaceView {
        return mSurfaceView
    }

    override fun onDestroy() {
        super.onDestroy()
        mDestroyCallback?.invoke()
    }
}
/*
class SurfaceViewTestActivity : Activity() {
    private var _surfaceView: SurfaceView? = null
    private lateinit var frameLayout: FrameLayout
    private lateinit var layoutParams: FrameLayout.LayoutParams

    val surfaceView: SurfaceView
        get() = _surfaceView!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        layoutParams =
            FrameLayout.LayoutParams(DEFAULT_WIDTH, DEFAULT_HEIGHT, Gravity.LEFT or Gravity.TOP)
        layoutParams.topMargin = 100
        layoutParams.leftMargin = 100

        frameLayout = FrameLayout(this)

        _surfaceView = SurfaceView(this).apply {
            holder.setFixedSize(DEFAULT_WIDTH, DEFAULT_HEIGHT)
        }

        setContentView(frameLayout)
    }

    fun addSurface(surfaceView: SurfaceView, callback: SurfaceHolder.Callback) {
        surfaceView.holder.addCallback(callback)
        frameLayout.addView(surfaceView, layoutParams)
    }

    companion object {
        private const val DEFAULT_WIDTH = 100
        private const val DEFAULT_HEIGHT = 100
    }
}*/