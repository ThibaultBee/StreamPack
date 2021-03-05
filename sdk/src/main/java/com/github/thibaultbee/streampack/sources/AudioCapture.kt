package com.github.thibaultbee.streampack.sources

import android.Manifest
import android.media.AudioRecord
import android.media.AudioTimestamp
import android.media.MediaFormat
import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresPermission
import com.github.thibaultbee.streampack.data.AudioConfig
import com.github.thibaultbee.streampack.data.Frame
import com.github.thibaultbee.streampack.utils.Logger
import java.nio.ByteBuffer

class AudioCapture(val logger: Logger) : ICapture {
    private var audioRecord: AudioRecord? = null

    fun set(audioConfig: AudioConfig) {
        val bufferSize = AudioRecord.getMinBufferSize(
            audioConfig.sampleRate,
            audioConfig.channelConfig,
            audioConfig.audioByteFormat
        )

        if (bufferSize <= 0) {
            throw IllegalArgumentException(audioRecordErrorToString(bufferSize))
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.DEFAULT, audioConfig.sampleRate,
            audioConfig.channelConfig, audioConfig.audioByteFormat, bufferSize
        )
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun run() {
        audioRecord?.let {
            it.startRecording()

            if (!isRunning()) {
                throw IllegalStateException("AudioCapture: failed to start recording")
            }
        } ?: throw IllegalStateException("AudioCapture: run: : No audioRecorder")
    }

    private fun isRunning() = audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING

    override fun stop() {
        if (!isRunning()) {
            logger.d(this, "Not running")
            return
        }

        // Stop audio record
        audioRecord?.stop()
    }

    override fun close() {
        // Release audio record
        audioRecord?.release()
        audioRecord = null
    }

    private fun getTimestamp(audioRecord: AudioRecord): Long {
        // Get timestamp from AudioRecord
        // If we can not get timestamp through getTimestamp, we timestamp audio sample.
        val timestampOut = AudioTimestamp()
        var timestamp: Long = -1
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (audioRecord.getTimestamp(
                    timestampOut,
                    AudioTimestamp.TIMEBASE_BOOTTIME
                ) == AudioRecord.SUCCESS
            ) {
                timestamp = timestampOut.nanoTime / 1000 // to us
            }
        }
        // Fallback
        if (timestamp < 0) {
            timestamp = System.currentTimeMillis() * 1000 // to us
        }

        return timestamp
    }

    override fun getFrame(buffer: ByteBuffer): Frame {
        audioRecord?.let {
            val length = it.read(buffer, buffer.remaining())
            return if (length > 0) {
                Frame(buffer, MediaFormat.MIMETYPE_AUDIO_RAW, getTimestamp(it))
            } else {
                throw IllegalArgumentException(audioRecordErrorToString(length))
            }
        } ?: throw IllegalStateException("AudioCapture: getFrame: No audioRecorder")
    }

    private fun audioRecordErrorToString(audioRecordError: Int) = when (audioRecordError) {
        AudioRecord.ERROR_INVALID_OPERATION -> "AudioRecord returns an invalid operation error"
        AudioRecord.ERROR_BAD_VALUE -> "AudioRecord returns a bad value error"
        AudioRecord.ERROR_DEAD_OBJECT -> "AudioRecord returns a dead object error"
        else -> "Unknown audio record error"
    }
}