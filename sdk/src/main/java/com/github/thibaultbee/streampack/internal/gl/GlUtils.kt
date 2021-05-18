/*
 * Copyright (C) 2021 Thibault B.
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
package com.github.thibaultbee.streampack.internal.gl

import android.opengl.EGL14
import android.opengl.GLES20


object GlUtils {
    /**
     * Checks for EGL errors.  Throws an exception if one is found.
     */
    fun checkEglError(msg: String) {
        var error: Int
        if (EGL14.eglGetError().also { error = it } != EGL14.EGL_SUCCESS) {
            throw RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error))
        }
    }

    /**
     * Checks to see if a GLES error has been raised.
     */
    fun checkGlError(op: String) {
        val error = GLES20.glGetError()
        if (error == GLES20.GL_OUT_OF_MEMORY) {
            throw RuntimeException("$op GL_OUT_OF_MEMORY")
        }
        if (error != GLES20.GL_NO_ERROR && error != GLES20.GL_OUT_OF_MEMORY) {
            val msg = op + ": glError 0x" + Integer.toHexString(error)
            throw RuntimeException(msg)
        }
    }

}