package io.github.thibaultbee.streampack.core.internal.endpoints

import android.content.Context
import android.media.MediaFormat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.thibaultbee.streampack.core.data.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.internal.utils.DescriptorUtils
import io.github.thibaultbee.streampack.core.internal.utils.FakeFrames
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DynamicEndpointTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `isOpened test`() = runTest {
        val dynamicEndpoint = DynamicEndpoint(context)
        assertFalse(dynamicEndpoint.isOpened.value)
        dynamicEndpoint.open(DescriptorUtils.createFileDescriptor("dynamic.ts"))
        assertTrue(dynamicEndpoint.isOpened.value)
    }

    @Test
    fun `test open mp4 file descriptor`() = runTest {
        val dynamicEndpoint = DynamicEndpoint(context)
        dynamicEndpoint.open(DescriptorUtils.createFileDescriptor("dynamic.mp4"))
        assertTrue(dynamicEndpoint.isOpened.value)
    }

    @Test
    fun `test open ts file descriptor`() = runTest {
        val dynamicEndpoint = DynamicEndpoint(context)
        dynamicEndpoint.open(DescriptorUtils.createFileDescriptor("dynamic.ts"))
        assertTrue(dynamicEndpoint.isOpened.value)
    }

    @Test
    fun `test open flv file descriptor`() = runTest {
        val dynamicEndpoint = DynamicEndpoint(context)
        dynamicEndpoint.open(DescriptorUtils.createFileDescriptor("dynamic.flv"))
        assertTrue(dynamicEndpoint.isOpened.value)
    }

    @Test
    fun `test open unknown file extension`() = runTest {
        val dynamicEndpoint = DynamicEndpoint(context)
        try {
            dynamicEndpoint.open(DescriptorUtils.createFileDescriptor("dynamic.unknown"))
            fail("IllegalArgumentException expected")
        } catch (e: Throwable) {
            assertFalse(dynamicEndpoint.isOpened.value)
        }
    }

    @Test
    fun `test open flv content descriptor`() = runTest {
        val dynamicEndpoint = DynamicEndpoint(context)
        dynamicEndpoint.open(
            UriMediaDescriptor(
                DescriptorUtils.createContentUri(
                    context,
                    "dynamic.flv"
                ), containerType = MediaContainerType.FLV
            )
        )
        assertTrue(dynamicEndpoint.isOpened.value)
    }

    @Test
    fun `test write to non open endpoint`() = runTest {
        val dynamicEndpoint = DynamicEndpoint(context)
        try {
            dynamicEndpoint.write(FakeFrames.generate(MediaFormat.MIMETYPE_AUDIO_AAC), 0)
            fail("Throwable expected")
        } catch (e: Throwable) {
            assertFalse(dynamicEndpoint.isOpened.value)
        }
    }
}