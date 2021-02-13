package com.github.thibaultbee.srtstreamer.sources

import android.Manifest
import android.media.AudioRecord
import android.media.AudioTimestamp
import android.media.MediaFormat
import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresPermission
import com.github.thibaultbee.srtstreamer.data.Frame
import com.github.thibaultbee.srtstreamer.utils.Error
import com.github.thibaultbee.srtstreamer.utils.EventHandlerManager
import com.github.thibaultbee.srtstreamer.utils.Logger
import java.nio.ByteBuffer

class AudioCapture(val logger: Logger): EventHandlerManager() {
    private var audioRecord: AudioRecord? = null

    fun configure(sampleRate: Int, channelConfig: Int, audioByteFormat: Int): Error {
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            channelConfig,
            audioByteFormat
        )

        if (bufferSize <= 0) {
            logger.e(this, "Invalid size to start recording")
            return Error.INVALID_PARAMETER
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.DEFAULT, sampleRate,
            channelConfig, audioByteFormat, bufferSize
        )

        return Error.SUCCESS
    }

    fun isConfigured(): Boolean {
        return audioRecord != null
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(): Error {
        if (!isConfigured()) {
            logger.e(this, "Not configured")
            return Error.BAD_STATE
        }

        audioRecord!!.startRecording()

        if (!isRunning()) {
            logger.e(this, "Failed to start recording")
            return Error.UNKNOWN
        }

        return Error.SUCCESS
    }

    fun isRunning() = audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING

    fun stop(): Error {
        if (!isRunning()) {
            logger.e(this, "Not running")
            return Error.BAD_STATE
        }

        // Stop audio record
        audioRecord?.stop()

        return Error.SUCCESS
    }

    fun release(): Error {
        // Release audio record
        audioRecord?.release()
        audioRecord = null

        return Error.SUCCESS
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

    fun getFrame(buffer: ByteBuffer): Frame? {
        val length = audioRecord!!.read(buffer, buffer.remaining())
        return if (length > 0) {
            Frame(buffer, MediaFormat.MIMETYPE_AUDIO_RAW, getTimestamp(audioRecord!!))
        } else {
            reportError(when (length) {
                AudioRecord.ERROR_INVALID_OPERATION -> Error.INVALID_OPERATION
                AudioRecord.ERROR_BAD_VALUE -> Error.INVALID_PARAMETER
                AudioRecord.ERROR_DEAD_OBJECT -> Error.DEAD_OBJECT
                else -> Error.UNKNOWN
            })
            null
        }
    }
}