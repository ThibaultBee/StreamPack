package io.github.thibaultbee.streampack.core.internal.utils

import java.io.File

object FileUtils {
    fun createTestFile(suffix: String = ".tmp"): File {
        val tmpFile = File.createTempFile("test", suffix)
        tmpFile.deleteOnExit()
        return tmpFile
    }
}