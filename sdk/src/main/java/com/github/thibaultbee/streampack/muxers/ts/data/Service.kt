package com.github.thibaultbee.streampack.muxers.ts.data

import com.github.thibaultbee.streampack.muxers.ts.tables.Pmt

class Service(
    val info: ServiceInfo,
    var pmt: Pmt? = null,
    var streams: MutableList<Stream> = mutableListOf(),
    var pcrPid: Short? = null
)