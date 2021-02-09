package com.github.thibaultbee.srtstreamer.utils

/**
 * Check if mime type is a video mime type
 * @return true if mime type is video, otherwise false
 */
fun String.isVideo() = this.startsWith("video")

/**
 * Check if mime type is an audio mime type
 * @return true if mime type is audio, otherwise false
 */
fun String.isAudio() = this.startsWith("audio")