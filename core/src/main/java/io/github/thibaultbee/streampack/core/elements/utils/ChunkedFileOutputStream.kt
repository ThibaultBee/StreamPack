/*
 * Copyright (C) 2022 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.core.elements.utils

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * A class that allows to write into multiple files.
 * It is useful when you want to upload a file to a server but you don't want to wait for the record
 * to finish before.
 *
 * @param filesDir the directory where the files will be written
 * @param chunkSize the size of each file in bytes
 * @param chunkNameGenerator generate the name of each file from its index
 */
class ChunkedFileOutputStream(
    val filesDir: File,
    private val chunkSize: Int,
    private val chunkNameGenerator: (Int) -> String = { id -> "chunk_$id" },
) : OutputStream() {
    private var currentOutputStreamBytesWritten = 0

    private var _isClosed = false

    private var _numOfFiles: Int = 0

    private val listener = mutableListOf<Listener>()

    /**
     * Get the number of files written.
     */
    val numOfFiles: Int
        get() = _numOfFiles

    private val fileId: Int
        get() = numOfFiles - 1


    private var outputStream: FileOutputStream? = null

    /**
     * Get if the stream is closed.
     */
    val isClosed: Boolean
        get() = _isClosed

    init {
        require(chunkSize > 0) { "Part size must be greater than 0" }
        require(filesDir.isDirectory) { "Files directory must be a directory" }
        require(filesDir.canWrite()) { "Files directory must be writable" }
    }

    private fun getFile(): File {
        return File(filesDir, chunkNameGenerator(fileId))
    }

    private fun closeOutputStream(outputStream: FileOutputStream, isLast: Boolean) {
        outputStream.close()
        listener.forEach {
            it.onFileAvailable(
                fileId,
                isLast,
                getFile()
            )
        }
    }

    private fun getOutputStream(): FileOutputStream {
        if ((currentOutputStreamBytesWritten >= chunkSize) || (outputStream == null)) {
            // Close previous stream
            outputStream?.let {
                closeOutputStream(it, false)
            }

            // Prepare a new stream
            currentOutputStreamBytesWritten = 0
            _numOfFiles++

            outputStream = FileOutputStream(getFile())
        }
        return outputStream!!
    }

    /**
     * Write [i] to the stream.
     *
     * @param i the byte to write
     */
    override fun write(i: Int) {
        synchronized(this) {
            if (isClosed) {
                throw IllegalStateException("Stream is closed")
            }

            val outputStream = getOutputStream()
            outputStream.write(i)
            currentOutputStreamBytesWritten++
        }
    }

    /**
     * Write [b] to the stream.
     *
     * @param b the byte to write
     */
    override fun write(b: ByteArray) {
        write(b, 0, b.size)
    }

    /**
     * Write [len] bytes from [b] starting at [offset].
     *
     * @param b the bytes to write
     * @param offset the offset in the output stream
     * @param len the number of bytes to write
     */
    override fun write(b: ByteArray, offset: Int, len: Int) {
        var remainingBytes = len
        var offsetShift = 0

        synchronized(this) {
            if (isClosed) {
                throw IllegalStateException("Stream is closed")
            }

            while (remainingBytes > 0) {
                val outputStream = getOutputStream()
                val numOfBytesToWrite = minOf(remainingBytes, chunkSize - currentOutputStreamBytesWritten)

                outputStream.write(b, offset + offsetShift, numOfBytesToWrite)

                currentOutputStreamBytesWritten += numOfBytesToWrite
                offsetShift += numOfBytesToWrite
                remainingBytes -= numOfBytesToWrite
            }
        }
    }

    /**
     * Close the stream.
     * This will close the current file and call [Listener.onFileAvailable] with the last file.
     */
    override fun close() {
        if (isClosed) {
            return
        }
        _isClosed = true

        synchronized(this) {
            outputStream?.let {
                closeOutputStream(it, true)
            }
        }
    }

    override fun flush() {
        outputStream?.flush()
    }

    /**
     * Adds a listener to the stream.
     *
     * @param listener the listener to add
     */
    fun addListener(listener: Listener) {
        this.listener.add(listener)
    }

    /**
     * Removes a listener from the stream.
     *
     * @param listener the listener to remove
     */
    fun removeListener(listener: Listener) {
        this.listener.remove(listener)
    }

    /**
     * Removes all listeners from the stream.
     */
    fun removeListeners() {
        this.listener.clear()
    }

    /**
     * Listener for [ChunkedFileOutputStream]
     */
    interface Listener {
        /**
         * Called when a file is available for other operations.
         *
         * It means that the file is ready to be read and won't be used anymore for the stream.
         *
         * @param index the index of the file
         * @param isLast true if this is the last file
         * @param file the file
         */
        fun onFileAvailable(index: Int, isLast: Boolean, file: File) {}
    }
}