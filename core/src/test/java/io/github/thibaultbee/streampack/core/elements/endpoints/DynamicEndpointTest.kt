package io.github.thibaultbee.streampack.core.elements.endpoints

import android.content.Context
import android.media.MediaFormat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.elements.utils.DescriptorUtils
import io.github.thibaultbee.streampack.core.elements.utils.FakeFramesWithCloseable
import kotlinx.coroutines.Dispatchers
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
    fun `isOpenFlow test`() = runTest {
        val dynamicEndpoint = DynamicEndpoint(context, Dispatchers.Default, Dispatchers.IO)
        assertFalse(dynamicEndpoint.isOpenFlow.value)
        dynamicEndpoint.open(DescriptorUtils.createFileDescriptor("dynamic.ts"))
        assertTrue(dynamicEndpoint.isOpenFlow.value)
        dynamicEndpoint.close()
    }

    @Test
    fun `test open mp4 file descriptor`() = runTest {
        val dynamicEndpoint = DynamicEndpoint(context, Dispatchers.Default, Dispatchers.IO)
        dynamicEndpoint.open(DescriptorUtils.createFileDescriptor("dynamic.mp4"))
        assertTrue(dynamicEndpoint.isOpenFlow.value)
        dynamicEndpoint.close()
    }

    @Test
    fun `test open ts file descriptor`() = runTest {
        val dynamicEndpoint = DynamicEndpoint(context, Dispatchers.Default, Dispatchers.IO)
        dynamicEndpoint.open(DescriptorUtils.createFileDescriptor("dynamic.ts"))
        assertTrue(dynamicEndpoint.isOpenFlow.value)
        dynamicEndpoint.close()
    }

    @Test
    fun `test open flv file descriptor`() = runTest {
        val dynamicEndpoint = DynamicEndpoint(context, Dispatchers.Default, Dispatchers.IO)
        dynamicEndpoint.open(DescriptorUtils.createFileDescriptor("dynamic.flv"))
        assertTrue(dynamicEndpoint.isOpenFlow.value)
        dynamicEndpoint.close()
    }

    @Test
    fun `test open unknown file extension`() = runTest {
        val dynamicEndpoint = DynamicEndpoint(context, Dispatchers.Default, Dispatchers.IO)
        try {
            dynamicEndpoint.open(DescriptorUtils.createFileDescriptor("dynamic.unknown"))
            fail("IllegalArgumentException expected")
        } catch (_: Throwable) {
            assertFalse(dynamicEndpoint.isOpenFlow.value)
        } finally {
            dynamicEndpoint.close()
        }
    }

    @Test
    fun `test open flv content descriptor`() = runTest {
        val dynamicEndpoint = DynamicEndpoint(context, Dispatchers.Default, Dispatchers.IO)
        dynamicEndpoint.open(
            UriMediaDescriptor(
                DescriptorUtils.createContentUri(
                    context,
                    "dynamic.flv"
                ), containerType = MediaContainerType.FLV
            )
        )
        assertTrue(dynamicEndpoint.isOpenFlow.value)
        dynamicEndpoint.close()
    }

    @Test
    fun `test write to non open endpoint`() = runTest {
        val dynamicEndpoint = DynamicEndpoint(context, Dispatchers.Default, Dispatchers.IO)
        try {
            dynamicEndpoint.write(
                FakeFramesWithCloseable.create(MediaFormat.MIMETYPE_AUDIO_AAC),
                0
            )
            fail("Throwable expected")
        } catch (_: Throwable) {
            assertFalse(dynamicEndpoint.isOpenFlow.value)
        } finally {
            dynamicEndpoint.close()
        }
    }
}