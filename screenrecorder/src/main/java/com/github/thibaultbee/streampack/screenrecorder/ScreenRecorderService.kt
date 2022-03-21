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
package com.github.thibaultbee.streampack.screenrecorder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Display
import androidx.activity.result.ActivityResult
import androidx.core.app.ActivityCompat
import com.github.thibaultbee.streampack.data.AudioConfig
import com.github.thibaultbee.streampack.data.BitrateRegulatorConfig
import com.github.thibaultbee.streampack.data.VideoConfig
import com.github.thibaultbee.streampack.error.StreamPackError
import com.github.thibaultbee.streampack.internal.muxers.ts.data.TsServiceInfo
import com.github.thibaultbee.streampack.listeners.OnConnectionListener
import com.github.thibaultbee.streampack.listeners.OnErrorListener
import com.github.thibaultbee.streampack.regulator.DefaultSrtBitrateRegulatorFactory
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.ACTIVITY_RESULT_KEY
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.AUDIO_CONFIG_KEY
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.BITRATE
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.BYTE_FORMAT
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.CHANNEL_CONFIG
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.CHANNEL_ID
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.ENABLE_BITRATE_REGULATION
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.ENABLE_ECHO_CANCELER
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.ENABLE_NOISE_SUPPRESSOR
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.ENDPOINT_CONFIG_KEY
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.ENDPOINT_TYPE
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.IP
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.LEVEL
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.MIME_TYPE
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.MUXER_CONFIG_KEY
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.PASSPHRASE
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.PORT
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.PROFILE
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.PROVIDER
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.RESOLUTION_HEIGHT
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.RESOLUTION_WIDTH
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.RTMP_URL
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.SAMPLE_RATE
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.SERVICE
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.STREAM_ID
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.VIDEO_BITRATE_REGULATION_LOWER
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.VIDEO_BITRATE_REGULATION_UPPER
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.VIDEO_CONFIG_KEY
import com.github.thibaultbee.streampack.screenrecorder.models.EndpointType
import com.github.thibaultbee.streampack.streamers.bases.BaseScreenRecorderStreamer
import com.github.thibaultbee.streampack.streamers.interfaces.ILiveStreamer
import com.github.thibaultbee.streampack.streamers.interfaces.IRtmpLiveStreamer
import com.github.thibaultbee.streampack.streamers.interfaces.ISrtLiveStreamer
import com.github.thibaultbee.streampack.streamers.interfaces.builders.IAdaptiveLiveStreamerBuilder
import com.github.thibaultbee.streampack.streamers.interfaces.builders.ITsStreamerBuilder
import com.github.thibaultbee.streampack.streamers.rtmp.ScreenRecorderRtmpLiveStreamer
import com.github.thibaultbee.streampack.streamers.srt.ScreenRecorderSrtLiveStreamer
import kotlinx.coroutines.runBlocking
import java.io.IOException

/**
 * Foreground service that launches and stops streamer.
 *
 * In your AndroidManifest, you have to add
 *     <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
 * ...
 *     <service
 *          android:name=".ScreenRecorderService"
 *          android:exported="false"
 *          android:foregroundServiceType="mediaProjection" />
 */
@SuppressLint("MissingPermission")
class ScreenRecorderService : Service() {
    private val tag = this::class.simpleName
    private lateinit var notificationManager: NotificationManager
    private val notifyId = 5734

    class ConfigKeys {
        companion object {
            // Audio/Video config
            const val MIME_TYPE = "mimeType"
            const val BITRATE = "bitrate"
            const val SAMPLE_RATE = "sampleRate"
            const val CHANNEL_CONFIG = "channelConfig"
            const val BYTE_FORMAT = "byteFormat"
            const val ENABLE_ECHO_CANCELER = "enableEchoCanceler"
            const val ENABLE_NOISE_SUPPRESSOR = "enableNoiseSuppressor"
            const val RESOLUTION_WIDTH = "resolutionWidth"
            const val RESOLUTION_HEIGHT = "resolutionHeight"
            const val LEVEL = "level"
            const val PROFILE = "profile"

            // Endpoint config
            const val ENDPOINT_TYPE = "ENDPOINT_TYPE"
            const val IP = "ip"
            const val PORT = "port"
            const val PASSPHRASE = "passPhrase"
            const val STREAM_ID = "streamID"
            const val ENABLE_BITRATE_REGULATION = "enableBitrateRegulation"
            const val VIDEO_BITRATE_REGULATION_LOWER = "bitrateRegulationLower"
            const val VIDEO_BITRATE_REGULATION_UPPER = "bitrateRegulationUpper"
            const val RTMP_URL = "rtmpUrl"

            // Muxer config
            const val PROVIDER = "provider"
            const val SERVICE = "service"

            // Main keys
            const val ACTIVITY_RESULT_KEY = "activityResult"
            const val AUDIO_CONFIG_KEY = "audioConfig"
            const val VIDEO_CONFIG_KEY = "videoConfig"
            const val ENDPOINT_CONFIG_KEY = "endpointConfig"
            const val MUXER_CONFIG_KEY = "muxerConfig"

            // Notification
            const val CHANNEL_ID = "StreamPackNotificationChannel"
        }
    }

    private var screenRecorder: BaseScreenRecorderStreamer? = null

    override fun onCreate() {
        super.onCreate()

        notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)

            val channel = NotificationChannel(
                CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = descriptionText
            notificationManager.createNotificationChannel(channel)

            startForeground(
                notifyId,
                createNotification(
                    R.string.notification_title,
                    R.string.notification_service_started
                )
            )
        } else {
            notify(
                R.string.notification_title,
                R.string.notification_service_started
            )
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        try {
            val activityResult = (intent.extras?.get(ACTIVITY_RESULT_KEY)
                ?: throw IllegalStateException("ActivityResult must be pass to the service")) as ActivityResult

            val endpointConfigBundle = intent.extras?.getBundle(ENDPOINT_CONFIG_KEY)
                ?: throw IllegalStateException("Connection configuration must be pass to the service")
            val endpointType = EndpointType.fromId(endpointConfigBundle.getInt(ENDPOINT_TYPE))

            val ip = endpointConfigBundle.getString(IP)
                ?: throw IllegalStateException("Connection IP must be pass to the service")
            val port = endpointConfigBundle.getInt(PORT)
            val streamId = endpointConfigBundle.getString(STREAM_ID)
            val passPhrase = endpointConfigBundle.getString(PASSPHRASE)
            val enableBitrateRegulation =
                endpointConfigBundle.getBoolean(ENABLE_BITRATE_REGULATION, false)
            val url = endpointConfigBundle.getString(RTMP_URL)
                ?: throw IllegalStateException("RTMP URL must be pass to the service")

            val videoBitrateRange = if (enableBitrateRegulation) {
                if (!endpointConfigBundle.containsKey(VIDEO_BITRATE_REGULATION_LOWER) or !endpointConfigBundle.containsKey(
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

            val muxerConfigBundle = intent.extras?.getBundle(MUXER_CONFIG_KEY)
                ?: throw IllegalStateException("Muxer configuration must be pass to the service")
            val serviceInfo = TsServiceInfo(
                TsServiceInfo.ServiceType.DIGITAL_TV,
                0x4698,
                muxerConfigBundle.getString(SERVICE)!!,
                muxerConfigBundle.getString(PROVIDER)!!
            )

            val videoConfig = intent.extras?.getBundle(VIDEO_CONFIG_KEY)?.let { bundle ->
                createVideoConfigFromBundle(bundle)
            } ?: VideoConfig.Builder().build()

            val hasAudio = intent.extras?.get(AUDIO_CONFIG_KEY) != null
            val audioConfig = (intent.extras?.get(AUDIO_CONFIG_KEY) as Bundle?)?.let { bundle ->
                createAudioConfigFromBundle(bundle)
            } ?: AudioConfig.Builder().build()

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                throw SecurityException("Permission RECORD_AUDIO must have been granted!")
            }

            val streamerBuilder = when (endpointType) {
                EndpointType.SRT -> {
                    ScreenRecorderSrtLiveStreamer.Builder()
                }
                EndpointType.RTMP -> {
                    ScreenRecorderRtmpLiveStreamer.Builder()
                }
            }

            if (streamerBuilder is IAdaptiveLiveStreamerBuilder) {
                streamerBuilder.setBitrateRegulator(
                    if (enableBitrateRegulation) {
                        DefaultSrtBitrateRegulatorFactory()
                    } else {
                        null
                    },
                    if (enableBitrateRegulation) {
                        BitrateRegulatorConfig.Builder()
                            .setVideoBitrateRange(videoBitrateRange!!) // if enableBitrateRegulation = true, videoBitrateRange exists
                            .setAudioBitrateRange(
                                Range(
                                    audioConfig.startBitrate,
                                    audioConfig.startBitrate
                                )
                            )
                            .build()
                    } else {
                        null
                    },
                )
            }

            if (streamerBuilder is ITsStreamerBuilder) {
                streamerBuilder.setServiceInfo(serviceInfo)
            }

            if (!hasAudio) {
                streamerBuilder.disableAudio()
            }

            screenRecorder = (streamerBuilder
                .setContext(this)
                .setConfiguration(audioConfig, videoConfig)
                .build() as BaseScreenRecorderStreamer).apply {

                onErrorListener = object : OnErrorListener {
                    override fun onError(error: StreamPackError) {
                        Log.e(tag, "An error occurred", error)
                        notify(
                            "An error occurred",
                            error.localizedMessage ?: "Unknown error"
                        )
                        stopSelf()
                    }
                }
                if (this is ILiveStreamer) {
                    onConnectionListener = object : OnConnectionListener {
                        override fun onLost(message: String) {
                            Log.e(tag, "Connection lost: $message")
                            notify("Connection", message)
                            stopSelf()
                        }

                        override fun onFailed(message: String) {
                            Log.e(tag, "Connection failed: $message")
                            notify("Connection failed", message)
                            stopSelf()
                        }

                        override fun onSuccess() {
                            Log.i(tag, "Connection succeeded")
                            notify("Connection succeeded", "")
                        }
                    }


                    if (this is ISrtLiveStreamer) {
                        streamId?.let {
                            this.streamId = it
                        }
                        passPhrase?.let {
                            this.passPhrase = it
                        }
                    }

                    this.activityResult = activityResult

                    try {
                        runBlocking {
                            when (this@apply) {
                                is ISrtLiveStreamer -> this@apply.startStream(ip, port)
                                is IRtmpLiveStreamer -> this@apply.startStream(url)
                                else -> throw IOException("Live streamer is not supported: ${this::class.java.simpleName}")
                            }
                        }
                    } catch (e: Exception) {
                        this@apply.release()
                        screenRecorder = null
                        throw e
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(tag, "An error occurred", e)
            notify("An error occurred", e.localizedMessage ?: "Unknown error")
            stopSelf()
        }
        return START_REDELIVER_INTENT
    }

    private fun createAudioConfigFromBundle(bundle: Bundle): AudioConfig {
        val configBuilder = AudioConfig.Builder()
        bundle.getString(MIME_TYPE)?.let {
            configBuilder.setMimeType(it)
        }
        if (bundle.containsKey(BITRATE)) {
            configBuilder.setStartBitrate(bundle.getInt(BITRATE))
        }
        if (bundle.containsKey(SAMPLE_RATE)) {
            configBuilder.setSampleRate(bundle.getInt(SAMPLE_RATE))
        }
        if (bundle.containsKey(CHANNEL_CONFIG)) {
            configBuilder.setNumberOfChannel(bundle.getInt(CHANNEL_CONFIG))
        }
        if (bundle.containsKey(BYTE_FORMAT)) {
            configBuilder.setByteFormat(bundle.getInt(BYTE_FORMAT))
        }
        if (bundle.containsKey(ENABLE_ECHO_CANCELER)) {
            configBuilder.setEchoCanceler(bundle.getBoolean(ENABLE_ECHO_CANCELER))
        }
        if (bundle.containsKey(ENABLE_NOISE_SUPPRESSOR)) {
            configBuilder.setNoiseSuppressor(bundle.getBoolean(ENABLE_NOISE_SUPPRESSOR))
        }
        return configBuilder.build()
    }

    private fun createVideoConfigFromBundle(bundle: Bundle): VideoConfig {
        val configBuilder = VideoConfig.Builder()
        bundle.getString(MIME_TYPE)?.let {
            configBuilder.setMimeType(it)
        }
        if (bundle.containsKey(BITRATE)) {
            configBuilder.setStartBitrate(bundle.getInt(BITRATE))
        }
        if (bundle.containsKey(RESOLUTION_WIDTH) && bundle.containsKey(RESOLUTION_HEIGHT)) {
            configBuilder.setResolution(
                Size(
                    bundle.getInt(RESOLUTION_WIDTH),
                    bundle.getInt(RESOLUTION_HEIGHT)
                )
            )
        }
        if (bundle.containsKey(LEVEL)) {
            configBuilder.setEncoderLevel(bundle.getInt(LEVEL))
        }
        if (bundle.containsKey(PROFILE)) {
            configBuilder.setEncoderLevel(bundle.getInt(PROFILE))
        }
        // refreshRate returns a high value but measured fps is lower...
        configBuilder.setFps(
            (this.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).getDisplay(
                Display.DEFAULT_DISPLAY
            ).refreshRate.toInt()
        )

        return configBuilder.build()
    }

    private fun notify(titleResId: Int, contentResId: Int) =
        notify(getString(titleResId), getString(contentResId))

    private fun notify(title: String, content: String) {
        notificationManager.notify(notifyId, createNotification(title, content))
    }

    private fun createNotification(titleResId: Int, contentResId: Int) =
        createNotification(getString(titleResId), getString(contentResId))

    private fun createNotification(title: String, content: String): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_notifications_24)
                .setContentTitle(title)
                .setContentText(content)
                .build()
        } else {
            Notification.Builder(applicationContext)
                .setSmallIcon(R.drawable.ic_baseline_notifications_24)
                .setContentTitle(title)
                .setContentText(content)
                .build()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        screenRecorder?.stopStream()
        (screenRecorder as ILiveStreamer?)?.disconnect()
        screenRecorder?.release()
        screenRecorder = null
    }
}