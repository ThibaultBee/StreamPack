package com.github.thibaultbee.streampack.streamers.interfaces.builders

import java.io.File

interface IFileStreamerBuilder : IStreamerBuilder {
    /**
     * Set destination file.
     *
     * @param file where to write date
     */
    fun setFile(file: File): IFileStreamerBuilder
}