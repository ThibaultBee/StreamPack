package com.github.thibaultbee.streampack

import android.content.Context
import com.github.thibaultbee.streampack.endpoints.FileWriter
import com.github.thibaultbee.streampack.muxers.ts.data.ServiceInfo
import com.github.thibaultbee.streampack.utils.Logger
import java.io.File

class CaptureFileStream(
    context: Context,
    tsServiceInfo: ServiceInfo,
    val logger: Logger
) : BaseCaptureStream(context, tsServiceInfo, FileWriter(logger), logger) {
    private val fileWriter = endpoint as FileWriter

    /**
     * Get/Set [FileWriter] file. If no file has been set. [FileWriter] uses a default temporary file.
     */
    var file: File
        /**
         * Get file writer file
         * @return file where [FileWriter] writes
         */
        get() = fileWriter.file
        /**
         * Set file writer file
         * @param value [File] where [FileWriter] writes
         */
        set(value) {
            fileWriter.file = value
        }
}