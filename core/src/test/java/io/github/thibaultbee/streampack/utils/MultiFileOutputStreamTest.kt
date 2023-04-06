package io.github.thibaultbee.streampack.utils

import org.junit.After
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.CountDownLatch


class MultiFileOutputStreamTest {
    @get:Rule
    val rootFolder: TemporaryFolder = TemporaryFolder()
    private lateinit var multiFileOutputStream: MultiFileOutputStream

    @After
    fun tearDown() {
        multiFileOutputStream.close()
    }

    @Test
    fun `write data`() {
        val chunkReadyCountDownLatch = CountDownLatch(3)
        val isLastChunkCountDownLatch = CountDownLatch(1)
        var lastChunkId = 0
        val listener = object : MultiFileOutputStream.Listener {
            override fun onFileCreated(index: Int, isLast: Boolean, file: File) {
                Assert.assertEquals(lastChunkId + 1, index)
                chunkReadyCountDownLatch.countDown()
                if (isLast) {
                    isLastChunkCountDownLatch.countDown()
                }
                lastChunkId = index
            }
        }
        multiFileOutputStream =
            MultiFileOutputStream(rootFolder.newFolder(), DEFAULT_CHUNK_SIZE, "", listener)

        multiFileOutputStream.write(Utils.generateRandomArray(2048))
        multiFileOutputStream.write(Utils.generateRandomArray(2048))
        multiFileOutputStream.write(Utils.generateRandomArray(600))
        multiFileOutputStream.close()

        Assert.assertEquals(0, chunkReadyCountDownLatch.count)
        Assert.assertEquals(0, isLastChunkCountDownLatch.count)
        Assert.assertEquals(3, lastChunkId)
    }

    @Test
    fun `write data size == chunk size`() {
        val countDownLatch = CountDownLatch(4)
        val listener = object : MultiFileOutputStream.Listener {
            override fun onFileCreated(index: Int, isLast: Boolean, file: File) {
                countDownLatch.countDown()
            }
        }
        multiFileOutputStream =
            MultiFileOutputStream(rootFolder.newFolder(), DEFAULT_CHUNK_SIZE, "", listener)

        multiFileOutputStream.write(Utils.generateRandomArray(DEFAULT_CHUNK_SIZE))
        multiFileOutputStream.write(Utils.generateRandomArray(DEFAULT_CHUNK_SIZE))
        multiFileOutputStream.write(Utils.generateRandomArray(DEFAULT_CHUNK_SIZE))
        multiFileOutputStream.close() // Must not create an empty chunk

        Assert.assertEquals(1, countDownLatch.count)
    }

    @Test
    fun `multiple close test`() {
        val countDownLatch = CountDownLatch(3)
        val listener = object : MultiFileOutputStream.Listener {
            override fun onFileCreated(index: Int, isLast: Boolean, file: File) {
                countDownLatch.countDown()
            }
        }
        multiFileOutputStream =
            MultiFileOutputStream(rootFolder.newFolder(), DEFAULT_CHUNK_SIZE, "", listener)
        multiFileOutputStream.write(Utils.generateRandomArray(2048))
        multiFileOutputStream.write(Utils.generateRandomArray(600))
        multiFileOutputStream.close()
        multiFileOutputStream.close()
        multiFileOutputStream.close()
        multiFileOutputStream.close()

        Assert.assertEquals(1, countDownLatch.count)
    }

    @Test
    fun `close without writing data`() {
        val countDownLatch = CountDownLatch(1)
        val listener = object : MultiFileOutputStream.Listener {
            override fun onFileCreated(index: Int, isLast: Boolean, file: File) {
                countDownLatch.countDown()
            }
        }
        multiFileOutputStream =
            MultiFileOutputStream(rootFolder.newFolder(), DEFAULT_CHUNK_SIZE, "", listener)
        multiFileOutputStream.close()

        Assert.assertEquals(1, countDownLatch.count)
    }

    companion object {
        const val DEFAULT_CHUNK_SIZE = 1024L
    }
}