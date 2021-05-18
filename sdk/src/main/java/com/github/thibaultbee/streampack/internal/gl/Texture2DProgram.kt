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

import android.opengl.GLES11Ext
import android.opengl.GLES20
import java.nio.FloatBuffer

/**
 * GL program and supporting functions for textured 2D shapes.
 *
 * (Contains mostly code borrowed from CameraX)
 *
 */
class Texture2DProgram {
    // Handles to the GL program and various components of it.
    private val programHandle: Int
    private val uMVPMatrixLoc: Int
    private val uTexMatrixLoc: Int
    private val aPositionLoc: Int
    private val aTextureCoordLoc: Int

    init {
        programHandle = createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT)
        if (programHandle == 0) {
            throw RuntimeException("Unable to create program")
        }

        // get locations of attributes and uniforms
        aPositionLoc = GLES20.glGetAttribLocation(programHandle, "aPosition")
        checkLocation(aPositionLoc, "aPosition")
        aTextureCoordLoc = GLES20.glGetAttribLocation(programHandle, "aTextureCoord")
        checkLocation(aTextureCoordLoc, "aTextureCoord")
        uMVPMatrixLoc = GLES20.glGetUniformLocation(programHandle, "uMVPMatrix")
        checkLocation(uMVPMatrixLoc, "uMVPMatrix")
        uTexMatrixLoc = GLES20.glGetUniformLocation(programHandle, "uTexMatrix")
        checkLocation(uTexMatrixLoc, "uTexMatrix")
    }

    /**
     * Releases the program.
     *
     *
     * The appropriate EGL context must be current (i.e. the one that was used to create
     * the program).
     */
    fun release() {
        GLES20.glDeleteProgram(programHandle)
    }

    /**
     * Creates a texture object suitable for use with this program.
     * <p>
     * On exit, the texture will be bound.
     */
    fun createTextureObject(): Int {
        val textureID = IntArray(1)
        GLES20.glGenTextures(1, textureID, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureID[0])
        GlUtils.checkGlError("glBindTexture mTextureID")

        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GlUtils.checkGlError("glTexParameter")
        return textureID[0]
    }


    /**
     * Creates a new program from the supplied vertex and fragment shaders.
     *
     * @return A handle to the program, or 0 on failure.
     */
    private fun createProgram(vertexSource: String?, fragmentSource: String?): Int {
        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) {
            return 0
        }
        val pixelShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (pixelShader == 0) {
            return 0
        }
        val program = GLES20.glCreateProgram()
        GlUtils.checkGlError("glCreateProgram")
        if (program == 0) {
            throw Exception("Could not create program")
        }
        GLES20.glAttachShader(program, vertexShader)
        GlUtils.checkGlError("glAttachShader")
        GLES20.glAttachShader(program, pixelShader)
        GlUtils.checkGlError("glAttachShader")
        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            val info = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            throw Exception("Could not link program: $info")
        }
        return program
    }


    /**
     * Compiles the provided shader source.
     *
     * @return A handle to the shader, or 0 on failure.
     */
    private fun loadShader(shaderType: Int, source: String?): Int {
        val shader = GLES20.glCreateShader(shaderType)
        GlUtils.checkGlError("glCreateShader type=$shaderType")
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val info = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw Exception("Could not compile shader $shaderType: $info")
        }
        return shader
    }

    /**
     * Checks to see if the location we obtained is valid.  GLES returns -1 if a label
     * could not be found, but does not set the GL error.
     *
     *
     * Throws a RuntimeException if the location is invalid.
     */
    private fun checkLocation(location: Int, label: String) {
        if (location < 0) {
            throw java.lang.RuntimeException("Unable to locate '$label' in program")
        }
    }

    /**
     * Issues the draw call.  Does the full setup on every call.
     *
     * @param mvpMatrix The 4x4 projection matrix.
     * @param vertexBuffer Buffer with vertex position data.
     * @param firstVertex Index of first vertex to use in vertexBuffer.
     * @param vertexCount Number of vertices in vertexBuffer.
     * @param coordsPerVertex The number of coordinates per vertex (e.g. x,y is 2).
     * @param vertexStride Width, in bytes, of the position data for each vertex (often
     * vertexCount * sizeof(float)).
     * @param texMatrix A 4x4 transformation matrix for texture coords.  (Primarily intended
     * for use with SurfaceTexture.)
     * @param texBuffer Buffer with vertex texture data.
     * @param texStride Width, in bytes, of the texture data for each vertex.
     */
    fun draw(
        mvpMatrix: FloatArray, vertexBuffer: FloatBuffer, firstVertex: Int,
        vertexCount: Int, coordsPerVertex: Int, vertexStride: Int,
        texMatrix: FloatArray, texBuffer: FloatBuffer, textureId: Int, texStride: Int
    ) {
        GlUtils.checkGlError("draw start")

        // Select the program.
        GLES20.glUseProgram(programHandle)
        GlUtils.checkGlError("glUseProgram")

        // Set the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        // Copy the model / view / projection matrix over.
        GLES20.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mvpMatrix, 0)
        GlUtils.checkGlError("glUniformMatrix4fv")

        // Copy the texture transformation matrix over.
        GLES20.glUniformMatrix4fv(uTexMatrixLoc, 1, false, texMatrix, 0)
        GlUtils.checkGlError("glUniformMatrix4fv")

        // Enable the "aPosition" vertex attribute.
        GLES20.glEnableVertexAttribArray(aPositionLoc)
        GlUtils.checkGlError("glEnableVertexAttribArray")

        // Connect vertexBuffer to "aPosition".
        GLES20.glVertexAttribPointer(
            aPositionLoc, coordsPerVertex,
            GLES20.GL_FLOAT, false, vertexStride, vertexBuffer
        )
        GlUtils.checkGlError("glVertexAttribPointer")

        // Enable the "aTextureCoord" vertex attribute.
        GLES20.glEnableVertexAttribArray(aTextureCoordLoc)
        GlUtils.checkGlError("glEnableVertexAttribArray")

        // Connect texBuffer to "aTextureCoord".
        GLES20.glVertexAttribPointer(
            aTextureCoordLoc, 2,
            GLES20.GL_FLOAT, false, texStride, texBuffer
        )
        GlUtils.checkGlError("glVertexAttribPointer")

        // Draw the rect.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount)
        GlUtils.checkGlError("glDrawArrays")

        // Done -- disable vertex array, texture, and program.
        GLES20.glDisableVertexAttribArray(aPositionLoc)
        GLES20.glDisableVertexAttribArray(aTextureCoordLoc)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        GLES20.glUseProgram(0)
    }

    companion object {
        // Simple vertex shader, used for all programs.
        private const val VERTEX_SHADER = """uniform mat4 uMVPMatrix;
    uniform mat4 uTexMatrix;
    attribute vec4 aPosition;
    attribute vec4 aTextureCoord;
    varying vec2 vTextureCoord;
    void main() {
        gl_Position = uMVPMatrix * aPosition;
        vTextureCoord = (uTexMatrix * aTextureCoord).xy;
    }
    """

        // Simple fragment shader for use with external 2D textures (e.g. what we get from
        // SurfaceTexture).
        private const val FRAGMENT_SHADER_EXT = """#extension GL_OES_EGL_image_external : require
    precision mediump float;
    varying vec2 vTextureCoord;
    uniform samplerExternalOES sTexture;
    void main() {
        gl_FragColor = texture2D(sTexture, vTextureCoord);
    }
    """
    }

}