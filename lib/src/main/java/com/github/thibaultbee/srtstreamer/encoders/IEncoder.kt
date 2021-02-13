package com.github.thibaultbee.srtstreamer.encoders

import com.github.thibaultbee.srtstreamer.interfaces.Controllable

interface IEncoder : Controllable {
    /**
     * Input and output of an async encoder
     */
    val encoderListener: IEncoderListener

    /**
     * Get encoder mime type
     * @return a string corresponding to a media mime type
     * @see List of audio/video mime type on <a href="https://developer.android.com/reference/android/media/MediaFormat">Android developer guide</a>
     */
    val mimeType: String?
}