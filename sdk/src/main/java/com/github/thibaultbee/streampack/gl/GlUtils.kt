package com.github.thibaultbee.streampack.gl

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