package com.github.thibaultbee.srtstreamer.utils

import com.github.thibaultbee.srtstreamer.models.Frame
import java.nio.ByteBuffer
import kotlin.random.Random

object FakeFrames {
    fun createFakeKeyFrame(mimeType: String) = Frame(
        ByteBuffer.wrap(Random.Default.nextBytes(1024)),
        mimeType,
        Random.nextLong(),
        isKeyFrame = true,
        extra = ByteBuffer.wrap(Random.nextBytes(10))
    )

    fun createFakeFrame(mimeType: String) = Frame(
        ByteBuffer.wrap(Random.Default.nextBytes(1024)),
        mimeType,
        Random.nextLong(),
        isKeyFrame = false
    )
}