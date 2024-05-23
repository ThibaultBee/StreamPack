package io.github.thibaultbee.streampack.internal.utils

import io.github.thibaultbee.streampack.utils.Utils
import org.junit.After
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.CountDownLatch


class ChunkedFileOutputStreamTest {
    @get:Rule
    val rootFolder: TemporaryFolder = TemporaryFolder()
    private lateinit var chunkFileOutputStream: ChunkedFileOutputStream

    @After
    fun tearDown() {
        chunkFileOutputStream.close()
    }

    @Test
    fun `write data larger than chunk`() {
        val chunkReadyCountDownLatch = CountDownLatch(3)
        val isLastChunkCountDownLatch = CountDownLatch(1)
        var nextChunkId = 0
        val listener = object : ChunkedFileOutputStream.Listener {
            override fun onFileClosed(index: Int, isLast: Boolean, file: File) {
                Assert.assertEquals(nextChunkId, index)
                chunkReadyCountDownLatch.countDown()
                if (isLast) {
                    isLastChunkCountDownLatch.countDown()
                }
                nextChunkId = index + 1
            }
        }

        val folder = rootFolder.newFolder()
        chunkFileOutputStream =
            ChunkedFileOutputStream(folder, 2).apply { addListener(listener) }

        chunkFileOutputStream.write(byteArrayOf(1, 2, 3))
        chunkFileOutputStream.write(byteArrayOf(4, 5, 6, 7))

        chunkFileOutputStream.close()

        // Check listener
        Assert.assertEquals(0, chunkReadyCountDownLatch.count)
        Assert.assertEquals(0, isLastChunkCountDownLatch.count)
        Assert.assertEquals(4, nextChunkId)

        // Check files
        Assert.assertEquals(4, folder.listFiles()?.size)
        Assert.assertArrayEquals(byteArrayOf(1, 2), File(folder, "chunk_0").readBytes())
        Assert.assertArrayEquals(byteArrayOf(3, 4), File(folder, "chunk_1").readBytes())
        Assert.assertArrayEquals(byteArrayOf(5, 6), File(folder, "chunk_2").readBytes())
        Assert.assertArrayEquals(byteArrayOf(7), File(folder, "chunk_3").readBytes())
    }

    @Test
    fun `write data == chunk`() {
        val countDownLatch = CountDownLatch(4)
        val listener = object : ChunkedFileOutputStream.Listener {
            override fun onFileClosed(index: Int, isLast: Boolean, file: File) {
                countDownLatch.countDown()
            }
        }

        val folder = rootFolder.newFolder()
        chunkFileOutputStream =
            ChunkedFileOutputStream(folder, 2).apply { addListener(listener) }

        chunkFileOutputStream.write(byteArrayOf(1, 2))
        chunkFileOutputStream.write(byteArrayOf(3, 4))
        chunkFileOutputStream.write(byteArrayOf(5, 6))

        chunkFileOutputStream.close() // Must not create an empty chunk

        // Check listener
        Assert.assertEquals(1, countDownLatch.count)

        // Check files
        Assert.assertEquals(3, folder.listFiles()?.size)
        Assert.assertArrayEquals(byteArrayOf(1, 2), File(folder, "chunk_0").readBytes())
        Assert.assertArrayEquals(byteArrayOf(3, 4), File(folder, "chunk_1").readBytes())
        Assert.assertArrayEquals(byteArrayOf(5, 6), File(folder, "chunk_2").readBytes())
    }

    @Test
    fun `write data smaller than chunk`() {
        val chunkReadyCountDownLatch = CountDownLatch(3)
        val isLastChunkCountDownLatch = CountDownLatch(1)
        var nextChunkId = 0
        val listener = object : ChunkedFileOutputStream.Listener {
            override fun onFileClosed(index: Int, isLast: Boolean, file: File) {
                Assert.assertEquals(nextChunkId, index)
                chunkReadyCountDownLatch.countDown()
                if (isLast) {
                    isLastChunkCountDownLatch.countDown()
                }
                nextChunkId = index + 1
            }
        }

        val folder = rootFolder.newFolder()
        chunkFileOutputStream =
            ChunkedFileOutputStream(folder, 2).apply { addListener(listener) }

        chunkFileOutputStream.write(byteArrayOf(1))
        chunkFileOutputStream.write(byteArrayOf(2))
        chunkFileOutputStream.write(byteArrayOf(3))
        chunkFileOutputStream.write(byteArrayOf(4))
        chunkFileOutputStream.write(byteArrayOf(5))
        chunkFileOutputStream.write(byteArrayOf(6))
        chunkFileOutputStream.write(byteArrayOf(7))

        chunkFileOutputStream.close()

        // Check listener
        Assert.assertEquals(0, chunkReadyCountDownLatch.count)
        Assert.assertEquals(0, isLastChunkCountDownLatch.count)
        Assert.assertEquals(4, nextChunkId)

        // Check files
        Assert.assertEquals(4, folder.listFiles()?.size)
        Assert.assertArrayEquals(byteArrayOf(1, 2), File(folder, "chunk_0").readBytes())
        Assert.assertArrayEquals(byteArrayOf(3, 4), File(folder, "chunk_1").readBytes())
        Assert.assertArrayEquals(byteArrayOf(5, 6), File(folder, "chunk_2").readBytes())
        Assert.assertArrayEquals(byteArrayOf(7), File(folder, "chunk_3").readBytes())
    }

    @Test
    fun `write single int`() {
        val countDownLatch = CountDownLatch(4)
        val listener = object : ChunkedFileOutputStream.Listener {
            override fun onFileClosed(index: Int, isLast: Boolean, file: File) {
                countDownLatch.countDown()
            }
        }

        val folder = rootFolder.newFolder()
        chunkFileOutputStream =
            ChunkedFileOutputStream(folder, 2).apply { addListener(listener) }

        chunkFileOutputStream.write(1)
        chunkFileOutputStream.write(2)
        chunkFileOutputStream.write(3)
        chunkFileOutputStream.write(4)
        chunkFileOutputStream.write(5)

        chunkFileOutputStream.close() // Must not create an empty chunk

        // Check listener
        Assert.assertEquals(1, countDownLatch.count)

        // Check files
        Assert.assertEquals(3, folder.listFiles()?.size)
        Assert.assertArrayEquals(byteArrayOf(1, 2), File(folder, "chunk_0").readBytes())
        Assert.assertArrayEquals(byteArrayOf(3, 4), File(folder, "chunk_1").readBytes())
        Assert.assertArrayEquals(byteArrayOf(5), File(folder, "chunk_2").readBytes())
    }

    @Test
    fun `multiple close test`() {
        val countDownLatch = CountDownLatch(3)
        val listener = object : ChunkedFileOutputStream.Listener {
            override fun onFileClosed(index: Int, isLast: Boolean, file: File) {
                countDownLatch.countDown()
            }
        }

        val folder = rootFolder.newFolder()
        chunkFileOutputStream =
            ChunkedFileOutputStream(folder, 16).apply { addListener(listener) }

        chunkFileOutputStream.write(Utils.generateRandomArray(8))
        chunkFileOutputStream.write(Utils.generateRandomArray(16))
        chunkFileOutputStream.close()
        chunkFileOutputStream.close()
        chunkFileOutputStream.close()
        chunkFileOutputStream.close()

        // Check listener
        Assert.assertEquals(1, countDownLatch.count)

        // Check files
        Assert.assertEquals(2, folder.listFiles()?.size)
    }

    @Test
    fun `close without writing data`() {
        val countDownLatch = CountDownLatch(1)
        val listener = object : ChunkedFileOutputStream.Listener {
            override fun onFileClosed(index: Int, isLast: Boolean, file: File) {
                countDownLatch.countDown()
            }
        }
        chunkFileOutputStream =
            ChunkedFileOutputStream(rootFolder.newFolder(), 16).apply { addListener(listener) }

        chunkFileOutputStream.close()

        Assert.assertEquals(1, countDownLatch.count)
    }
}