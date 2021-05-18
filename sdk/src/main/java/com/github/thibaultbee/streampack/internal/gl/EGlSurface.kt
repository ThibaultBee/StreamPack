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
package com.github.thibaultbee.streampack.internal.gl

import android.opengl.*
import android.view.Surface
import java.util.*

/**
 * Holds state associated with a Surface used for MediaCodec encoder input.
 * <p>
 * The constructor takes a Surface obtained from MediaCodec.createInputSurface(), and uses that
 * to create an EGL window surface. Calls to eglSwapBuffers() cause a frame of data to be sent
 * to the video encoder.
 *
 * (Contains mostly code borrowed from CameraX)
 */

class EGlSurface(private val surface: Surface) {
    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private val configs = arrayOfNulls<EGLConfig>(1)

    private var width = 0
    private var height = 0

    companion object {
        private const val EGL_RECORDABLE_ANDROID = 0x3142
    }

    init {
        eglSetup()
    }

    /**
     * Prepares EGL. We want a GLES 2.0 context and a surface that supports recording.
     */
    private fun eglSetup() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (Objects.equals(eglDisplay, EGL14.EGL_NO_DISPLAY)) {
            throw RuntimeException("unable to get EGL14 display")
        }
        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            throw RuntimeException("unable to initialize EGL14")
        }

        // Configure EGL for recordable and OpenGL ES 2.0.  We want enough RGB bits
        // to minimize artifacts from possible YUV conversion.
        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE
        )
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(
                eglDisplay, attribList, 0, configs, 0, configs.size,
                numConfigs, 0
            )
        ) {
            throw RuntimeException("unable to find RGB888+recordable ES2 EGL config")
        }

        // Configure context for OpenGL ES 2.0.
        val attrib_list = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        eglContext = EGL14.eglCreateContext(
            eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
            attrib_list, 0
        )
        GlUtils.checkEglError("eglCreateContext")

        // Create a window surface, and attach it to the Surface we received.
        createEGLSurface()
        width = getWidth()
        height = getHeight()
    }

    private fun createEGLSurface() {
        val surfaceAttribs = intArrayOf(
            EGL14.EGL_NONE
        )
        eglSurface = EGL14.eglCreateWindowSurface(
            eglDisplay, configs[0], surface,
            surfaceAttribs, 0
        )
        GlUtils.checkEglError("eglCreateWindowSurface")
    }

    /**
     * Discard all resources held by this class, notably the EGL context. Also releases the
     * Surface that was passed to our constructor.
     */
    fun release() {
        if (!Objects.equals(eglDisplay, EGL14.EGL_NO_DISPLAY)) {
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay)
        }
        surface.release()
        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglSurface = EGL14.EGL_NO_SURFACE
    }

    /**
     * Makes our EGL context and surface current.
     */
    fun makeCurrent() {
        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            throw RuntimeException("eglMakeCurrent failed")
        }
    }

    /**
     * Makes our EGL context and surface not current.
     */
    fun makeUnCurrent() {
        if (!EGL14.eglMakeCurrent(
                eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
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
        return EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    /**
     * Queries the surface's width.
     */
    fun getWidth(): Int {
        val value = IntArray(1)
        EGL14.eglQuerySurface(eglDisplay, eglSurface, EGL14.EGL_WIDTH, value, 0)
        return value[0]
    }

    /**
     * Queries the surface's height.
     */
    fun getHeight(): Int {
        val value = IntArray(1)
        EGL14.eglQuerySurface(eglDisplay, eglSurface, EGL14.EGL_HEIGHT, value, 0)
        return value[0]
    }

    /**
     * Sends the presentation time stamp to EGL. Time is expressed in nanoseconds.
     */
    fun setPresentationTime(nSecs: Long) {
        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, nSecs)
    }
}