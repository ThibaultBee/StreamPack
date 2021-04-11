package com.github.thibaultbee.streampack.app.configuration

import android.content.Context
import com.github.thibaultbee.streampack.utils.getOutputCaptureSizesIntersection

class ConfigurationHelper(context: Context) {
    val resolutionEntries = context.getOutputCaptureSizesIntersection()
}