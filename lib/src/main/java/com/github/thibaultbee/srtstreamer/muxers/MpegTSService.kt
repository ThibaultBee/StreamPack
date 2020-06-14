package com.github.thibaultbee.srtstreamer.muxers

class MpegTSService(
    val pmt: MpegTSSection,
    val type: ServiceType,
    val sid: Int,
    val name: String,
    val providerName: String,
    var pcrPid: Int = 0x1fff,
    var pcrPacketCount: Int = 0,
    var pcrPacketPeriod: Int = 0
) {
    enum class ServiceType(val value: Int) {
        DIGITAL_TV(0x01),
        DIGITAL_RADIO(0x02),
        TELETEXT(0x03),
        ADVANCED_CODEC_DIGITAL_RADIO(0x0A),
        MPEG2_DIGITAL_HDTV(0x11),
        ADVANCED_CODEC_DIGITAL_SDTV(0x16),
        ADVANCED_CODEC_DIGITAL_HDTV(0x19),
        HEVC_DIGITAL_HDTV(0x1F),
    }
}

