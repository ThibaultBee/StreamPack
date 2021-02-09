package com.github.thibaultbee.srtstreamer.mux.ts.data

import com.github.thibaultbee.srtstreamer.mux.ts.tables.Pmt

class Service(
    val info: ServiceInfo,
    var pmt: Pmt? = null,
    var streams: MutableList<Stream> = mutableListOf(),
    var pcrPid: Short? = null
)