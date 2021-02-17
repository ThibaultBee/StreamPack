package com.github.thibaultbee.srtstreamer.muxers.ts.utils

import com.github.thibaultbee.srtstreamer.muxers.ts.data.ServiceInfo

object Utils {
    fun fakeServiceInfo() = ServiceInfo(
        ServiceInfo.ServiceType.DIGITAL_TV,
        0x4698,
        "ServiceName",
        "ProviderName"
    )
}