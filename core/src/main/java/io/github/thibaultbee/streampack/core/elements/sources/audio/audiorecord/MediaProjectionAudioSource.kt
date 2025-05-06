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
package io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import androidx.annotation.RequiresApi
import io.github.thibaultbee.streampack.core.elements.sources.IMediaProjectionSource
import io.github.thibaultbee.streampack.core.elements.sources.audio.AudioSourceConfig
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.runBlocking
import java.util.UUID

/**
 * The [MediaProjectionAudioSource] class is an implementation of [IAudioSourceInternal] that
 * captures audio playback.
 *
 * @param mediaProjection The media projection
 */
@RequiresApi(Build.VERSION_CODES.Q)
internal class MediaProjectionAudioSource(
    override val mediaProjection: MediaProjection,
) : AudioRecordSource(), IMediaProjectionSource {
    private val callbackHandlerThread = HandlerThread("AudioProjectionThread").apply { start() }
    private val callbackHandler = Handler(callbackHandlerThread.looper)

    private val mediaProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            Logger.i(TAG, "onStop")

            runBlocking {
                stopStream()
            }
        }
    }

    override fun buildAudioRecord(config: AudioSourceConfig, bufferSize: Int): AudioRecord {
        val audioFormat = AudioFormat.Builder()
            .setEncoding(config.byteFormat)
            .setSampleRate(config.sampleRate)
            .setChannelMask(config.channelConfig)
            .build()

        return AudioRecord.Builder()
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize)
            .setAudioPlaybackCaptureConfig(
                AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            .build()
    }

    override suspend fun startStream() {
        mediaProjection.registerCallback(mediaProjectionCallback, callbackHandler)

        super.startStream()
    }

    override suspend fun stopStream() {
        super.stopStream()

        mediaProjection.unregisterCallback(mediaProjectionCallback)
    }

    override fun release() {
        super.release()
        mediaProjection.unregisterCallback(mediaProjectionCallback)
        callbackHandlerThread.quitSafely()
        try {
            callbackHandlerThread.join()
        } catch (e: InterruptedException) {
            Logger.e(TAG, "Failed to join callback handler thread: $e")
        }
    }

    companion object {
        private const val TAG = "MediaProjectionAudio"
    }
}

/**
 * A factory to create a [MediaProjectionAudioSource].
 *
 * If you want to share the [MediaProjection] to another source. You must use this factory.
 *
 * @param mediaProjection The media projection
 * @param effects The audio effects to apply
 */
@RequiresApi(Build.VERSION_CODES.Q)
class MediaProjectionAudioSourceFactory(
    private val mediaProjection: MediaProjection,
    effects: Set<UUID> = defaultAudioEffects
) : AudioRecordSourceFactory(effects) {
    override suspend fun createImpl(context: Context): AudioRecordSource {
        return MediaProjectionAudioSource(mediaProjection)
    }

    override fun isSourceEquals(source: IAudioSourceInternal?): Boolean {
        return source is MediaProjectionAudioSource
    }
}