package com.github.thibaultbee.streampack.streamers.interfaces

import java.io.File

interface IFileStreamer {
    /**
     * [File] where to write data.
     */
    var file: File?
}