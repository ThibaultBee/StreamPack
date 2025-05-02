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
package io.github.thibaultbee.streampack.screenrecorder

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.Menu
import android.view.MenuInflater
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.elements.encoders.mediacodec.MediaCodecHelper
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.ts.data.TSServiceInfo
import io.github.thibaultbee.streampack.core.interfaces.startStream
import io.github.thibaultbee.streampack.core.streamers.IConfigurableAudioStreamer
import io.github.thibaultbee.streampack.core.streamers.IVideoStreamer
import io.github.thibaultbee.streampack.core.streamers.dual.DualStreamerAudioConfig
import io.github.thibaultbee.streampack.core.streamers.dual.DualStreamerVideoConfig
import io.github.thibaultbee.streampack.core.streamers.dual.IAudioDualStreamer
import io.github.thibaultbee.streampack.core.streamers.dual.IDualStreamer
import io.github.thibaultbee.streampack.core.streamers.dual.IVideoDualStreamer
import io.github.thibaultbee.streampack.core.streamers.single.AudioConfig
import io.github.thibaultbee.streampack.core.streamers.single.IAudioSingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.ISingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.IVideoSingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.VideoConfig
import io.github.thibaultbee.streampack.core.streamers.utils.MediaProjectionUtils
import io.github.thibaultbee.streampack.ext.srt.data.mediadescriptor.SrtMediaDescriptor
import io.github.thibaultbee.streampack.screenrecorder.databinding.ActivityMainBinding
import io.github.thibaultbee.streampack.screenrecorder.models.EndpointType
import io.github.thibaultbee.streampack.screenrecorder.services.DemoMediaProjectionService
import io.github.thibaultbee.streampack.screenrecorder.services.DemoMediaProjectionService.Companion.AUDIO_SOURCE_KEY
import io.github.thibaultbee.streampack.screenrecorder.services.DemoMediaProjectionService.Companion.AUDIO_SOURCE_MICROPHONE_KEY
import io.github.thibaultbee.streampack.screenrecorder.settings.SettingsActivity
import io.github.thibaultbee.streampack.services.MediaProjectionService
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val configuration by lazy {
        Configuration(this)
    }

    private val tsServiceInfo: TSServiceInfo
        get() = TSServiceInfo(
            TSServiceInfo.ServiceType.DIGITAL_TV,
            0x4698,
            configuration.muxer.service,
            configuration.muxer.provider
        )

    private var connection: ServiceConnection? = null

    private var streamer: IVideoStreamer<*>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()

        binding.actions.setOnClickListener {
            showPopup()
        }

        binding.liveButton.setOnCheckedChangeListener { view, isChecked ->
            if (view.isPressed) {
                if (isChecked) {
                    requestAudioPermissionsLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                    runBlocking {
                        streamer?.stopStream()
                        streamer?.close()
                    }
                    connection?.let { unbindService(it) }
                    connection = null
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService()
    }

    private val requestAudioPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            showPermissionAlertDialog(this) { this.finish() }
        } else {
            getContent.launch(
                MediaProjectionUtils.createScreenCaptureIntent(
                    this
                )
            )
        }
    }

    private var getContent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            connection = MediaProjectionService.bindService(
                context = this,
                serviceClass = DemoMediaProjectionService::class.java,
                resultCode = result.resultCode,
                resultData = result.data!!,
                onServiceCreated = { streamer ->
                    streamer as IVideoStreamer<*> // Cast to streamer type
                    lifecycleScope.launch {
                        try {
                            configure(streamer)
                        } catch (t: Throwable) {
                            this@MainActivity.showAlertDialog(
                                this@MainActivity, "Error", t.message ?: "Unknown error"
                            )
                            binding.liveButton.isChecked = false
                            Log.e(TAG, "Error while starting streamer", t)
                        }
                        startStream(streamer)
                    }
                    lifecycleScope.launch {
                        streamer.isStreamingFlow.collect { isStreaming ->
                            binding.liveButton.isChecked = isStreaming
                        }
                    }
                    this.streamer = streamer
                },
                onServiceDisconnected = {
                    streamer = null
                    Log.i(TAG, "Service disconnected")
                },
                onExtra = { extra ->
                    extra.putExtra(AUDIO_SOURCE_KEY, AUDIO_SOURCE_MICROPHONE_KEY)
                }
            )
        }

    private fun configure(streamer: IVideoStreamer<*>) {
        val deviceRefreshRate =
            (this.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).getDisplay(
                Display.DEFAULT_DISPLAY
            ).refreshRate.toInt()
        val fps = if (MediaCodecHelper.Video.getFramerateRange(configuration.video.encoder)
                .contains(deviceRefreshRate)
        ) {
            deviceRefreshRate
        } else {
            30
        }

        val videoConfig = VideoConfig(
            mimeType = configuration.video.encoder,
            startBitrate = configuration.video.bitrate * 1000, // to b/s
            resolution = configuration.video.resolution,
            fps = fps
        )
        lifecycleScope.launch {
            when (streamer) {
                is IVideoSingleStreamer -> streamer.setVideoConfig(videoConfig)
                is IVideoDualStreamer -> streamer.setVideoConfig(DualStreamerVideoConfig(videoConfig))
                else -> throw IllegalStateException("Streamer is not a supported streamer")
            }
        }

        if (streamer !is IConfigurableAudioStreamer<*>) {
            Log.e(TAG, "Streamer does not support audio configuration")
            return
        }

        val audioConfig = AudioConfig(
            mimeType = configuration.audio.encoder,
            startBitrate = configuration.audio.bitrate,
            sampleRate = configuration.audio.sampleRate,
            channelConfig = AudioConfig.getChannelConfig(configuration.audio.numberOfChannels),
            byteFormat = configuration.audio.byteFormat
        )

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            lifecycleScope.launch {
                when (streamer) {
                    is IAudioSingleStreamer -> streamer.setAudioConfig(audioConfig)
                    is IAudioDualStreamer -> streamer.setAudioConfig(
                        DualStreamerAudioConfig(
                            audioConfig
                        )
                    )

                    else -> throw IllegalStateException("Streamer is not a audio streamer")
                }
            }
        } else {
            throw SecurityException("Permission RECORD_AUDIO must have been granted!")
        }
    }

    private suspend fun startStream(streamer: IVideoStreamer<*>) {
        try {
            val descriptor = when (configuration.endpoint.type) {
                EndpointType.SRT -> SrtMediaDescriptor(
                    configuration.endpoint.srt.ip,
                    configuration.endpoint.srt.port,
                    configuration.endpoint.srt.streamID,
                    configuration.endpoint.srt.passPhrase,
                    serviceInfo = tsServiceInfo
                )

                EndpointType.RTMP -> UriMediaDescriptor(configuration.endpoint.rtmp.url.toUri())
            }

            when (streamer) {
                is ISingleStreamer -> streamer.startStream(descriptor)

                is IDualStreamer -> {
                    Log.w(TAG, "Only first output will be used")
                    streamer.first.startStream(descriptor)
                }

                else -> throw IllegalStateException("Streamer is not a supported streamer")
            }
            moveTaskToBack(true)
        } catch (t: Throwable) {
            this.showAlertDialog(
                this, "Error", t.message ?: "Unknown error"
            )
            Log.e(TAG, "Error while starting streamer", t)
        }
    }

    private fun stopService() {
        connection?.let { unbindService(it) }
        connection = null
        val intent = Intent(this, DemoMediaProjectionService::class.java)
        stopService(intent)
    }

    private fun showPopup() {
        val popup = PopupMenu(this, binding.actions)
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.actions, popup.menu)
        popup.show()
        popup.setOnMenuItemClickListener {
            if (it.itemId == R.id.action_settings) {
                goToSettingsActivity()
            } else {
                Log.e(TAG, "Unknown menu item ${it.itemId}")
            }
            true
        }
    }

    private fun goToSettingsActivity() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.actions, menu)
        return true
    }

    private fun showAlertDialog(
        context: Context, title: String, message: String, afterPositiveButton: () -> Unit = {}
    ) {
        AlertDialog.Builder(context).setTitle(title).setMessage(message)
            .setPositiveButton(android.R.string.ok) { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
                afterPositiveButton()
            }.show()
    }

    private fun showAlertDialog(
        context: Context,
        titleResourceId: Int,
        messageResourceId: Int,
        afterPositiveButton: () -> Unit = {}
    ) {
        AlertDialog.Builder(context).setTitle(titleResourceId).setMessage(messageResourceId)
            .setPositiveButton(android.R.string.ok) { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
                afterPositiveButton()
            }.show()
    }

    private fun showPermissionAlertDialog(
        context: Context, afterPositiveButton: () -> Unit = {}
    ) = showAlertDialog(
        context, R.string.permission, R.string.permission_not_granted, afterPositiveButton
    )

    companion object {
        private const val TAG = "MainActivity"
    }
}
