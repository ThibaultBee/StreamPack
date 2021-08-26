/*
 * Copyright (C) 2021 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.thibaultbee.streampack.app.configuration

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.media.MediaFormat
import android.util.Range
import android.util.Size
import androidx.preference.PreferenceManager
import com.github.thibaultbee.streampack.app.R

class Configuration(context: Context) {
    private val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
    private val resources = context.resources
    val video = Video(sharedPref, resources)
    val audio = Audio(sharedPref, resources)
    val muxer = Muxer(sharedPref, resources)
    val endpoint = Endpoint(sharedPref, resources)

    class Video(private val sharedPref: SharedPreferences, private val resources: Resources) {
        var encoder: String = MediaFormat.MIMETYPE_VIDEO_AVC
            get() = sharedPref.getString(resources.getString(R.string.video_encoder_key), field)!!

        var fps: Int = 30
            get() = sharedPref.getString(
                resources.getString(R.string.video_fps_key),
                field.toString()
            )!!.toInt()

        var resolution: Size = Size(1280, 720)
            get() {
                val res = sharedPref.getString(
                    resources.getString(R.string.video_resolution_key),
                    field.toString()
                )!!
                val resArray = res.split("x")
                return Size(
                    resArray[0].toInt(),
                    resArray[1].toInt()
                )
            }

        var bitrate: Int = 2000
            get() = sharedPref.getInt(resources.getString(R.string.video_bitrate_key), field)
    }

    class Audio(private val sharedPref: SharedPreferences, private val resources: Resources) {
        var encoder: String = MediaFormat.MIMETYPE_AUDIO_AAC
            get() = sharedPref.getString(resources.getString(R.string.audio_encoder_key), field)!!

        var numberOfChannels: Int = 2
            get() = sharedPref.getString(
                resources.getString(R.string.audio_number_of_channels_key),
                field.toString()
            )!!.toInt()

        var bitrate: Int = 128000
            get() = sharedPref.getString(
                resources.getString(R.string.audio_bitrate_key),
                field.toString()
            )!!.toInt()

        var sampleRate: Int = 48000
            get() = sharedPref.getString(
                resources.getString(R.string.audio_sample_rate_key),
                field.toString()
            )!!.toInt()


        val byteFormat: Int = 2
            get() = sharedPref.getString(
                resources.getString(R.string.audio_byte_format_key),
                field.toString()
            )!!.toInt()

        var enableEchoCanceler: Boolean = false
            get() = sharedPref.getBoolean(
                resources.getString(R.string.audio_enable_echo_canceler_key),
                field
            )

        var enableNoiseSuppressor: Boolean = false
            get() = sharedPref.getBoolean(
                resources.getString(R.string.audio_enable_noise_suppressor_key),
                field
            )
    }

    class Muxer(private val sharedPref: SharedPreferences, private val resources: Resources) {
        var service: String = resources.getString(R.string.default_muxer_service)
            get() = sharedPref.getString(resources.getString(R.string.muxer_service_key), field)!!

        var provider: String = resources.getString(R.string.default_muxer_provider)
            get() = sharedPref.getString(resources.getString(R.string.muxer_provider_key), field)!!
    }

    class Endpoint(private val sharedPref: SharedPreferences, private val resources: Resources) {
        val file = File(sharedPref, resources)
        val connection = Connection(sharedPref, resources)

        enum class EndpointType {
            FILE,
            SRT
        }

        val enpointType: EndpointType
            get() {
                return if (sharedPref.getBoolean(
                        resources.getString(R.string.endpoint_type_key),
                        true
                    )
                ) {
                    EndpointType.SRT
                } else {
                    EndpointType.FILE
                }
            }

        class File(
            private val sharedPref: SharedPreferences,
            private val resources: Resources
        ) {
            companion object {
                const val TS_FILE_EXTENSION = ".ts"
            }

            var filename: String = ""
                get() = "${
                    sharedPref.getString(
                        resources.getString(R.string.file_name_key),
                        field
                    )!!
                }$TS_FILE_EXTENSION"
        }

        class Connection(
            private val sharedPref: SharedPreferences,
            private val resources: Resources
        ) {
            var ip: String = ""
                get() = sharedPref.getString(resources.getString(R.string.server_ip_key), field)!!

            var port: Int = 9998
                get() = sharedPref.getString(
                    resources.getString(R.string.server_port_key),
                    field.toString()
                )!!.toInt()

            var streamID: String = ""
                get() = sharedPref.getString(
                    resources.getString(R.string.server_stream_id_key),
                    field
                )!!

            var passPhrase: String = ""
                get() = sharedPref.getString(
                    resources.getString(R.string.server_passphrase_key),
                    field
                )!!

            var enableBitrateRegulation: Boolean = false
                get() = sharedPref.getBoolean(
                    resources.getString(R.string.server_enable_bitrate_regulation_key),
                    field
                )

            var videoBitrateRange: Range<Int> = Range(300, 5000000)
                get() = Range(
                    sharedPref.getInt(
                        resources.getString(R.string.server_video_min_bitrate_key),
                        field.lower
                    ) * 1000,  // to b/s
                    sharedPref.getInt(
                        resources.getString(R.string.server_video_target_bitrate_key),
                        field.upper
                    ) * 1000,  // to b/s
                )
        }
    }

}