package io.github.thibaultbee.streampack.app.utils

object NetworkUtils {
    fun isLocalHost(host: String): Boolean {
        val cleanHost = host.replace("[", "").replace("]", "")
        return cleanHost == "localhost" ||
                cleanHost.startsWith("127.") ||
                cleanHost.startsWith("192.168.") ||
                cleanHost.startsWith("10.") ||
                (cleanHost.startsWith("172.") && cleanHost.split(".")[1].toIntOrNull() in 16..31) ||
                cleanHost.startsWith("fe80:") ||
                cleanHost == "::1"
    }
}