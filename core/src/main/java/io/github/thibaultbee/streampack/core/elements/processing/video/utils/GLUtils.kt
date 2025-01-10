/*
 * Copyright 2024 The Android Open Source Project
 * Copyright 2024 Thibault B.
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
package io.github.thibaultbee.streampack.core.elements.processing.video.utils

import android.media.MediaFormat
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLES20.glUniformMatrix4fv
import android.opengl.Matrix
import android.util.Size
import android.view.Surface
import io.github.thibaultbee.streampack.core.elements.processing.video.ShaderProvider
import io.github.thibaultbee.streampack.core.elements.utils.av.video.DynamicRangeProfile
import io.github.thibaultbee.streampack.core.logger.Logger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

/**
 * Utility class for OpenGL ES.
 */
object GLUtils {
    /**
     * Unknown version information.
     */
    const val VERSION_UNKNOWN: String = "0.0"

    const val TAG: String = "GLUtils"

    val IDENTITY_MATRIX = create4x4IdentityMatrix()

    const val EGL_GL_COLORSPACE_KHR: Int = 0x309D
    const val EGL_GL_COLORSPACE_BT2020_HLG_EXT: Int = 0x3540

    const val VAR_TEXTURE_COORD: String = "vTextureCoord"
    const val VAR_TEXTURE: String = "sTexture"
    const val PIXEL_STRIDE: Int = 4
    val EMPTY_ATTRIBS: IntArray = intArrayOf(EGL14.EGL_NONE)
    val HLG_SURFACE_ATTRIBS: IntArray = intArrayOf(
        EGL_GL_COLORSPACE_KHR, EGL_GL_COLORSPACE_BT2020_HLG_EXT,
        EGL14.EGL_NONE
    )

    val DEFAULT_VERTEX_SHADER: String = String.format(
        Locale.US,
        ("""uniform mat4 uTexMatrix;
uniform mat4 uTransMatrix;
attribute vec4 aPosition;
attribute vec4 aTextureCoord;
varying vec2 %s;
void main() {
    gl_Position = uTransMatrix * aPosition;
    %s = (uTexMatrix * aTextureCoord).xy;
}
"""), VAR_TEXTURE_COORD, VAR_TEXTURE_COORD
    )

    val HDR_VERTEX_SHADER: String = String.format(
        Locale.US,
        ("""#version 300 es
in vec4 aPosition;
in vec4 aTextureCoord;
uniform mat4 uTexMatrix;
uniform mat4 uTransMatrix;
out vec2 %s;
void main() {
  gl_Position = uTransMatrix * aPosition;
  %s = (uTexMatrix * aTextureCoord).xy;
}
"""), VAR_TEXTURE_COORD, VAR_TEXTURE_COORD
    )

    const val BLANK_VERTEX_SHADER: String = ("uniform mat4 uTransMatrix;\n"
            + "attribute vec4 aPosition;\n"
            + "void main() {\n"
            + "    gl_Position = uTransMatrix * aPosition;\n"
            + "}\n")

    const val BLANK_FRAGMENT_SHADER: String = ("precision mediump float;\n"
            + "uniform float uAlphaScale;\n"
            + "void main() {\n"
            + "    gl_FragColor = vec4(0.0, 0.0, 0.0, uAlphaScale);\n"
            + "}\n")
    val VERTEX_COORDS: FloatArray = floatArrayOf(
        -1.0f, -1.0f,  // 0 bottom left
        1.0f, -1.0f,  // 1 bottom right
        -1.0f, 1.0f,  // 2 top left
        1.0f, 1.0f,  // 3 top right
    )
    val TEX_COORDS: FloatArray = floatArrayOf(
        0.0f, 0.0f,  // 0 bottom left
        1.0f, 0.0f,  // 1 bottom right
        0.0f, 1.0f,  // 2 top left
        1.0f, 1.0f // 3 top right
    )
    const val SIZEOF_FLOAT: Int = 4
    val VERTEX_BUF: FloatBuffer = createFloatBuffer(VERTEX_COORDS)
    val TEX_BUF: FloatBuffer = createFloatBuffer(TEX_COORDS)
    val NO_OUTPUT_SURFACE: OutputSurface = OutputSurface.of(EGL14.EGL_NO_SURFACE, 0, 0)
    private val SHADER_PROVIDER_DEFAULT: ShaderProvider = object : ShaderProvider {
        override fun createFragmentShader(
            samplerVarName: String,
            fragCoordsVarName: String
        ): String {
            return String.format(
                Locale.US,
                ("""#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 %s;
uniform samplerExternalOES %s;
uniform float uAlphaScale;
void main() {
    vec4 src = texture2D(%s, %s);
    gl_FragColor = vec4(src.rgb, src.a * uAlphaScale);
}
"""),
                fragCoordsVarName, samplerVarName, samplerVarName, fragCoordsVarName
            )
        }
    }
    private val SHADER_PROVIDER_HDR_DEFAULT: ShaderProvider = object : ShaderProvider {
        override fun createFragmentShader(
            samplerVarName: String,
            fragCoordsVarName: String
        ): String {
            return String.format(
                Locale.US,
                ("""#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;
uniform samplerExternalOES %s;
uniform float uAlphaScale;
in vec2 %s;
out vec4 outColor;

void main() {
  vec4 src = texture(%s, %s);
  outColor = vec4(src.rgb, src.a * uAlphaScale);
}"""),
                samplerVarName, fragCoordsVarName, samplerVarName, fragCoordsVarName
            )
        }
    }
    private val SHADER_PROVIDER_HDR_YUV: ShaderProvider = object : ShaderProvider {
        override fun createFragmentShader(
            samplerVarName: String,
            fragCoordsVarName: String
        ): String {
            return String.format(
                Locale.US,
                ("""#version 300 es
#extension GL_EXT_YUV_target : require
precision mediump float;
uniform __samplerExternal2DY2YEXT %s;
uniform float uAlphaScale;
in vec2 %s;
out vec4 outColor;

vec3 yuvToRgb(vec3 yuv) {
  const vec3 yuvOffset = vec3(0.0625, 0.5, 0.5);
  const mat3 yuvToRgbColorMat = mat3(
    1.1689f, 1.1689f, 1.1689f,
    0.0000f, -0.1881f, 2.1502f,
    1.6853f, -0.6530f, 0.0000f
  );
  return clamp(yuvToRgbColorMat * (yuv - yuvOffset), 0.0, 1.0);
}

void main() {
  vec3 srcYuv = texture(%s, %s).xyz;
  vec3 srcRgb = yuvToRgb(srcYuv);
  outColor = vec4(srcRgb, uAlphaScale);
}"""),
                samplerVarName, fragCoordsVarName, samplerVarName, fragCoordsVarName
            )
        }
    }

    /**
     * Creates an [EGLSurface].
     */
    fun createWindowSurface(
        eglDisplay: EGLDisplay,
        eglConfig: EGLConfig, surface: Surface, surfaceAttrib: IntArray
    ): EGLSurface {
        // Create a window surface, and attach it to the Surface we received.
        val eglSurface = EGL14.eglCreateWindowSurface(
            eglDisplay, eglConfig, surface,
            surfaceAttrib,  /*offset=*/0
        )
        checkEglErrorOrThrow("eglCreateWindowSurface")
        checkNotNull(eglSurface) { "surface was null" }
        return eglSurface
    }

    /**
     * Creates the vertex or fragment shader.
     */
    fun loadShader(shaderType: Int, source: String): Int {
        val shader = GLES20.glCreateShader(shaderType)
        checkGlErrorOrThrow(
            "glCreateShader type=$shaderType"
        )
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled,  /*offset=*/0)
        if (compiled[0] == 0) {
            Logger.w(
                TAG,
                "Could not compile shader: $source"
            )
            val shaderLog = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw IllegalStateException(
                "Could not compile shader type $shaderType:$shaderLog"
            )
        }
        return shader
    }

    /**
     * Queries the [EGLSurface] information.
     */
    fun querySurface(
        eglDisplay: EGLDisplay, eglSurface: EGLSurface,
        what: Int
    ): Int {
        val value = IntArray(1)
        EGL14.eglQuerySurface(eglDisplay, eglSurface, what, value,  /*offset=*/0)
        return value[0]
    }

    /**
     * Gets the size of [EGLSurface].
     */
    fun getSurfaceSize(
        eglDisplay: EGLDisplay,
        eglSurface: EGLSurface
    ): Size {
        val width = querySurface(eglDisplay, eglSurface, EGL14.EGL_WIDTH)
        val height = querySurface(eglDisplay, eglSurface, EGL14.EGL_HEIGHT)
        return Size(width, height)
    }

    /**
     * Creates a [FloatBuffer].
     */
    fun createFloatBuffer(coords: FloatArray): FloatBuffer {
        val bb = ByteBuffer.allocateDirect(coords.size * SIZEOF_FLOAT)
        bb.order(ByteOrder.nativeOrder())
        val fb = bb.asFloatBuffer()
        fb.put(coords)
        fb.position(0)
        return fb
    }

    /**
     * Creates a new EGL pixel buffer surface.
     */
    fun createPBufferSurface(
        eglDisplay: EGLDisplay,
        eglConfig: EGLConfig, width: Int, height: Int
    ): EGLSurface {
        val surfaceAttrib = intArrayOf(
            EGL14.EGL_WIDTH, width,
            EGL14.EGL_HEIGHT, height,
            EGL14.EGL_NONE
        )
        val eglSurface = EGL14.eglCreatePbufferSurface(
            eglDisplay, eglConfig, surfaceAttrib,  /*offset=*/
            0
        )
        checkEglErrorOrThrow("eglCreatePbufferSurface")
        checkNotNull(eglSurface) { "surface was null" }
        return eglSurface
    }

    /**
     * Creates program objects based on shaders which are appropriate for each input format.
     *
     *
     * Each [InputFormat] may have different sampler requirements based on the dynamic
     * range. For that reason, we create a separate program for each input format, and will switch
     * to the program when the input format changes so we correctly sample the input texture
     * (or no-op, in some cases).
     */
    fun createPrograms(
        dynamicRange: DynamicRangeProfile,
        shaderProviderOverrides: Map<InputFormat?, ShaderProvider?>
    ): Map<InputFormat, Program2D> {
        val programs = HashMap<InputFormat, Program2D>()
        for (inputFormat in InputFormat.entries) {
            val shaderProviderOverride = shaderProviderOverrides[inputFormat]
            var program: Program2D
            if (shaderProviderOverride != null) {
                // Always use the overridden shader provider if present
                program = SamplerShaderProgram(dynamicRange, shaderProviderOverride)
            } else if (inputFormat == InputFormat.YUV || inputFormat == InputFormat.DEFAULT) {
                // Use a default sampler shader for DEFAULT or YUV
                program = SamplerShaderProgram(dynamicRange, inputFormat)
            } else {
                check(inputFormat == InputFormat.UNKNOWN) {
                    "Unhandled input format: $inputFormat"
                }
                if (dynamicRange.isHdr) {
                    // InputFormat is UNKNOWN and we don't know if we need to use a
                    // YUV-specific sampler for HDR. Use a blank shader program.
                    program = BlankShaderProgram()
                } else {
                    // If we're not rendering HDR content, we can use the default sampler shader
                    // program since it can handle both YUV and DEFAULT inputs when the format
                    // is UNKNOWN.
                    val defaultShaderProviderOverride =
                        shaderProviderOverrides[InputFormat.DEFAULT]
                    program = if (defaultShaderProviderOverride != null) {
                        SamplerShaderProgram(
                            dynamicRange,
                            defaultShaderProviderOverride
                        )
                    } else {
                        SamplerShaderProgram(dynamicRange, InputFormat.DEFAULT)
                    }
                }
            }
            Logger.d(
                TAG, ("Shader program for input format " + inputFormat + " created: "
                        + program)
            )
            programs[inputFormat] = program
        }
        return programs
    }

    /**
     * Creates a texture.
     */
    fun createTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        checkGlErrorOrThrow("glGenTextures")

        val texId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texId)
        checkGlErrorOrThrow(
            "glBindTexture $texId"
        )

        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_NEAREST
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        checkGlErrorOrThrow("glTexParameter")
        return texId
    }

    /**
     * Creates a 4x4 identity matrix.
     */
    fun create4x4IdentityMatrix(): FloatArray {
        val matrix = FloatArray(16)
        Matrix.setIdentityM(matrix,  /* smOffset= */0)
        return matrix
    }

    /**
     * Checks the location error.
     */
    fun checkLocationOrThrow(location: Int, label: String) {
        check(location >= 0) { "Unable to locate '$label' in program" }
    }

    /**
     * Checks the egl error and throw.
     */
    fun checkEglErrorOrThrow(op: String) {
        val error = EGL14.eglGetError()
        check(error == EGL14.EGL_SUCCESS) { op + ": EGL error: 0x" + Integer.toHexString(error) }
    }

    /**
     * Checks the gl error and throw.
     */
    fun checkGlErrorOrThrow(op: String) {
        val error = GLES20.glGetError()
        check(error == GLES20.GL_NO_ERROR) { op + ": GL error 0x" + Integer.toHexString(error) }
    }

    /**
     * Checks the egl error and log.
     */
    fun checkEglErrorOrLog(op: String) {
        try {
            checkEglErrorOrThrow(op)
        } catch (e: IllegalStateException) {
            Logger.e(TAG, e.toString(), e)
        }
    }

    /**
     * Checks the initialization status.
     */
    fun checkInitializedOrThrow(
        initialized: AtomicBoolean,
        shouldInitialized: Boolean
    ) {
        val result = shouldInitialized == initialized.get()
        val message = if (shouldInitialized)
            "OpenGlRenderer is not initialized"
        else
            "OpenGlRenderer is already initialized"
        check(result) { message }
    }

    /**
     * Checks the gl thread.
     */
    fun checkGlThreadOrThrow(thread: Thread?) {
        check(thread === Thread.currentThread()) {
            "Method call must be called on the GL thread."
        }
    }

    val glVersionNumber: String
        /**
         * Gets the gl version number.
         */
        get() {
            // Logic adapted from CTS Egl14Utils:
            // https://cs.android.com/android/platform/superproject/+/master:cts/tests/tests/opengl/src/android/opengl/cts/Egl14Utils.java;l=46;drc=1c705168ab5118c42e5831cd84871d51ff5176d1
            val glVersion = GLES20.glGetString(GLES20.GL_VERSION)
            val pattern = Pattern.compile("OpenGL ES ([0-9]+)\\.([0-9]+).*")
            val matcher = pattern.matcher(glVersion)
            if (matcher.find()) {
                val major = requireNotNull(matcher.group(1))
                val minor = requireNotNull(matcher.group(2))
                return "$major.$minor"
            }
            return VERSION_UNKNOWN
        }

    /**
     * Chooses the surface attributes for HDR 10bit.
     */
    fun chooseSurfaceAttrib(
        eglExtensions: String,
        dynamicRange: DynamicRangeProfile
    ): IntArray {
        var attribs = EMPTY_ATTRIBS
        if (dynamicRange.transferFunction == MediaFormat.COLOR_TRANSFER_HLG) {
            if (eglExtensions.contains("EGL_EXT_gl_colorspace_bt2020_hlg")) {
                attribs = HLG_SURFACE_ATTRIBS
            } else {
                Logger.w(
                    TAG, ("Dynamic range uses HLG encoding, but "
                            + "device does not support EGL_EXT_gl_colorspace_bt2020_hlg."
                            + "Fallback to default colorspace.")
                )
            }
        }
        // TODO(b/303675500): Add path for PQ (EGL_EXT_gl_colorspace_bt2020_pq) output for
        //  HDR10/HDR10+
        return attribs
    }

    /**
     * Generates framebuffer object.
     */
    fun generateFbo(): Int {
        val fbos = IntArray(1)
        GLES20.glGenFramebuffers(1, fbos, 0)
        checkGlErrorOrThrow("glGenFramebuffers")
        return fbos[0]
    }

    /**
     * Generates texture.
     */
    fun generateTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        checkGlErrorOrThrow("glGenTextures")
        return textures[0]
    }

    /**
     * Deletes texture.
     */
    fun deleteTexture(texture: Int) {
        val textures = intArrayOf(texture)
        GLES20.glDeleteTextures(1, textures, 0)
        checkGlErrorOrThrow("glDeleteTextures")
    }

    /**
     * Deletes framebuffer object.
     */
    fun deleteFbo(fbo: Int) {
        val fbos = intArrayOf(fbo)
        GLES20.glDeleteFramebuffers(1, fbos, 0)
        checkGlErrorOrThrow("glDeleteFramebuffers")
    }

    private fun getFragmentShaderSource(shaderProvider: ShaderProvider): String {
        // Throw IllegalArgumentException if the shader provider can not provide a valid
        // fragment shader.
        try {
            val source = shaderProvider.createFragmentShader(VAR_TEXTURE, VAR_TEXTURE_COORD)
            // A simple check to workaround custom shader doesn't contain required variable.
            // See b/241193761.
            require(
                !(source == null || !source.contains(VAR_TEXTURE_COORD) || !source.contains(
                    VAR_TEXTURE
                ))
            ) { "Invalid fragment shader" }
            return source
        } catch (t: Throwable) {
            if (t is IllegalArgumentException) {
                throw t
            }
            throw IllegalArgumentException("Unable retrieve fragment shader source", t)
        }
    }

    enum class InputFormat {
        /**
         * Input texture format is unknown.
         *
         *
         * When the input format is unknown, HDR content may require rendering blank frames
         * since we are not sure what type of sampler can be used. For SDR content, it is
         * typically safe to use samplerExternalOES since this can handle both RGB and YUV inputs
         * for SDR content.
         */
        UNKNOWN,

        /**
         * Input texture format is the default format.
         *
         *
         * The texture format may be RGB or YUV. For SDR content, using samplerExternalOES is
         * safe since it will be able to convert YUV to RGB automatically within the shader. For
         * HDR content, the input is expected to be RGB.
         */
        DEFAULT,

        /**
         * Input format is explicitly YUV.
         *
         *
         * This needs to be specified for HDR content. Only __samplerExternal2DY2YEXT should be
         * used for HDR YUV content as samplerExternalOES may not correctly convert to RGB.
         */
        YUV
    }

    abstract class Program2D protected constructor(
        vertexShaderSource: String,
        fragmentShaderSource: String
    ) {
        protected var mProgramHandle: Int = 0
        protected var mTransMatrixLoc: Int = -1
        protected var mAlphaScaleLoc: Int = -1
        protected var mPositionLoc: Int = -1

        init {
            var vertexShader = -1
            var fragmentShader = -1
            var program = -1
            try {
                vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderSource)
                fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSource)
                program = GLES20.glCreateProgram()
                checkGlErrorOrThrow("glCreateProgram")
                GLES20.glAttachShader(program, vertexShader)
                checkGlErrorOrThrow("glAttachShader")
                GLES20.glAttachShader(program, fragmentShader)
                checkGlErrorOrThrow("glAttachShader")
                GLES20.glLinkProgram(program)
                val linkStatus = IntArray(1)
                GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus,  /*offset=*/0)
                check(linkStatus[0] == GLES20.GL_TRUE) {
                    "Could not link program: " + GLES20.glGetProgramInfoLog(
                        program
                    )
                }
                mProgramHandle = program
            } catch (e: IllegalStateException) {
                if (vertexShader != -1) {
                    GLES20.glDeleteShader(vertexShader)
                }
                if (fragmentShader != -1) {
                    GLES20.glDeleteShader(fragmentShader)
                }
                if (program != -1) {
                    GLES20.glDeleteProgram(program)
                }
                throw e
            } catch (e: IllegalArgumentException) {
                if (vertexShader != -1) {
                    GLES20.glDeleteShader(vertexShader)
                }
                if (fragmentShader != -1) {
                    GLES20.glDeleteShader(fragmentShader)
                }
                if (program != -1) {
                    GLES20.glDeleteProgram(program)
                }
                throw e
            }

            loadLocations()
        }

        /**
         * Use this shader program
         */
        open fun use() {
            // Select the program.
            GLES20.glUseProgram(mProgramHandle)
            checkGlErrorOrThrow("glUseProgram")

            // Enable the "aPosition" vertex attribute.
            GLES20.glEnableVertexAttribArray(mPositionLoc)
            checkGlErrorOrThrow("glEnableVertexAttribArray")

            // Connect vertexBuffer to "aPosition".
            val coordsPerVertex = 2
            val vertexStride = 0
            GLES20.glVertexAttribPointer(
                mPositionLoc, coordsPerVertex, GLES20.GL_FLOAT,  /*normalized=*/
                false, vertexStride, VERTEX_BUF
            )
            checkGlErrorOrThrow("glVertexAttribPointer")

            // Set to default value for single camera case
            updateTransformMatrix(create4x4IdentityMatrix())
            updateAlpha(1.0f)
        }

        /**
         * Updates the global transform matrix
         */
        fun updateTransformMatrix(transformMat: FloatArray) {
            glUniformMatrix4fv(
                mTransMatrixLoc,  /*count=*/
                1,  /*transpose=*/false, transformMat,  /*offset=*/
                0
            )
            checkGlErrorOrThrow("glUniformMatrix4fv")
        }

        /**
         * Updates the alpha of the drawn frame
         */
        fun updateAlpha(alpha: Float) {
            GLES20.glUniform1f(mAlphaScaleLoc, alpha)
            checkGlErrorOrThrow("glUniform1f")
        }

        /**
         * Delete the shader program
         *
         *
         * Once called, this program should no longer be used.
         */
        fun delete() {
            GLES20.glDeleteProgram(mProgramHandle)
        }

        protected open fun loadLocations() {
            mPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition")
            checkLocationOrThrow(mPositionLoc, "aPosition")
            mTransMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTransMatrix")
            checkLocationOrThrow(mTransMatrixLoc, "uTransMatrix")
            mAlphaScaleLoc = GLES20.glGetUniformLocation(mProgramHandle, "uAlphaScale")
            checkLocationOrThrow(mAlphaScaleLoc, "uAlphaScale")
        }
    }

    class SamplerShaderProgram(
        dynamicRange: DynamicRangeProfile,
        shaderProvider: ShaderProvider
    ) : Program2D(
        if (dynamicRange.isHdr) HDR_VERTEX_SHADER else DEFAULT_VERTEX_SHADER,
        getFragmentShaderSource(shaderProvider)
    ) {
        private var mSamplerLoc = -1
        private var mTexMatrixLoc = -1
        private var mTexCoordLoc = -1

        constructor(
            dynamicRange: DynamicRangeProfile,
            inputFormat: InputFormat
        ) : this(dynamicRange, resolveDefaultShaderProvider(dynamicRange, inputFormat))

        init {
            loadLocations()
        }

        override fun use() {
            super.use()
            // Initialize the sampler to the correct texture unit offset
            GLES20.glUniform1i(mSamplerLoc, 0)

            // Enable the "aTextureCoord" vertex attribute.
            GLES20.glEnableVertexAttribArray(mTexCoordLoc)
            checkGlErrorOrThrow("glEnableVertexAttribArray")

            // Connect texBuffer to "aTextureCoord".
            val coordsPerTex = 2
            val texStride = 0
            GLES20.glVertexAttribPointer(
                mTexCoordLoc, coordsPerTex, GLES20.GL_FLOAT,  /*normalized=*/
                false, texStride, TEX_BUF
            )
            checkGlErrorOrThrow("glVertexAttribPointer")
        }

        /**
         * Updates the texture transform matrix
         */
        fun updateTextureMatrix(textureMat: FloatArray) {
            glUniformMatrix4fv(
                mTexMatrixLoc,  /*count=*/1,  /*transpose=*/false,
                textureMat,  /*offset=*/0
            )
            checkGlErrorOrThrow("glUniformMatrix4fv")
        }

        override fun loadLocations() {
            super.loadLocations()
            mSamplerLoc = GLES20.glGetUniformLocation(mProgramHandle, VAR_TEXTURE)
            checkLocationOrThrow(mSamplerLoc, VAR_TEXTURE)
            mTexCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord")
            checkLocationOrThrow(mTexCoordLoc, "aTextureCoord")
            mTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix")
            checkLocationOrThrow(mTexMatrixLoc, "uTexMatrix")
        }

        companion object {
            private fun resolveDefaultShaderProvider(
                dynamicRange: DynamicRangeProfile,
                inputFormat: InputFormat?
            ): ShaderProvider {
                if (dynamicRange.isHdr) {
                    check(inputFormat != InputFormat.UNKNOWN) {
                        "No default sampler shader available for $inputFormat"
                    }
                    if (inputFormat == InputFormat.YUV) {
                        return SHADER_PROVIDER_HDR_YUV
                    }
                    return SHADER_PROVIDER_HDR_DEFAULT
                } else {
                    return SHADER_PROVIDER_DEFAULT
                }
            }
        }
    }

    class BlankShaderProgram : Program2D(BLANK_VERTEX_SHADER, BLANK_FRAGMENT_SHADER)
}