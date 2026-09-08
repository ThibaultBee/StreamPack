package io.github.thibaultbee.streampack.ui.views

import android.util.Size
import androidx.camera.viewfinder.core.ScaleType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PreviewViewTest {

    @Test
    fun testGetSurfaceToViewMatrix_fitCenter() {
        // Surface is 1000x500 (2:1), View is 1000x1000 (1:1)
        // With FIT_CENTER, the surface should be letterboxed vertically.
        // It will be scaled to 1000x500 and centered vertically -> offset Y = 250
        // Matrix should map (0,0) -> (0, 250)
        // Matrix should map (1,1) -> (1000, 750)

        val matrix = PreviewView.getSurfaceToViewMatrix(
            viewWidth = 1000,
            viewHeight = 1000,
            surfaceSize = Size(1000, 500),
            scaleType = ScaleType.FIT_CENTER
        )

        val point0 = floatArrayOf(0f, 0f)
        matrix.mapPoints(point0)
        assertEquals(0f, point0[0], 0.01f)
        assertEquals(250f, point0[1], 0.01f)

        val point1 = floatArrayOf(1f, 1f)
        matrix.mapPoints(point1)
        assertEquals(1000f, point1[0], 0.01f)
        assertEquals(750f, point1[1], 0.01f)
    }

    @Test
    fun testGetSurfaceToViewMatrix_fitStart() {
        // With FIT_START, it aligns to top-left -> offset Y = 0
        // Matrix should map (0,0) -> (0, 0)
        // Matrix should map (1,1) -> (1000, 500)
        val matrix = PreviewView.getSurfaceToViewMatrix(
            viewWidth = 1000,
            viewHeight = 1000,
            surfaceSize = Size(1000, 500),
            scaleType = ScaleType.FIT_START
        )

        val point0 = floatArrayOf(0f, 0f)
        matrix.mapPoints(point0)
        assertEquals(0f, point0[0], 0.01f)
        assertEquals(0f, point0[1], 0.01f)

        val point1 = floatArrayOf(1f, 1f)
        matrix.mapPoints(point1)
        assertEquals(1000f, point1[0], 0.01f)
        assertEquals(500f, point1[1], 0.01f)
    }

    @Test
    fun testGetSurfaceToViewMatrix_fitEnd() {
        // With FIT_END, it aligns to bottom-right -> offset Y = 500
        // Matrix should map (0,0) -> (0, 500)
        // Matrix should map (1,1) -> (1000, 1000)
        val matrix = PreviewView.getSurfaceToViewMatrix(
            viewWidth = 1000,
            viewHeight = 1000,
            surfaceSize = Size(1000, 500),
            scaleType = ScaleType.FIT_END
        )

        val point0 = floatArrayOf(0f, 0f)
        matrix.mapPoints(point0)
        assertEquals(0f, point0[0], 0.01f)
        assertEquals(500f, point0[1], 0.01f)

        val point1 = floatArrayOf(1f, 1f)
        matrix.mapPoints(point1)
        assertEquals(1000f, point1[0], 0.01f)
        assertEquals(1000f, point1[1], 0.01f)
    }

    @Test
    fun testGetSurfaceToViewMatrix_fillCenter() {
        // Surface is 1000x500 (2:1), View is 1000x1000 (1:1)
        // With FILL_CENTER, the surface must fill the 1000x1000 view.
        // It maintains aspect ratio, so surface is scaled by 2x to 2000x1000.
        // It's centered, so the left/right parts are cropped.
        // The surface [0, 1] width is scaled to 2000, and offset by -500 to center it.
        // Matrix should map (0,0) -> (-500, 0)
        // Matrix should map (1,1) -> (1500, 1000)

        val matrix = PreviewView.getSurfaceToViewMatrix(
            viewWidth = 1000,
            viewHeight = 1000,
            surfaceSize = Size(1000, 500),
            scaleType = ScaleType.FILL_CENTER
        )

        val point0 = floatArrayOf(0f, 0f)
        matrix.mapPoints(point0)
        assertEquals(-500f, point0[0], 0.01f)
        assertEquals(0f, point0[1], 0.01f)

        val point1 = floatArrayOf(1f, 1f)
        matrix.mapPoints(point1)
        assertEquals(1500f, point1[0], 0.01f)
        assertEquals(1000f, point1[1], 0.01f)
    }

    @Test
    fun testGetSurfaceToViewMatrix_fillStart() {
        // With FILL_START, the surface scales to 2000x1000.
        // It aligns to top-left, so offset X = 0.
        // Matrix should map (0,0) -> (0, 0)
        // Matrix should map (1,1) -> (2000, 1000)
        val matrix = PreviewView.getSurfaceToViewMatrix(
            viewWidth = 1000,
            viewHeight = 1000,
            surfaceSize = Size(1000, 500),
            scaleType = ScaleType.FILL_START
        )

        val point0 = floatArrayOf(0f, 0f)
        matrix.mapPoints(point0)
        assertEquals(0f, point0[0], 0.01f)
        assertEquals(0f, point0[1], 0.01f)

        val point1 = floatArrayOf(1f, 1f)
        matrix.mapPoints(point1)
        assertEquals(2000f, point1[0], 0.01f)
        assertEquals(1000f, point1[1], 0.01f)
    }
    
    @Test
    fun testGetSurfaceToViewMatrix_fillEnd() {
        // With FILL_END, the surface scales to 2000x1000.
        // It aligns to bottom-right, so offset X = -1000.
        // Matrix should map (0,0) -> (-1000, 0)
        // Matrix should map (1,1) -> (1000, 1000)
        val matrix = PreviewView.getSurfaceToViewMatrix(
            viewWidth = 1000,
            viewHeight = 1000,
            surfaceSize = Size(1000, 500),
            scaleType = ScaleType.FILL_END
        )

        val point0 = floatArrayOf(0f, 0f)
        matrix.mapPoints(point0)
        assertEquals(-1000f, point0[0], 0.01f)
        assertEquals(0f, point0[1], 0.01f)

        val point1 = floatArrayOf(1f, 1f)
        matrix.mapPoints(point1)
        assertEquals(1000f, point1[0], 0.01f)
        assertEquals(1000f, point1[1], 0.01f)
    }
}
