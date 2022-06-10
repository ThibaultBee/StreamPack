package io.github.thibaultbee.streampack.internal.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.muxers.mp4.models.SampleDependsOn
import io.github.thibaultbee.streampack.internal.muxers.mp4.models.SampleFlags
import io.github.thibaultbee.streampack.internal.utils.extractArray
import io.github.thibaultbee.streampack.utils.ResourcesUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class TrackRunBoxTest {
    @Test
    fun `write valid trun test`() {
        val expectedBuffer = ResourcesUtils.readMP4ByteBuffer("trun.box")
        val entries = listOf(
            TrackRunBox.Entry(size = 28381),
            TrackRunBox.Entry(size = 889),
            TrackRunBox.Entry(size = 1383),
            TrackRunBox.Entry(size = 1276),
            TrackRunBox.Entry(size = 1167),
            TrackRunBox.Entry(size = 1367),
            TrackRunBox.Entry(size = 1570),
            TrackRunBox.Entry(size = 1206),
            TrackRunBox.Entry(size = 2088),
            TrackRunBox.Entry(size = 1466),
            TrackRunBox.Entry(size = 1741),
            TrackRunBox.Entry(size = 1069),
            TrackRunBox.Entry(size = 1451),
            TrackRunBox.Entry(size = 1393),
            TrackRunBox.Entry(size = 1196),
            TrackRunBox.Entry(size = 1363),
            TrackRunBox.Entry(size = 1667),
            TrackRunBox.Entry(size = 1247),
            TrackRunBox.Entry(size = 2091),
            TrackRunBox.Entry(size = 1475),
            TrackRunBox.Entry(size = 2164),
            TrackRunBox.Entry(size = 1043),
            TrackRunBox.Entry(size = 1324),
            TrackRunBox.Entry(size = 1402),
            TrackRunBox.Entry(size = 1206),
            TrackRunBox.Entry(size = 1366),
            TrackRunBox.Entry(size = 1671),
            TrackRunBox.Entry(size = 1638),
            TrackRunBox.Entry(size = 2223),
            TrackRunBox.Entry(size = 1548)
        )
        val trun = TrackRunBox(
            version = 0,
            dataOffset = 240,
            firstSampleFlags = SampleFlags(
                dependsOn = SampleDependsOn.NO_OTHER,
                isNonSyncSample = false
            ),
            entries = entries
        )
        val buffer = trun.write()
        assertArrayEquals(expectedBuffer.extractArray(), buffer.extractArray())
    }
}