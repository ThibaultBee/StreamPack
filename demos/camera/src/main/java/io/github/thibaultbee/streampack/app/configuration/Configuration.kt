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
package io.github.thibaultbee.streampack.app.configuration

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Range
import android.util.Size
import androidx.preference.PreferenceManager
import io.github.thibaultbee.streampack.app.R
import io.github.thibaultbee.streampack.app.models.EndpointType
import io.github.thibaultbee.streampack.app.utils.ProfileLevelDisplay

class Configuration(context: Context) {
    private val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
    private val resources = context.resources
    private val profileLevelDisplay = ProfileLevelDisplay(context)
    val video = Video(sharedPref, resources, profileLevelDisplay)
    val audio = Audio(sharedPref, resources)
    val muxer = Muxer(sharedPref, resources)
    val endpoint = Endpoint(sharedPref, resources)

    class Video(
        private val sharedPref: SharedPreferences,
        private val resources: Resources,
        private val profileLevelDisplay: ProfileLevelDisplay
    ) {
        var enable: Boolean = true
            get() = sharedPref.getBoolean(resources.getString(R.string.video_enable_key), field)

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

        var profile: Int = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline
            get() {
                val profileName =
                    sharedPref.getString(resources.getString(R.string.video_profile_key), null)
                        ?: profileLevelDisplay.getProfileName(
                            encoder,
                            field
                        )
                return profileLevelDisplay.getProfile(encoder, profileName)
            }

        var level: Int = MediaCodecInfo.CodecProfileLevel.AVCLevel1
            get() {
                val levelName =
                    sharedPref.getString(resources.getString(R.string.video_level_key), null)
                        ?: profileLevelDisplay.getLevelName(
                            encoder,
                            field
                        )
                return profileLevelDisplay.getLevel(encoder, levelName)
            }
    }

    class Audio(private val sharedPref: SharedPreferences, private val resources: Resources) {
        var enable: Boolean = true
            get() = sharedPref.getBoolean(resources.getString(R.string.audio_enable_key), field)

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

        var profile: Int = MediaCodecInfo.CodecProfileLevel.AACObjectLC
            get() = sharedPref.getString(
                resources.getString(R.string.audio_profile_key),
                field.toString()
            )!!.toInt()
    }

    class Muxer(private val sharedPref: SharedPreferences, private val resources: Resources) {
        var service: String = resources.getString(R.string.default_muxer_service)
            get() = sharedPref.getString(
                resources.getString(R.string.ts_muxer_service_key),
                field
            )!!

        var provider: String = resources.getString(R.string.default_ts_muxer_provider)
            get() = sharedPref.getString(
                resources.getString(R.string.ts_muxer_provider_key),
                field
            )!!
    }

    class Endpoint(private val sharedPref: SharedPreferences, private val resources: Resources) {
        val file = File(sharedPref, resources)
        val srt = SrtConnection(sharedPref, resources)
        val rtmp = RtmpConnection(sharedPref, resources)

        val endpointType: EndpointType
            get() {
                val endpointId = sharedPref.getString(
                    resources.getString(R.string.endpoint_type_key),
                    "${EndpointType.SRT.id}"
                )!!.toInt()

                return EndpointType.fromId(endpointId)
            }

        class File(
            private val sharedPref: SharedPreferences,
            private val resources: Resources
        ) {
            var filename: String = ""
                get() = sharedPref.getString(
                    resources.getString(R.string.file_name_key),
                    field
                )!!
        }

        class SrtConnection(
            private val sharedPref: SharedPreferences,
            private val resources: Resources
        ) {
            var ip: String = ""
                get() = sharedPref.getString(
                    resources.getString(R.string.srt_server_ip_key),
                    field
                )!!

            var port: Int = 9998
                get() = sharedPref.getString(
                    resources.getString(R.string.srt_server_port_key),
                    field.toString()
                )!!.toInt()

            var streamID: String = ""
                get() = sharedPref.getString(
                    resources.getString(R.string.ts_server_stream_id_key),
                    field
                )!!

            var passPhrase: String = ""
                get() = sharedPref.getString(
                    resources.getString(R.string.ts_server_passphrase_key),
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

        class RtmpConnection(
            private val sharedPref: SharedPreferences,
            private val resources: Resources
        ) {
            var url: String = resources.getString(R.string.default_rtmp_url)
                get() = sharedPref.getString(
                    resources.getString(R.string.rtmp_server_url_key),
                    field
                )!!
        }
    }

}