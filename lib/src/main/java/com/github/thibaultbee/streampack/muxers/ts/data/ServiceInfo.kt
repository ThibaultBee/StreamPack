package com.github.thibaultbee.streampack.muxers.ts.data

data class ServiceInfo(
    val type: ServiceType,
    val id: Short,
    val name: String,
    val providerName: String,
) {
    enum class ServiceType(val value: Byte) {
        DIGITAL_TV(0x01.toByte()),
        DIGITAL_RADIO(0x02.toByte()),
        TELETEXT(0x03.toByte()),
        ADVANCED_CODEC_DIGITAL_RADIO(0x0A.toByte()),
        MPEG2_DIGITAL_HDTV(0x11.toByte()),
        ADVANCED_CODEC_DIGITAL_SDTV(0x16.toByte()),
        ADVANCED_CODEC_DIGITAL_HDTV(0x19.toByte()),
        HEVC_DIGITAL_HDTV(0x1F.toByte()),
    }

    override fun equals(other: Any?): Boolean {
        if ((other as ServiceInfo).id == id) {
            return true
        }
        return false
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + id
        result = 31 * result + name.hashCode()
        result = 31 * result + providerName.hashCode()
        return result
    }
}

