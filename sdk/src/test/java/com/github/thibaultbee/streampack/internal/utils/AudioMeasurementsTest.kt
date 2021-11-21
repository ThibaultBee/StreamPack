package com.github.thibaultbee.streampack.internal.utils

import android.media.AudioFormat
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

class AudioMeasurementsTest {
    @Test
    fun `Peak for 8 bits per sample buffer in mono`() {
        val audioMeasurement = AudioMeasurements(AudioFormat.ENCODING_PCM_8BIT, 1)

        // First call to peak enable measurement
        var peak = audioMeasurement.peak
        assertEquals(1, peak.size)
        assertEquals(true, peak[0].isInfinite())

        audioMeasurement.onNewBuffer(ByteBuffer.wrap(byteArrayOf(-125, 0, 20, 110)))
        audioMeasurement.onNewBuffer(ByteBuffer.wrap(byteArrayOf(-124, 0, 19, 109)))

        peak = audioMeasurement.peak
        assertEquals(1, peak.size)
        assertEquals(20 * log10(125.toFloat() / (Byte.MAX_VALUE - Byte.MIN_VALUE)), peak[0])
    }

    @Test
    fun `Peak for 8 bits per sample buffer in stereo`() {
        val audioMeasurement = AudioMeasurements(AudioFormat.ENCODING_PCM_8BIT, 2)

        // First call to peak enable measurement
        var peak = audioMeasurement.peak
        assertEquals(2, peak.size)
        assertEquals(true, peak[0].isInfinite())
        assertEquals(true, peak[1].isInfinite())

        audioMeasurement.onNewBuffer(ByteBuffer.wrap(byteArrayOf(-125, 0, 20, 110)))
        audioMeasurement.onNewBuffer(ByteBuffer.wrap(byteArrayOf(-124, 0, 19, 109)))

        peak = audioMeasurement.peak
        assertEquals(2, peak.size)
        assertEquals(20 * log10(125.toFloat() / (Byte.MAX_VALUE - Byte.MIN_VALUE)), peak[0])
        assertEquals(20 * log10(110.toFloat() / (Byte.MAX_VALUE - Byte.MIN_VALUE)), peak[1])
    }

    @Test
    fun `Peak for 16 bits per sample buffer in stereo`() {
        val audioMeasurement = AudioMeasurements(AudioFormat.ENCODING_PCM_16BIT, 2)

        // First call to peak enable measurement
        var peak = audioMeasurement.peak
        assertEquals(2, peak.size)
        assertEquals(true, peak[0].isInfinite())
        assertEquals(true, peak[1].isInfinite())

        val buffer = ByteBuffer.allocate(4 * Short.SIZE_BYTES)
        shortArrayOf(-125, 0, 20, 110).forEach { buffer.putShort(it) }
        buffer.rewind()
        audioMeasurement.onNewBuffer(buffer)

        peak = audioMeasurement.peak
        assertEquals(2, peak.size)
        assertEquals(
            20 * log10(125.toFloat() / (Short.MAX_VALUE - Short.MIN_VALUE.toLong())),
            peak[0]
        )
        assertEquals(
            20 * log10(110.toFloat() / (Short.MAX_VALUE - Short.MIN_VALUE.toLong())),
            peak[1]
        )
    }

    @Test
    fun `Peak for 32 bits per sample buffer in stereo`() {
        val audioMeasurement = AudioMeasurements(AudioFormat.ENCODING_PCM_32BIT, 2)

        // First call to peak enable measurement
        var peak = audioMeasurement.peak
        assertEquals(2, peak.size)
        assertEquals(true, peak[0].isInfinite())
        assertEquals(true, peak[1].isInfinite())

        val buffer = ByteBuffer.allocate(4 * Int.SIZE_BYTES)
        intArrayOf(-125, 0, 20, 110).forEach { buffer.putInt(it) }
        buffer.rewind()
        audioMeasurement.onNewBuffer(buffer)

        peak = audioMeasurement.peak
        assertEquals(2, peak.size)
        assertEquals(20 * log10(125.toFloat() / (Int.MAX_VALUE - Int.MIN_VALUE.toLong())), peak[0])
        assertEquals(20 * log10(110.toFloat() / (Int.MAX_VALUE - Int.MIN_VALUE.toLong())), peak[1])
    }

    @Test
    fun `Rms for 16 bits per sample buffer in stereo`() {
        val audioMeasurement = AudioMeasurements(AudioFormat.ENCODING_PCM_16BIT, 2)

        // First call to peak enable measurement
        var rms = audioMeasurement.rms
        assertEquals(2, rms.size)
        assertEquals(true, rms[0].isNaN())
        assertEquals(true, rms[1].isNaN())

        val buffer = ByteBuffer.allocate(4 * Short.SIZE_BYTES)
        shortArrayOf(-125, 0, 20, 110).forEach { buffer.putShort(it) }
        buffer.rewind()
        audioMeasurement.onNewBuffer(buffer)

        rms = audioMeasurement.rms
        assertEquals(2, rms.size)
        assertEquals(
            20 * log10(
                sqrt(
                    (125.toFloat().pow(2) + 20.toFloat().pow(2)) / 2
                ) * sqrt(2F) / (Short.MAX_VALUE - Short.MIN_VALUE.toLong())
            ), rms[0]
        )
        assertEquals(
            20 * log10(
                sqrt(
                    (0.toFloat().pow(2) + 110.toFloat().pow(2)) / 2
                ) * sqrt(2F) / (Short.MAX_VALUE - Short.MIN_VALUE.toLong())
            ), rms[1]
        )
    }
}