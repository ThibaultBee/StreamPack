/*
 * Copyright (C) 2022 Thibault B.
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
package io.github.thibaultbee.streampack.ext.srt.services

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Range
import io.github.thibaultbee.streampack.R
import io.github.thibaultbee.streampack.data.BitrateRegulatorConfig
import io.github.thibaultbee.streampack.ext.srt.regulator.srt.DefaultSrtBitrateRegulatorFactory
import io.github.thibaultbee.streampack.ext.srt.streamers.ScreenRecorderSrtLiveStreamer
import io.github.thibaultbee.streampack.internal.endpoints.muxers.ts.data.TsServiceInfo
import io.github.thibaultbee.streampack.internal.utils.extensions.defaultTsServiceInfo
import io.github.thibaultbee.streampack.streamers.bases.BaseScreenRecorderStreamer
import io.github.thibaultbee.streampack.streamers.services.BaseScreenRecorderService

open class ScreenRecorderSrtLiveService(
    notificationId: Int = DEFAULT_NOTIFICATION_ID,
    channelId: String = DEFAULT_NOTIFICATION_CHANNEL_ID,
    channelNameResourceId: Int = R.string.default_channel_name,
    channelDescriptionResourceId: Int = 0,
) : BaseScreenRecorderService(
    notificationId,
    channelId,
    channelNameResourceId,
    channelDescriptionResourceId
) {

    override fun createStreamer(bundle: Bundle): BaseScreenRecorderStreamer {
        val enableAudio = bundle.getBoolean(ENABLE_AUDIO_KEY)

        val muxerConfigBundle = bundle.getBundle(MUXER_CONFIG_KEY)
        val tsServiceInfo = TsServiceInfo(
            TsServiceInfo.ServiceType.DIGITAL_TV,
            0x4698,
            muxerConfigBundle?.getString(SERVICE_NAME)
                ?: getString(R.string.ts_service_default_name),
            muxerConfigBundle?.getString(SERVICE_PROVIDER_NAME)
                ?: getString(R.string.ts_service_default_provider_name),
        )

        val endpointConfigBundle = bundle.getBundle(ENDPOINT_CONFIG_KEY)
        val enableBitrateRegulation =
            endpointConfigBundle?.getBoolean(ENABLE_BITRATE_REGULATION, false) ?: false

        val videoBitrateRange = if (enableBitrateRegulation) {
            if (!endpointConfigBundle!!.containsKey(VIDEO_BITRATE_REGULATION_LOWER) or !endpointConfigBundle.containsKey(
                    VIDEO_BITRATE_REGULATION_UPPER
                )
            ) {
                throw IllegalStateException("If bitrate regulation is enabled, video bitrate regulation must be set")
            }
            Range(
                endpointConfigBundle.getInt(VIDEO_BITRATE_REGULATION_LOWER),
                endpointConfigBundle.getInt(VIDEO_BITRATE_REGULATION_UPPER)
            )
        } else {
            null
        }

        val bitrateRegulatorFactory = if (enableBitrateRegulation) {
            DefaultSrtBitrateRegulatorFactory()
        } else {
            null
        }
        val bitrateRegulatorConfig = if (enableBitrateRegulation) {
            BitrateRegulatorConfig(
                videoBitrateRange!!,  // if enableBitrateRegulation = true, videoBitrateRange exists
                Range(
                    128000,
                    128000
                ) // Not used for now, but we need to set it to something
            )
        } else {
            null
        }

        return ScreenRecorderSrtLiveStreamer(
            applicationContext,
            enableAudio = enableAudio,
            tsServiceInfo = tsServiceInfo,
            bitrateRegulatorFactory = bitrateRegulatorFactory,
            bitrateRegulatorConfig = bitrateRegulatorConfig
        )
    }

    companion object {
        // Muxer config
        const val SERVICE_PROVIDER_NAME = "providerName"
        const val SERVICE_NAME = "name"

        // Endpoint
        const val ENABLE_BITRATE_REGULATION = "enableBitrateRegulation"
        const val VIDEO_BITRATE_REGULATION_LOWER = "bitrateRegulationLower"
        const val VIDEO_BITRATE_REGULATION_UPPER = "bitrateRegulationUpper"

        /**
         * Starts and binds the service with the appropriate parameters.
         *
         * @param context The application context.
         * @param serviceClass The children service class.
         * @param enableAudio [Boolean.true] to also capture audio. [Boolean.false] to disable audio capture.
         * @param tsServiceInfo MPEG-TS service description
         * @param enableBitrateRegulation [Boolean.true] to enable bitrate regulation. [Boolean.false] to disable bitrate regulation.
         * @param bitrateRegulatorConfig bitrate regulator configuration. If bitrateRegulatorFactory is not null, bitrateRegulatorConfig must not be null.
         * @param onServiceCreated Callback that returns the [ScreenRecorderSrtLiveStreamer] instance when the service has been connected.
         * @param onServiceDisconnected Callback that will be called when the service is disconnected.
         */
        fun launch(
            context: Context,
            serviceClass: Class<out ScreenRecorderSrtLiveService>,
            enableAudio: Boolean = true,
            tsServiceInfo: TsServiceInfo = context.defaultTsServiceInfo,
            enableBitrateRegulation: Boolean = false,
            bitrateRegulatorConfig: BitrateRegulatorConfig? = null,
            onServiceCreated: (ScreenRecorderSrtLiveStreamer) -> Unit,
            onServiceDisconnected: (name: ComponentName?) -> Unit
        ) {
            val constructorBundle = Bundle().apply {
                putBoolean(ENABLE_AUDIO_KEY, enableAudio)

                putBundle(MUXER_CONFIG_KEY, Bundle().apply {
                    putString(SERVICE_PROVIDER_NAME, tsServiceInfo.providerName)
                    putString(SERVICE_NAME, tsServiceInfo.name)
                })

                putBundle(ENDPOINT_CONFIG_KEY, Bundle().apply {
                    putBoolean(ENABLE_BITRATE_REGULATION, enableBitrateRegulation)
                    if (enableBitrateRegulation) {
                        require(bitrateRegulatorConfig != null) { "If bitrate regulation is enabled, bitrateRegulatorConfig must be set" }
                        putInt(
                            VIDEO_BITRATE_REGULATION_LOWER,
                            bitrateRegulatorConfig.videoBitrateRange.lower
                        )
                        putInt(
                            VIDEO_BITRATE_REGULATION_UPPER,
                            bitrateRegulatorConfig.videoBitrateRange.upper
                        )
                    }
                })
            }

            launch(
                context,
                serviceClass,
                constructorBundle,
                { streamer -> onServiceCreated(streamer as ScreenRecorderSrtLiveStreamer) },
                onServiceDisconnected
            )
        }
    }
}