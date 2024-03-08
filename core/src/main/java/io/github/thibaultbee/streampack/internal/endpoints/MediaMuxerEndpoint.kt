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
package io.github.thibaultbee.streampack.internal.endpoints

import android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME
import android.media.MediaCodec.BufferInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import io.github.thibaultbee.streampack.data.Config
import io.github.thibaultbee.streampack.internal.data.Frame
import java.io.File
import java.io.FileDescriptor
import java.io.OutputStream

/**
 * An [IEndpoint] implementation of the [MediaMuxer].
 */
class MediaMuxerEndpoint : IEndpoint, IFileEndpoint {
    private var mediaMuxer: MediaMuxer? = null
    private var isStarted = false

    /**
     * Map streamId to MediaMuxer trackId
     */
    private val streamIdToTrackId = mutableMapOf<Int, Int>()
    private var numOfStreams = 0

    override val helper = MediaMuxerEndpointHelper

    override var file: File? = null
        set(value) {
            mediaMuxer?.release()
            if (value != null) {
                mediaMuxer =
                    MediaMuxer(value.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            }
            field = value
        }

    override var outputStream: OutputStream? = null
        set(_) {
            throw UnsupportedOperationException("MediaMuxerEndpoint does not support OutputStream")
        }

    override var fileDescriptor: FileDescriptor? = null
        set(value) {
            mediaMuxer?.release()
            if (value != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mediaMuxer = MediaMuxer(value, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                } else {
                    throw UnsupportedOperationException("MediaMuxerEndpoint does not support FileDescriptor on this device")
                }
            }
            field = value
        }

    override fun write(frame: Frame, streamPid: Int) {
        mediaMuxer?.let {
            if (streamIdToTrackId.size < numOfStreams) {
                addTrack(it, streamPid, frame.format)
            }
            if (streamIdToTrackId.size == numOfStreams) {
                if (!isStarted) {
                    it.start()
                    isStarted = true
                }

                val trackId = streamIdToTrackId[streamPid]
                    ?: throw IllegalStateException("Could not find trackId for streamPid $streamPid: ${frame.format}")
                val info = BufferInfo().apply {
                    set(
                        0,
                        frame.buffer.remaining(),
                        frame.pts,
                        if (frame.isKeyFrame) BUFFER_FLAG_KEY_FRAME else 0
                    )
                }
                return it.writeSampleData(trackId, frame.buffer, info)
            } else {
                return
            }
        }
        throw IllegalStateException("MediaMuxer is not initialized")
    }

    private fun addTrack(mediaMuxer: MediaMuxer, streamId: Int, format: MediaFormat) {
        if (streamIdToTrackId[streamId] == null) {
            streamIdToTrackId[streamId] = mediaMuxer.addTrack(format)
        }
    }

    override fun addStreams(streamsConfig: List<Config>): Map<Config, Int> {
        mediaMuxer?.let {
            /**
             * We can't addTrack here because we don't have the codec specific data.
             * We will add it when we receive the first frame.
             */
            return streamsConfig.associateWith { numOfStreams++ }
        }
        throw IllegalStateException("MediaMuxer is not initialized")
    }

    override fun addStream(streamConfig: Config): Int {
        mediaMuxer?.let {
            /**
             * We can't addTrack here because we don't have the codec specific data.
             * We will add it when we receive the first frame.
             */
            return numOfStreams++
        }
        throw IllegalStateException("MediaMuxer is not initialized")
    }

    override suspend fun startStream() {
        require(mediaMuxer != null) { "MediaMuxer is not initialized" }
        /**
         * [MediaMuxer.start] is called when we called addTrack for each stream.
         */
    }

    override suspend fun stopStream() {
        try {
            mediaMuxer?.stop()
        } catch (e: IllegalStateException) {
            // MediaMuxer is already stopped
        } finally {
            numOfStreams = 0
            streamIdToTrackId.clear()
            isStarted = false
        }
    }

    override fun release() {
        mediaMuxer?.release()
    }

    object MediaMuxerEndpointHelper : IEndpoint.IEndpointHelper {
        override val audio = object : IEndpoint.IEndpointHelper.IAudioEndpointHelper {
            override val supportedEncoders = listOf(
                MediaFormat.MIMETYPE_AUDIO_AAC,
                MediaFormat.MIMETYPE_AUDIO_AMR_NB,
                MediaFormat.MIMETYPE_AUDIO_AMR_WB
            )
            override val supportedSampleRates = null
            override val supportedByteFormats = null
        }

        override val video = object : IEndpoint.IEndpointHelper.IVideoEndpointHelper {
            override val supportedEncoders = run {
                mutableListOf(
                    MediaFormat.MIMETYPE_VIDEO_H263,
                    MediaFormat.MIMETYPE_VIDEO_AVC,
                    MediaFormat.MIMETYPE_VIDEO_MPEG4
                ).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        add(MediaFormat.MIMETYPE_VIDEO_HEVC)
                    }
                }
            }
        }
    }
}