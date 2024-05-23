package io.github.thibaultbee.streampack.data.mediadescriptor

import io.github.thibaultbee.streampack.internal.endpoints.composites.muxers.ts.data.TSServiceInfo

/**
 * Creates a default [TSServiceInfo].
 */
fun createDefaultTsServiceInfo() = TSServiceInfo(
    TSServiceInfo.ServiceType.DIGITAL_TV,
    0x4698,
    "Stream",
    "StreamPack"
)