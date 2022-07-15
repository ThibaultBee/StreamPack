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
package io.github.thibaultbee.streampack.internal.encoders

import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import io.github.thibaultbee.streampack.error.StreamPackError
import io.github.thibaultbee.streampack.internal.data.Frame
import io.github.thibaultbee.streampack.internal.events.EventHandler
import io.github.thibaultbee.streampack.internal.utils.extractArray
import io.github.thibaultbee.streampack.internal.utils.slices
import io.github.thibaultbee.streampack.internal.utils.startsWith
import io.github.thibaultbee.streampack.logger.ILogger
import java.nio.ByteBuffer
import java.security.InvalidParameterException

abstract class MediaCodecEncoder<T>(
    override val encoderListener: IEncoderListener,
    val logger: ILogger
) :
    EventHandler(), IEncoder<T> {
    protected var mediaCodec: MediaCodec? = null
    private var callbackThread: HandlerThread? = null
    private var handler: Handler? = null
    private val lock = Object()
    private var isStopped = true
    private var isOnError = false

    protected var _bitrate = 0
    var bitrate: Int = 0
        get() = _bitrate
        set(value) {
            val bundle = Bundle()
            bundle.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, value)
            mediaCodec?.setParameters(bundle)
            field = value
            _bitrate = value
        }

    private val encoderCallback = object : MediaCodec.Callback() {
        override fun onOutputBufferAvailable(
            codec: MediaCodec,
            index: Int,
            info: MediaCodec.BufferInfo
        ) {
            synchronized(lock) {
                if (isStopped) {
                    return
                }
                if (isOnError) {
                    return
                }

                try {
                    mediaCodec?.getOutputBuffer(index)?.let { buffer ->
                        val isKeyFrame = info.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME
                        /**
                         * Drops codec data. They are already passed in the extra buffer.
                         */
                        if (info.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                            val extra = if (isKeyFrame) {
                                generateExtra(codec.outputFormat)
                            } else {
                                null
                            }

                            // Remove startCode + sps + startCode + pps
                            var frameBuffer = buffer
                            extra?.let { it ->
                                var prefix = ByteArray(0)
                                it.forEach { csd -> prefix = prefix.plus(csd.extractArray()) }
                                if (buffer.startsWith(prefix)) {
                                    buffer.position(prefix.size)
                                    frameBuffer = buffer.slice()
                                }
                            }
                            Frame(
                                frameBuffer,
                                codec.outputFormat.getString(MediaFormat.KEY_MIME)!!,
                                info.presentationTimeUs, // pts
                                null, // dts
                                isKeyFrame,
                                extra
                            ).let { frame ->
                                encoderListener.onOutputFrame(
                                    frame
                                )
                            }
                        }

                        mediaCodec?.releaseOutputBuffer(index, false)
                    }
                        ?: reportError(StreamPackError(UnsupportedOperationException("MediaCodecEncoder: can't get output buffer")))
                } catch (e: IllegalStateException) {
                    isOnError = true
                    logger.w(this, "onOutputBufferAvailable called while stopped")
                } catch (e: StreamPackError) {
                    isOnError = true
                    reportError(e)
                }
            }
        }

        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
            /**
             * An IllegalStateException happens when MediaCodec is stopped. Dirty fix: catch it...
             */
            synchronized(lock) {
                if (isStopped) {
                    return
                }
                if (isOnError) {
                    return
                }

                try {
                    mediaCodec?.getInputBuffer(index)?.let { buffer ->
                        encoderListener.onInputFrame(buffer).let { frame ->
                            mediaCodec?.queueInputBuffer(
                                index,
                                0,
                                frame.buffer.remaining(),
                                frame.pts /* in us */,
                                0
                            )
                        }
                    }
                        ?: reportError(
                            StreamPackError(
                                UnsupportedOperationException("MediaCodecEncoder: can't get input buffer")
                            )
                        )
                } catch (e: IllegalStateException) {
                    isOnError = true
                    logger.w(this, "onInputBufferAvailable called while stopped")
                } catch (e: StreamPackError) {
                    isOnError = true
                    reportError(e)
                }
            }
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            logger.i(this, "Format changed : $format")
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
            reportError(StreamPackError(e))
        }
    }

    private fun createHandler(name: String) {
        callbackThread = HandlerThread(name)
        handler = callbackThread?.let { handlerThread ->
            handlerThread.start()
            Handler(handlerThread.looper)
        }
    }

    protected fun createCodec(format: MediaFormat): MediaCodec {
        logger.d(this, "createCodec: $format")
        val mediaCodecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        val encoderName = mediaCodecList.findEncoderForFormat(format)
        encoderName?.let { return MediaCodec.createByCodecName(encoderName) }
            ?: throw InvalidParameterException("Failed to create ${encoderName ?: "unknown"} codec for: $format")
    }

    protected fun configureCodec(codec: MediaCodec, format: MediaFormat, handlerName: String) {
        // Apply configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            createHandler(handlerName)
            codec.setCallback(encoderCallback, handler)
        } else {
            codec.setCallback(encoderCallback)
        }

        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        } catch (e: Exception) {
            codec.release()
            throw e
        }
    }

    override val mimeType: String?
        get() = mediaCodec?.outputFormat?.getString(MediaFormat.KEY_MIME)
            ?: throw IllegalStateException("Can't get MimeType without configuration")

    override fun startStream() {
        synchronized(lock) {
            isOnError = false
            isStopped = false
            mediaCodec?.start() ?: throw IllegalStateException("Can't start without configuration")
        }
    }

    override fun stopStream() {
        try {
            synchronized(lock) {
                isStopped = true
                mediaCodec?.setCallback(null)
                mediaCodec?.signalEndOfInputStream()
                mediaCodec?.flush()
                mediaCodec?.stop()
            }
        } catch (e: IllegalStateException) {
            logger.d(this, "Not running")
        }
    }

    override fun release() {
        mediaCodec?.release()
        mediaCodec = null
    }

    private fun generateExtra(format: MediaFormat): List<ByteBuffer> {
        val extra = mutableListOf<ByteBuffer>()


        format.getByteBuffer("csd-0")?.let {
            /**
             * For HEVC, vps, sps amd pps are all in csd-0.
             * They all start with a start code 0x00000001.
             */
            if (format.getString(MediaFormat.KEY_MIME) == MediaFormat.MIMETYPE_VIDEO_HEVC) {
                extra.addAll(it.slices(byteArrayOf(0x00, 0x00, 0x00, 0x01)))
            } else {
                extra.add(it.duplicate())
            }
        }
        format.getByteBuffer("csd-1")?.let {
            extra.add(it.duplicate())
        }
        format.getByteBuffer("csd-2")?.let {
            extra.add(it.duplicate())
        }


        return extra
    }
}