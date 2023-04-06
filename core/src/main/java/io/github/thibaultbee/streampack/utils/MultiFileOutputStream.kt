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
package io.github.thibaultbee.streampack.utils

import io.github.thibaultbee.streampack.streamers.interfaces.IFileStreamer
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * A class that allows to write to multiple files.
 * If you are looking to write the video or audio in multiple files, use this class
 * as an [OutputStream] for a [IFileStreamer].
 *
 * @param filesDir the directory where the files will be written
 * @param partSize the size of each file
 * @param namePrefix the prefix of each file
 * @param listener the listener that will be called when a file is created
 */
class MultiFileOutputStream(
    val filesDir: File,
    private val partSize: Long,
    private val namePrefix: String = "part_",
    private val listener: Listener
) : OutputStream() {
    private var currentFileBytesWritten = 0L
    private var bytesWritten = 0

    private var _isClosed = false

    /**
     * Get if the stream is closed.
     */
    val isClosed: Boolean
        get() = _isClosed

    private var _outputStream: FileOutputStream? = null
    private val outputStream: FileOutputStream
        get() {
            if (_isClosed) {
                throw IllegalStateException("Stream is closed")
            }
            synchronized(this) {
                if ((_outputStream == null) || (currentFileBytesWritten >= partSize)) {
                    _outputStream?.let {
                        it.close()
                        listener.onFileCreated(
                            numOfFileWritten,
                            false,
                            getFile(numOfFileWritten)
                        )
                    }

                    currentFileBytesWritten = 0
                    _numOfFileWritten++

                    _outputStream = FileOutputStream(getFile(numOfFileWritten))
                }
                return _outputStream!!
            }
        }

    private var _numOfFileWritten: Int = 0
    /**
     * Get the number of files written.
     */
    val numOfFileWritten: Int
        get() = _numOfFileWritten

    init {
        require(partSize > 0) { "Part size must be greater than 0" }
        require(filesDir.isDirectory) { "Files directory must be a directory" }
        require(filesDir.canWrite()) { "Files directory must be writable" }
    }

    private fun getFile(fileIndex: Int): File {
        return File(filesDir, "$namePrefix$fileIndex")
    }

    /**
     * Write [b] to the stream.
     *
     * @param b the byte to write
     */
    override fun write(b: Int) {
        outputStream.write(b)
        currentFileBytesWritten++
        bytesWritten++
    }

    /**
     * Write [b] to the stream.
     *
     * @param b the byte to write
     */
    override fun write(b: ByteArray) {
        outputStream.write(b)
        currentFileBytesWritten += b.size
        bytesWritten += b.size
    }

    /**
     * Write [len] bytes from [b] starting at [off].
     *
     * @param b the bytes to write
     * @param off the offset in [b] to start writing
     * @param len the number of bytes to write
     */
    override fun write(b: ByteArray, off: Int, len: Int) {
        outputStream.write(b, off, len)
        currentFileBytesWritten += len
        bytesWritten += len
    }

    /**
     * Close the stream.
     * This will close the current file and call [Listener.onFileCreated] with the last file.
     */
    override fun close() {
        if (_isClosed) {
            return
        }
        _isClosed = true
        _outputStream?.let {
            it.close()
            listener.onFileCreated(numOfFileWritten, true, getFile(numOfFileWritten))
        }
        _outputStream = null
    }

    override fun flush() {
        _outputStream?.flush()
    }

    /**
     * Delete all files
     */
    fun delete() {
        filesDir.deleteRecursively()
    }

    /**
     * Listener for [MultiFileOutputStream]
     */
    interface Listener {
        /**
         * Called when a file is created.
         *
         * @param index the index of the file
         * @param isLast true if this is the last file
         * @param file the file
         */
        fun onFileCreated(index: Int, isLast: Boolean, file: File) {}
    }
}