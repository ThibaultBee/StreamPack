/*
 * Copyright 2018 Google Inc. All rights reserved.
 * Copyright 2021 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.internal.gl

import android.opengl.EGL14
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.view.Surface
import java.util.Objects

/**
 * Holds state associated with a Surface used for MediaCodec encoder input.
 * <p>
 * The constructor takes a Surface obtained from MediaCodec.createInputSurface(), and uses that
 * to create an EGL window surface. Calls to eglSwapBuffers() cause a frame of data to be sent
 * to the video encoder.
 *
 * (Contains mostly code borrowed from CameraX)
 */

class EglWindowSurface(
    private val surface: Surface,
    private val useHighBitDepth: Boolean,
    private val displayContext:EglDisplayContext = EglDisplayContext(useHighBitDepth)
) {

    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    companion object {
        private const val EGL_RECORDABLE_ANDROID = 0x3142
    }

    init {
        val surfaceAttribs = intArrayOf(
            EGL14.EGL_NONE
        )
        eglSurface = EGL14.eglCreateWindowSurface(
            displayContext.display, displayContext.config, surface,
            surfaceAttribs, 0
        )
        GlUtils.checkEglError("eglCreateWindowSurface")
    }

    /**
     * Discard all resources held by this class, notably the EGL context. Also releases the
     * Surface that was passed to our constructor.
     */
    fun release() {
        if (Objects.equals(displayContext.display, EGL14.EGL_NO_DISPLAY)) {
            throw UnsupportedOperationException("must release surface before releasing context")
        }
        EGL14.eglDestroySurface(displayContext.display, eglSurface)
        surface.release()
        eglSurface = EGL14.EGL_NO_SURFACE
    }

    /**
     * Makes our EGL context and surface current.
     */
    fun makeCurrent() {
        if (!EGL14.eglMakeCurrent(displayContext.display, eglSurface, eglSurface, displayContext.context)) {
            throw RuntimeException("eglMakeCurrent failed")
        }
    }

    /**
     * Makes our EGL context and surface not current.
     */
    fun makeUnCurrent() {
        if (!EGL14.eglMakeCurrent(
                displayContext.display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )
        ) {
            throw RuntimeException("eglMakeCurrent failed")
        }
    }

    /**
     * Calls eglSwapBuffers. Use this to "publish" the current frame.
     */
    fun swapBuffers(): Boolean {
        return EGL14.eglSwapBuffers(displayContext.display, eglSurface)
    }

    /**
     * Queries the surface's width.
     */
    fun getWidth(): Int {
        val value = IntArray(1)
        EGL14.eglQuerySurface(displayContext.display, eglSurface, EGL14.EGL_WIDTH, value, 0)
        return value[0]
    }

    /**
     * Queries the surface's height.
     */
    fun getHeight(): Int {
        val value = IntArray(1)
        EGL14.eglQuerySurface(displayContext.display, eglSurface, EGL14.EGL_HEIGHT, value, 0)
        return value[0]
    }

    /**
     * Sends the presentation time stamp to EGL. Time is expressed in nanoseconds.
     */
    fun setPresentationTime(nSecs: Long) {
        EGLExt.eglPresentationTimeANDROID(displayContext.display, eglSurface, nSecs)
    }
}