package com.github.thibaultbee.streampack.app.configuration

import android.content.Context
import android.content.SharedPreferences
import android.util.Size
import androidx.preference.PreferenceManager

class Configuration(context: Context) {
    private val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
    val video = Video(sharedPref)
    val connection = Connection(sharedPref)

    class Video(private val sharedPref: SharedPreferences) {
        companion object {
            const val PREF_RESOLUTION_WIDTH_KEY = "shared_pref_video_resolution_width_key"
            const val PREF_RESOLUTION_HEIGHT_KEY = "shared_pref_video_resolution_height_key"
            const val PREF_BITRATE_KEY = "shared_pref_video_bitrate_key"
        }

        var resolution: Size = Size(1280, 720)
            get() = Size(
                sharedPref.getInt(PREF_RESOLUTION_WIDTH_KEY, field.width),
                sharedPref.getInt(
                    PREF_RESOLUTION_HEIGHT_KEY, field.height
                )
            )
            set(value) {
                with(sharedPref.edit()) {
                    putInt(
                        PREF_RESOLUTION_WIDTH_KEY,
                        value.width
                    )
                    putInt(
                        PREF_RESOLUTION_HEIGHT_KEY,
                        value.height
                    )
                    apply()
                }
                field = value
            }

        var bitrate: Int = 1500
            get() = sharedPref.getInt(PREF_BITRATE_KEY, field)
            set(value) {
                with(sharedPref.edit()) {
                    putInt(PREF_BITRATE_KEY, value)
                    apply()
                }
                field = value
            }
    }

    class Connection(private val sharedPref: SharedPreferences) {
        companion object {
            const val PREF_SERVER_IP_KEY = "shared_pref_connection_server_ip_key"
            const val PREF_SERVER_PORT_KEY = "shared_pref_connection_server_port_key"
        }

        var ip: String = ""
            get() = sharedPref.getString(PREF_SERVER_IP_KEY, field)!!
            set(value) {
                with(sharedPref.edit()) {
                    putString(PREF_SERVER_IP_KEY, value)
                    apply()
                }
                field = value
            }

        var port: Int = 9998
            get() = sharedPref.getInt(PREF_SERVER_PORT_KEY, field)
            set(value) {
                with(sharedPref.edit()) {
                    putInt(PREF_SERVER_PORT_KEY, value)
                    apply()
                }
                field = value
            }

    }
}