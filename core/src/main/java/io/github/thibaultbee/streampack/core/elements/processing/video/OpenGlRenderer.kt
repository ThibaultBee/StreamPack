/*
 * Copyright 2022 The Android Open Source Project
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
package io.github.thibaultbee.streampack.core.elements.processing.video

import android.graphics.Bitmap
import android.graphics.Rect
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.annotation.WorkerThread
import androidx.core.graphics.createBitmap
import androidx.core.util.Pair
import io.github.thibaultbee.streampack.core.elements.processing.video.outputs.SurfaceOutput
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.GLUtils.EMPTY_ATTRIBS
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.GLUtils.InputFormat
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.GLUtils.NO_OUTPUT_SURFACE
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.GLUtils.PIXEL_STRIDE
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.GLUtils.Program2D
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.GLUtils.SamplerShaderProgram
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.GLUtils.checkEglErrorOrLog
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.GLUtils.checkEglErrorOrThrow
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.GLUtils.checkGlErrorOrThrow
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.GLUtils.checkGlThreadOrThrow
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.GLUtils.checkInitializedOrThrow
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.GLUtils.chooseSurfaceAttrib
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.GLUtils.createPBufferSurface
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.GLUtils.createPrograms
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.GLUtils.createTexture
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.GLUtils.createWindowSurface
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.GLUtils.deleteFbo
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.GLUtils.deleteTexture
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.GLUtils.generateFbo
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.GLUtils.generateTexture
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.GLUtils.glVersionNumber
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.GraphicDeviceInfo
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.OutputSurface
import io.github.thibaultbee.streampack.core.elements.utils.av.video.DynamicRangeProfile
import io.github.thibaultbee.streampack.core.logger.Logger
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import javax.microedition.khronos.egl.EGL10

/**
 * OpenGLRenderer renders texture image to the output surface.
 *
 *
 * OpenGLRenderer's methods must run on the same thread, so called GL thread. The GL thread is
 * locked as the thread running the [.init] method, otherwise an
 * [IllegalStateException] will be thrown when other methods are called.
 */
@WorkerThread
class OpenGlRenderer {
    protected val mInitialized: AtomicBoolean = AtomicBoolean(false)
    protected val mOutputSurfaceMap: MutableMap<Surface, OutputSurface> =
        HashMap()
    protected var mGlThread: Thread? = null
    protected var mEglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    protected var mEglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    protected var mSurfaceAttrib: IntArray = EMPTY_ATTRIBS
    protected var mEglConfig: EGLConfig? = null
    protected var mTempSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    protected var mCurrentSurface: Surface? = null
    protected var mProgramHandles: Map<InputFormat, Program2D> = emptyMap()
    protected var mCurrentProgram: Program2D? = null
    protected var mCurrentInputformat: InputFormat = InputFormat.UNKNOWN

    private var mExternalTextureId = -1

    /**
     * Initializes the OpenGLRenderer
     *
     *
     * This is equivalent to calling [.init] without providing any
     * shader overrides. Default shaders will be used for the dynamic range specified.
     */
    fun init(dynamicRange: DynamicRangeProfile): GraphicDeviceInfo {
        return init(dynamicRange, emptyMap<InputFormat?, ShaderProvider>())
    }

    /**
     * Initializes the OpenGLRenderer
     *
     *
     * Initialization must be done before calling other methods, otherwise an
     * [IllegalStateException] will be thrown. Following methods must run on the same
     * thread as this method, so called GL thread, otherwise an [IllegalStateException]
     * will be thrown.
     *
     * @param dynamicRange    the dynamic range used to select default shaders.
     * @param shaderOverrides specific shader overrides for fragment shaders
     * per [InputFormat].
     * @return Info about the initialized graphics device.
     * @throws IllegalStateException    if the renderer is already initialized or failed to be
     * initialized.
     * @throws IllegalArgumentException if the ShaderProvider fails to create shader or provides
     * invalid shader string.
     */
    fun init(
        dynamicRange: DynamicRangeProfile,
        shaderOverrides: Map<InputFormat?, ShaderProvider?>
    ): GraphicDeviceInfo {
        checkInitializedOrThrow(mInitialized, false)
        val infoBuilder = GraphicDeviceInfo.Builder()
        try {
            var dynamicRangeCorrected = dynamicRange
            if (dynamicRange.isHdr) {
                val extensions = getExtensionsBeforeInitialized(dynamicRange)
                val glExtensions = requireNotNull(extensions.first)
                val eglExtensions = requireNotNull(extensions.second)
                if (!glExtensions.contains("GL_EXT_YUV_target")) {
                    Logger.w(TAG, "Device does not support GL_EXT_YUV_target. Fallback to SDR.")
                    dynamicRangeCorrected = DynamicRangeProfile.sdr
                }
                mSurfaceAttrib = chooseSurfaceAttrib(eglExtensions, dynamicRangeCorrected)
                infoBuilder.setGlExtensions(glExtensions)
                infoBuilder.setEglExtensions(eglExtensions)
            }
            createEglContext(dynamicRangeCorrected, infoBuilder)
            createTempSurface()
            makeCurrent(mTempSurface)
            infoBuilder.setGlVersion(glVersionNumber)
            mProgramHandles = createPrograms(dynamicRangeCorrected, shaderOverrides)
            mExternalTextureId = createTexture()
            useAndConfigureProgramWithTexture(mExternalTextureId)
        } catch (e: IllegalStateException) {
            releaseInternal()
            throw e
        } catch (e: IllegalArgumentException) {
            releaseInternal()
            throw e
        }
        mGlThread = Thread.currentThread()
        mInitialized.set(true)
        return infoBuilder.build()
    }

    /**
     * Releases the OpenGLRenderer
     *
     * @throws IllegalStateException if the caller doesn't run on the GL thread.
     */
    fun release() {
        if (!mInitialized.getAndSet(false)) {
            return
        }
        checkGlThreadOrThrow(mGlThread)
        releaseInternal()
    }

    /**
     * Register the output surface.
     *
     * @throws IllegalStateException if the renderer is not initialized or the caller doesn't run
     * on the GL thread.
     */
    fun registerOutputSurface(surface: Surface) {
        checkInitializedOrThrow(mInitialized, true)
        checkGlThreadOrThrow(mGlThread)

        if (!mOutputSurfaceMap.containsKey(surface)) {
            mOutputSurfaceMap[surface] = NO_OUTPUT_SURFACE
        }
    }

    /**
     * Unregister the output surface.
     *
     * @throws IllegalStateException if the renderer is not initialized or the caller doesn't run
     * on the GL thread.
     */
    fun unregisterOutputSurface(surface: Surface) {
        checkInitializedOrThrow(mInitialized, true)
        checkGlThreadOrThrow(mGlThread)

        removeOutputSurfaceInternal(surface, true)
    }

    val textureName: Int
        /**
         * Gets the texture name.
         *
         * @return the texture name
         * @throws IllegalStateException if the renderer is not initialized or the caller doesn't run
         * on the GL thread.
         */
        get() {
            checkInitializedOrThrow(mInitialized, true)
            checkGlThreadOrThrow(mGlThread)

            return mExternalTextureId
        }

    /**
     * Sets the input format.
     *
     *
     * This will ensure the correct sampler is used for the input.
     *
     * @param inputFormat The input format for the input texture.
     * @throws IllegalStateException if the renderer is not initialized or the caller doesn't run
     * on the GL thread.
     */
    fun setInputFormat(inputFormat: InputFormat) {
        checkInitializedOrThrow(mInitialized, true)
        checkGlThreadOrThrow(mGlThread)

        if (mCurrentInputformat !== inputFormat) {
            mCurrentInputformat = inputFormat
            useAndConfigureProgramWithTexture(mExternalTextureId)
        }
    }

    private fun activateExternalTexture(externalTextureId: Int) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        checkGlErrorOrThrow("glActiveTexture")

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, externalTextureId)
        checkGlErrorOrThrow("glBindTexture")
    }

    /**
     * Renders the texture image to the output surface.
     *
     * @throws IllegalStateException if the renderer is not initialized, the caller doesn't run
     * on the GL thread or the surface is not registered by
     * [.registerOutputSurface].
     */
    fun render(
        timestampNs: Long,
        textureTransform: FloatArray,
        surface: Surface,
        viewportRect: Rect
    ) {
        checkInitializedOrThrow(mInitialized, true)
        checkGlThreadOrThrow(mGlThread)

        var outputSurface: OutputSurface? = getOutSurfaceOrThrow(surface)

        // Workaround situations that out surface is failed to create or needs to be recreated.
        if (outputSurface === NO_OUTPUT_SURFACE) {
            outputSurface = createOutputSurfaceInternal(surface, viewportRect)
            if (outputSurface == null) {
                return
            }

            mOutputSurfaceMap[surface] = outputSurface
        }

        requireNotNull(outputSurface)

        // Set output surface.
        if (surface !== mCurrentSurface) {
            makeCurrent(outputSurface.eglSurface)
            mCurrentSurface = surface
            GLES20.glViewport(
                outputSurface.viewPortRect.left,
                outputSurface.viewPortRect.top,
                outputSurface.viewPortRect.width(),
                outputSurface.viewPortRect.height()
            )
            GLES20.glScissor(
                outputSurface.viewPortRect.left,
                outputSurface.viewPortRect.top,
                outputSurface.viewPortRect.width(),
                outputSurface.viewPortRect.height()
            )
        }


        // TODO(b/245855601): Upload the matrix to GPU when textureTransform is changed.
        val program: Program2D = requireNotNull(mCurrentProgram)
        if (program is SamplerShaderProgram) {
            // Copy the texture transformation matrix over.
            program.updateTextureMatrix(textureTransform)
        }

        // Draw the rect.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,  /*firstVertex=*/0,  /*vertexCount=*/4)
        checkGlErrorOrThrow("glDrawArrays")

        // Set timestamp
        EGLExt.eglPresentationTimeANDROID(mEglDisplay, outputSurface.eglSurface, timestampNs)

        // Swap buffer
        if (!EGL14.eglSwapBuffers(mEglDisplay, outputSurface.eglSurface)) {
            Logger.w(
                TAG, "Failed to swap buffers with EGL error: 0x" + Integer.toHexString(
                    EGL14.eglGetError()
                )
            )
            removeOutputSurfaceInternal(surface, false)
        }
    }

    /**
     * Takes a snapshot of the current external texture and returns a Bitmap.
     *
     * @param size             the size of the output [Bitmap].
     * @param textureTransform the transformation matrix.
     * See: [SurfaceOutput.updateTransformMatrix]
     */
    fun snapshot(size: Size, textureTransform: FloatArray): Bitmap {
        // Allocate buffer.
        val byteBuffer = ByteBuffer.allocateDirect(
            size.width * size.height * PIXEL_STRIDE
        )

        // Take a snapshot.
        snapshot(byteBuffer, size, textureTransform)
        byteBuffer.rewind()

        // Create a Bitmap and copy the bytes over.
        val bitmap = createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(byteBuffer)
        return bitmap
    }

    /**
     * Takes a snapshot of the current external texture and stores it in the given byte buffer.
     *
     *
     *  The image is stored as RGBA with pixel stride of 4 bytes and row stride of width * 4
     * bytes.
     *
     * @param byteBuffer       the byte buffer to store the snapshot.
     * @param size             the size of the output image.
     * @param textureTransform the transformation matrix.
     * See: [SurfaceOutput.updateTransformMatrix]
     */
    private fun snapshot(
        byteBuffer: ByteBuffer, size: Size,
        textureTransform: FloatArray
    ) {
        check(byteBuffer.capacity() == size.width * size.height * 4) {
            "ByteBuffer capacity is not equal to width * height * 4."
        }
        check(byteBuffer.isDirect) { "ByteBuffer is not direct." }

        // Create and initialize intermediate texture.
        val texture: Int = generateTexture()
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        checkGlErrorOrThrow("glActiveTexture")
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
        checkGlErrorOrThrow("glBindTexture")
        // Configure the texture.
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, size.width,
            size.height, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, null
        )
        checkGlErrorOrThrow("glTexImage2D")
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR
        )

        // Create FBO.
        val fbo: Int = generateFbo()
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo)
        checkGlErrorOrThrow("glBindFramebuffer")

        // Attach the intermediate texture to the FBO
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D, texture, 0
        )
        checkGlErrorOrThrow("glFramebufferTexture2D")

        // Bind external texture (camera output).
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        checkGlErrorOrThrow("glActiveTexture")
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mExternalTextureId)
        checkGlErrorOrThrow("glBindTexture")

        // Set scissor and viewport.
        mCurrentSurface = null
        GLES20.glViewport(0, 0, size.width, size.height)
        GLES20.glScissor(0, 0, size.width, size.height)

        val program: Program2D = requireNotNull(mCurrentProgram)
        if (program is SamplerShaderProgram) {
            // Upload transform matrix.
            program.updateTextureMatrix(textureTransform)
        }

        // Draw the external texture to the intermediate texture.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,  /*firstVertex=*/0,  /*vertexCount=*/4)
        checkGlErrorOrThrow("glDrawArrays")

        // Read the pixels from the framebuffer
        GLES20.glReadPixels(
            0, 0, size.width, size.height, GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            byteBuffer
        )
        checkGlErrorOrThrow("glReadPixels")

        // Clean up
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        deleteTexture(texture)
        deleteFbo(fbo)
        // Set the external texture to be active.
        activateExternalTexture(mExternalTextureId)
    }

    // Returns a pair of GL extension (first) and EGL extension (second) strings.
    private fun getExtensionsBeforeInitialized(
        dynamicRangeToInitialize: DynamicRangeProfile
    ): Pair<String, String> {
        checkInitializedOrThrow(mInitialized, false)
        try {
            createEglContext(dynamicRangeToInitialize,  /*infoBuilder=*/null)
            createTempSurface()
            makeCurrent(mTempSurface)
            // eglMakeCurrent() has to be called before checking GL_EXTENSIONS.
            val glExtensions = GLES20.glGetString(GLES20.GL_EXTENSIONS)
            val eglExtensions = EGL14.eglQueryString(mEglDisplay, EGL14.EGL_EXTENSIONS)
            return Pair(
                glExtensions ?: "", eglExtensions ?: ""
            )
        } catch (e: IllegalStateException) {
            Logger.w(TAG, "Failed to get GL or EGL extensions: " + e.message, e)
            return Pair("", "")
        } finally {
            releaseInternal()
        }
    }

    private fun createEglContext(
        dynamicRange: DynamicRangeProfile,
        infoBuilder: GraphicDeviceInfo.Builder?
    ) {
        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(mEglDisplay != EGL14.EGL_NO_DISPLAY) { "Unable to get EGL14 display" }
        val version = IntArray(2)
        if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
            mEglDisplay = EGL14.EGL_NO_DISPLAY
            throw IllegalStateException("Unable to initialize EGL14")
        }

        infoBuilder?.setEglVersion(version[0].toString() + "." + version[1])

        val rgbBits = if (dynamicRange.isHdr) 10 else 8
        val alphaBits = if (dynamicRange.isHdr) 2 else 8
        val renderType = if (dynamicRange.isHdr)
            EGLExt.EGL_OPENGL_ES3_BIT_KHR
        else
            EGL14.EGL_OPENGL_ES2_BIT
        // TODO(b/319277249): It will crash on older Samsung devices for HDR video 10-bit
        //  because EGLExt.EGL_RECORDABLE_ANDROID is only supported from OneUI 6.1. We need to
        //  check by GPU Driver version when new OS is release.
        val recordableAndroid =
            if (dynamicRange.isHdr) EGL10.EGL_DONT_CARE else EGL14.EGL_TRUE
        val attribToChooseConfig = intArrayOf(
            EGL14.EGL_RED_SIZE, rgbBits,
            EGL14.EGL_GREEN_SIZE, rgbBits,
            EGL14.EGL_BLUE_SIZE, rgbBits,
            EGL14.EGL_ALPHA_SIZE, alphaBits,
            EGL14.EGL_DEPTH_SIZE, 0,
            EGL14.EGL_STENCIL_SIZE, 0,
            EGL14.EGL_RENDERABLE_TYPE, renderType,
            EGLExt.EGL_RECORDABLE_ANDROID, recordableAndroid,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT or EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        check(
            EGL14.eglChooseConfig(
                mEglDisplay, attribToChooseConfig, 0, configs, 0, configs.size,
                numConfigs, 0
            )
        ) { "Unable to find a suitable EGLConfig" }
        val config = configs[0]
        val attribToCreateContext = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, if (dynamicRange.isHdr) 3 else 2,
            EGL14.EGL_NONE
        )
        val context = EGL14.eglCreateContext(
            mEglDisplay, config, EGL14.EGL_NO_CONTEXT,
            attribToCreateContext, 0
        )
        checkEglErrorOrThrow("eglCreateContext")
        mEglConfig = config
        mEglContext = context

        // Confirm with query.
        val values = IntArray(1)
        EGL14.eglQueryContext(
            mEglDisplay, mEglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, values,
            0
        )
        Log.d(TAG, "EGLContext created, client version " + values[0])
    }

    private fun createTempSurface() {
        mTempSurface = createPBufferSurface(
            mEglDisplay, requireNotNull(mEglConfig),  /*width=*/1,  /*height=*/
            1
        )
    }

    protected fun makeCurrent(eglSurface: EGLSurface) {
        check(
            EGL14.eglMakeCurrent(
                mEglDisplay,
                eglSurface,
                eglSurface,
                mEglContext
            )
        ) { "eglMakeCurrent failed" }
    }

    protected fun useAndConfigureProgramWithTexture(textureId: Int) {
        val program = requireNotNull(mProgramHandles[mCurrentInputformat]) {
            "Unable to configure program for input format: $mCurrentInputformat"
        }
        if (mCurrentProgram !== program) {
            mCurrentProgram = program
            program.use()
            Log.d(
                TAG, ("Using program for input format " + mCurrentInputformat + ": "
                        + mCurrentProgram)
            )
        }

        // Activate the texture
        activateExternalTexture(textureId)
    }

    private fun releaseInternal() {
        // Delete program
        for (program in mProgramHandles.values) {
            program.delete()
        }
        mProgramHandles = emptyMap<InputFormat, Program2D>()
        mCurrentProgram = null

        if (mEglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(
                mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )

            // Destroy EGLSurfaces
            for (outputSurface in mOutputSurfaceMap.values) {
                if (outputSurface.eglSurface != EGL14.EGL_NO_SURFACE) {
                    if (!EGL14.eglDestroySurface(mEglDisplay, outputSurface.eglSurface)) {
                        checkEglErrorOrLog("eglDestroySurface")
                    }
                }
            }
            mOutputSurfaceMap.clear()

            // Destroy temp surface
            if (mTempSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(mEglDisplay, mTempSurface)
                mTempSurface = EGL14.EGL_NO_SURFACE
            }

            // Destroy EGLContext and terminate display
            if (mEglContext != EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(mEglDisplay, mEglContext)
                mEglContext = EGL14.EGL_NO_CONTEXT
            }
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(mEglDisplay)
            mEglDisplay = EGL14.EGL_NO_DISPLAY
        }

        // Reset other members
        mEglConfig = null
        mExternalTextureId = -1
        mCurrentInputformat = InputFormat.UNKNOWN
        mCurrentSurface = null
        mGlThread = null
    }

    protected fun getOutSurfaceOrThrow(surface: Surface): OutputSurface {
        check(mOutputSurfaceMap.containsKey(surface)) {
            "The surface is not registered."
        }

        return requireNotNull(mOutputSurfaceMap[surface])
    }

    protected fun createOutputSurfaceInternal(
        surface: Surface,
        viewportRect: Rect
    ): OutputSurface? {
        val eglSurface = try {
            createWindowSurface(
                mEglDisplay, requireNotNull(mEglConfig), surface,
                mSurfaceAttrib
            )
        } catch (e: IllegalStateException) {
            Logger.w(TAG, "Failed to create EGL surface: " + e.message, e)
            return null
        } catch (e: IllegalArgumentException) {
            Logger.w(TAG, "Failed to create EGL surface: " + e.message, e)
            return null
        }

        return OutputSurface(eglSurface, viewportRect)
    }

    protected fun removeOutputSurfaceInternal(surface: Surface, unregister: Boolean) {
        // Unmake current surface.
        if (mCurrentSurface === surface) {
            mCurrentSurface = null
            makeCurrent(mTempSurface)
        }

        // Remove cached EGL surface.
        val removedOutputSurface: OutputSurface = if (unregister) {
            mOutputSurfaceMap.remove(surface)!!
        } else {
            mOutputSurfaceMap.put(surface, NO_OUTPUT_SURFACE)!!
        }

        // Destroy EGL surface.
        if (removedOutputSurface !== NO_OUTPUT_SURFACE) {
            try {
                EGL14.eglDestroySurface(mEglDisplay, removedOutputSurface.eglSurface)
            } catch (e: RuntimeException) {
                Logger.w(TAG, "Failed to destroy EGL surface: " + e.message, e)
            }
        }
    }

    companion object {
        private const val TAG = "OpenGlRenderer"
    }
}