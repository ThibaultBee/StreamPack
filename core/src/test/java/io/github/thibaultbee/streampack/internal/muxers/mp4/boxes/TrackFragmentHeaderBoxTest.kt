package io.github.thibaultbee.streampack.internal.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.muxers.mp4.models.SampleFlags
import io.github.thibaultbee.streampack.internal.muxers.mp4.models.SampleDependsOn
import io.github.thibaultbee.streampack.internal.utils.extractArray
import io.github.thibaultbee.streampack.utils.ResourcesUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class TrackFragmentHeaderBoxTest {
    @Test
    fun `write valid tfhd test`() {
        val expectedBuffer = ResourcesUtils.readMP4ByteBuffer("tfhd.box")
        val tfhd = TrackFragmentHeaderBox(
            id = 1,
            baseDataOffset = 72077L,
            defaultSampleDuration = 512,
            defaultSampleSize = 28381,
            defaultSampleFlags = SampleFlags(
                dependsOn = SampleDependsOn.OTHERS,
                isNonSyncSample = true
            ),
            durationIsEmpty = false

        )
        val buffer = tfhd.write()
        assertArrayEquals(expectedBuffer.extractArray(), buffer.extractArray())
    }
}