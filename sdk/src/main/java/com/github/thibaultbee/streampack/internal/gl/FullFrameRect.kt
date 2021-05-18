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

import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Size
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer


/**
 * This class essentially represents a viewport-sized sprite that will be rendered with
 * a texture, usually from an external source like the camera or video decoder.
 *
 * (Contains mostly code borrowed from graphika)
 */
class FullFrameRect(var program: Texture2DProgram) {
    private val mvpMatrix = FloatArray(16)

    companion object {
        /**
         * A "full" square, extending from -1 to +1 in both dimensions. When the
         * model/view/projection matrix is identity, this will exactly cover the viewport.
         */
        private val FULL_RECTANGLE_COORDS = floatArrayOf(
            -1.0f, -1.0f,  // 0 bottom left
            1.0f, -1.0f,  // 1 bottom right
            -1.0f, 1.0f,  // 2 top left
            1.0f, 1.0f
        )

        private val FULL_RECTANGLE_BUF: FloatBuffer = createFloatBuffer(FULL_RECTANGLE_COORDS)

        private val FULL_RECTANGLE_TEX_COORDS = floatArrayOf(
            0.0f, 0.0f,  // 0 bottom left
            1.0f, 0.0f,  // 1 bottom right
            0.0f, 1.0f,  // 2 top left
            1.0f, 1.0f // 3 top right
        )
        private val FULL_RECTANGLE_TEX_BUF: FloatBuffer =
            createFloatBuffer(FULL_RECTANGLE_TEX_COORDS)

        /**
         * Allocates a direct float buffer, and populates it with the float array data.
         */
        private fun createFloatBuffer(coords: FloatArray): FloatBuffer {
            // Allocate a direct ByteBuffer, using 4 bytes per float, and copy coords into it.
            val bb: ByteBuffer = ByteBuffer.allocateDirect(coords.size * Float.SIZE_BYTES)
            bb.order(ByteOrder.nativeOrder())
            val fb: FloatBuffer = bb.asFloatBuffer()
            fb.put(coords)
            fb.position(0)
            return fb
        }
    }

    /**
     * Releases resources.
     *
     *
     * This must be called with the appropriate EGL context current (i.e. the one that was
     * current when the constructor was called).  If we're about to destroy the EGL context,
     * there's no value in having the caller make it current just to do this cleanup, so you
     * can pass a flag that will tell this function to skip any EGL-context-specific cleanup.
     */
    fun release(doEglCleanup: Boolean) {
        if (doEglCleanup) {
            program.release()
        }
    }

    /**
     * Changes the program.  The previous program will be released.
     *
     *
     * The appropriate EGL context must be current.
     */
    fun changeProgram(program: Texture2DProgram) {
        this.program.release()
        this.program = program
    }

    /**
     * Creates a texture object suitable for use with drawFrame().
     */
    fun createTextureObject(): Int {
        return program.createTextureObject()
    }

    fun setMVPMatrixAndViewPort(rotation: Float, resolution: Size) {
        Matrix.setIdentityM(mvpMatrix, 0)
        Matrix.rotateM(
            mvpMatrix, 0,
            if (rotation == 0F) 270F else rotation - 90, 0f, 0f, -1f
        )
        GLES20.glViewport(0, 0, resolution.width, resolution.height)
    }

    /**
     * Draws a viewport-filling rect, texturing it with the specified texture object.
     */
    fun drawFrame(textureId: Int, texMatrix: FloatArray) {
        // Use the identity matrix for MVP so our 2x2 FULL_RECTANGLE covers the viewport.
        program.draw(
            mvpMatrix, FULL_RECTANGLE_BUF, 0,
            4, 2, 2 * Float.SIZE_BYTES,
            texMatrix, FULL_RECTANGLE_TEX_BUF, textureId, 2 * Float.SIZE_BYTES
        )
    }
}