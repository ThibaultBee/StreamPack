package io.github.thibaultbee.streampack.core.elements.utils

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

object FileUtils {
    fun createTestFile(suffix: String = ".tmp"): File {
        val tmpFile = File.createTempFile("test", suffix)
        tmpFile.deleteOnExit()
        return tmpFile
    }

    fun createCacheFile(name: String): File {
        return File(InstrumentationRegistry.getInstrumentation().context.cacheDir, name)
    }
}