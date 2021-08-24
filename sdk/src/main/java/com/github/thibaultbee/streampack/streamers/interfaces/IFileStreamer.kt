package com.github.thibaultbee.streampack.streamers.interfaces

import java.io.File

interface IFileStreamer : IStreamer {
    /**
     * Streamer file.
     */
    var file: File?
}