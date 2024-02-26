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
import io.github.thibaultbee.streampack.data.AudioConfig
import io.github.thibaultbee.streampack.data.BitrateRegulatorConfig
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.ext.rtmp.services.ScreenRecorderRtmpLiveService
import io.github.thibaultbee.streampack.ext.srt.data.SrtConnectionDescriptor
import io.github.thibaultbee.streampack.ext.srt.services.ScreenRecorderSrtLiveService
import io.github.thibaultbee.streampack.ext.srt.streamers.interfaces.ISrtLiveStreamer
import io.github.thibaultbee.streampack.internal.encoders.mediacodec.MediaCodecHelper
import io.github.thibaultbee.streampack.internal.endpoints.muxers.ts.data.TsServiceInfo
import io.github.thibaultbee.streampack.screenrecorder.databinding.ActivityMainBinding
import io.github.thibaultbee.streampack.screenrecorder.models.EndpointType
import io.github.thibaultbee.streampack.screenrecorder.services.DemoScreenRecorderRtmpLiveService
import io.github.thibaultbee.streampack.screenrecorder.services.DemoScreenRecorderSrtLiveService
import io.github.thibaultbee.streampack.screenrecorder.settings.SettingsActivity
import io.github.thibaultbee.streampack.streamers.bases.BaseScreenRecorderStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.ILiveStreamer
import io.github.thibaultbee.streampack.streamers.live.BaseScreenRecorderLiveStreamer
import io.github.thibaultbee.streampack.utils.getStreamer
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val configuration by lazy {
        Configuration(this)
    }
    private lateinit var streamer: BaseScreenRecorderLiveStreamer

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

        binding.liveButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestAudioPermissionsLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                stopService()
            }
        }
    }

    private val requestAudioPermissionsLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (!isGranted) {
                showPermissionAlertDialog(this) { this.finish() }
            } else {
                getContent.launch(
                    BaseScreenRecorderStreamer.createScreenRecorderIntent(
                        this
                    )
                )
            }
        }

    private var getContent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (configuration.endpoint.type) {
                EndpointType.SRT -> {
                    ScreenRecorderSrtLiveService.launch(
                        this,
                        DemoScreenRecorderSrtLiveService::class.java,
                        configuration.audio.enable,
                        TsServiceInfo(
                            TsServiceInfo.ServiceType.DIGITAL_TV,
                            0x4698,
                            configuration.muxer.service,
                            configuration.muxer.provider
                        ),
                        configuration.endpoint.srt.enableBitrateRegulation,
                        BitrateRegulatorConfig(videoBitrateRange = configuration.endpoint.srt.videoBitrateRange),
                        { streamer ->
                            this.streamer = streamer.apply {
                                activityResult = result
                            }
                            try {
                                configureAndStart()
                                moveTaskToBack(true)
                            } catch (e: Exception) {
                                this@MainActivity.showAlertDialog(
                                    this@MainActivity,
                                    "Error",
                                    e.message ?: "Unknown error"
                                )
                                binding.liveButton.isChecked = false
                                Log.e(TAG, "Error while starting streamer", e)
                            }
                        },
                        {
                            binding.liveButton.isChecked = false
                            Log.i(TAG, "Service disconnected")
                        })
                }

                EndpointType.RTMP -> {
                    ScreenRecorderRtmpLiveService.launch(
                        this,
                        DemoScreenRecorderRtmpLiveService::class.java,
                        configuration.audio.enable,
                        { streamer ->
                            this.streamer = streamer.apply {
                                activityResult = result
                            }
                            try {
                                configureAndStart()
                                moveTaskToBack(true)
                            } catch (e: Exception) {
                                this@MainActivity.showAlertDialog(
                                    this@MainActivity,
                                    "Error",
                                    e.message ?: "Unknown error"
                                )
                                binding.liveButton.isChecked = false
                                Log.e(TAG, "Error while starting streamer", e)
                            }
                        },
                        {
                            this@MainActivity.showAlertDialog(
                                this@MainActivity,
                                "Error",
                                "Service disconnected"
                            )
                            binding.liveButton.isChecked = false
                            Log.i(TAG, "Service disconnected")
                        })
                }
            }
        }

    private fun configureAndStart() {
        val deviceRefreshRate =
            (this.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).getDisplay(
                Display.DEFAULT_DISPLAY
            ).refreshRate.toInt()
        val fps =
            if (MediaCodecHelper.Video.getFramerateRange(configuration.video.encoder)
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
        streamer.configure(videoConfig)

        if (configuration.audio.enable) {
            val audioConfig = AudioConfig(
                mimeType = configuration.audio.encoder,
                startBitrate = configuration.audio.bitrate,
                sampleRate = configuration.audio.sampleRate,
                channelConfig = AudioConfig.getChannelConfig(configuration.audio.numberOfChannels),
                byteFormat = configuration.audio.byteFormat,
                enableEchoCanceler = configuration.audio.enableEchoCanceler,
                enableNoiseSuppressor = configuration.audio.enableNoiseSuppressor
            )

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                streamer.configure(audioConfig)
            } else {
                throw SecurityException("Permission RECORD_AUDIO must have been granted!")
            }
        }

        runBlocking {
            streamer.getStreamer<ISrtLiveStreamer>()?.let {
                val connection = SrtConnectionDescriptor(
                    configuration.endpoint.srt.ip,
                    configuration.endpoint.srt.port,
                    configuration.endpoint.srt.streamID,
                    configuration.endpoint.srt.passPhrase
                )
                it.connect(connection)
            } ?: streamer.getStreamer<ILiveStreamer>()?.connect(
                configuration.endpoint.rtmp.url
            )

            streamer.startStream()
        }
    }

    private fun stopService() {
        runBlocking {
            streamer.stopStream()
        }
        streamer.disconnect()

        when (configuration.endpoint.type) {
            EndpointType.SRT -> stopService(
                Intent(
                    this,
                    DemoScreenRecorderSrtLiveService::class.java
                )
            )

            EndpointType.RTMP -> stopService(
                Intent(
                    this,
                    DemoScreenRecorderRtmpLiveService::class.java
                )
            )
        }
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
        context: Context,
        title: String,
        message: String,
        afterPositiveButton: () -> Unit = {}
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
                afterPositiveButton()
            }
            .show()
    }

    private fun showAlertDialog(
        context: Context,
        titleResourceId: Int,
        messageResourceId: Int,
        afterPositiveButton: () -> Unit = {}
    ) {
        AlertDialog.Builder(context)
            .setTitle(titleResourceId)
            .setMessage(messageResourceId)
            .setPositiveButton(android.R.string.ok) { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
                afterPositiveButton()
            }
            .show()
    }

    private fun showPermissionAlertDialog(context: Context, afterPositiveButton: () -> Unit = {}) =
        showAlertDialog(
            context,
            R.string.permission,
            R.string.permission_not_granted,
            afterPositiveButton
        )

    companion object {
        private const val TAG = "MainActivity"
    }
}
