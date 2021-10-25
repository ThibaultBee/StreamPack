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
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.ACTIVITY_RESULT_KEY
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.AUDIO_CONFIG_KEY
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.BITRATE
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.BYTE_FORMAT
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.CHANNEL_CONFIG
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.CONNECTION_CONFIG_KEY
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.ENABLE_BITRATE_REGULATION
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.ENABLE_ECHO_CANCELER
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.ENABLE_NOISE_SUPPRESSOR
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.IP
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.MIME_TYPE
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.MUXER_CONFIG_KEY
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.PASSPHRASE
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.PORT
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.PROVIDER
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.RESOLUTION_HEIGHT
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.RESOLUTION_WIDTH
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.SAMPLE_RATE
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.SERVICE
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.STREAM_ID
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.VIDEO_BITRATE_REGULATION_LOWER
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.VIDEO_BITRATE_REGULATION_UPPER
import com.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.VIDEO_CONFIG_KEY
import com.github.thibaultbee.streampack.screenrecorder.databinding.ActivityMainBinding
import com.github.thibaultbee.streampack.screenrecorder.settings.SettingsActivity
import com.github.thibaultbee.streampack.streamers.BaseScreenRecorderStreamer
import com.tbruyelle.rxpermissions3.RxPermissions

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val tag = this::class.simpleName
    private val configuration by lazy {
        Configuration(this)
    }
    private val rxPermissions: RxPermissions by lazy { RxPermissions(this) }

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
                rxPermissions
                    .request(Manifest.permission.RECORD_AUDIO)
                    .subscribe { granted ->
                        if (!granted) {
                            showPermissionAlertDialog(this) { this.finish() }
                        } else {
                            getContent.launch(
                                BaseScreenRecorderStreamer.createScreenRecorderIntent(
                                    this
                                )
                            )
                        }
                    }
            } else {
                stopService(Intent(this, ScreenRecorderService::class.java))
            }
        }
    }

    private var getContent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val intent = Intent(this, ScreenRecorderService::class.java)
            intent.putExtra(AUDIO_CONFIG_KEY, createAudioConfigBundle())
            intent.putExtra(VIDEO_CONFIG_KEY, createVideoConfigBundle())
            intent.putExtra(MUXER_CONFIG_KEY, createMuxerConfigBundle())
            intent.putExtra(CONNECTION_CONFIG_KEY, createConnectionConfigBundle())
            intent.putExtra(ACTIVITY_RESULT_KEY, result)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            moveTaskToBack(true)
        }

    private fun createMuxerConfigBundle(): Bundle {
        val bundle = Bundle()
        bundle.putString(PROVIDER, configuration.muxer.provider)
        bundle.putString(SERVICE, configuration.muxer.service)
        return bundle
    }

    private fun createConnectionConfigBundle(): Bundle {
        val bundle = Bundle()
        bundle.putString(IP, configuration.endpoint.connection.ip)
        bundle.putInt(PORT, configuration.endpoint.connection.port)
        bundle.putString(PASSPHRASE, configuration.endpoint.connection.passPhrase)
        bundle.putString(STREAM_ID, configuration.endpoint.connection.streamID)
        bundle.putBoolean(
            ENABLE_BITRATE_REGULATION,
            configuration.endpoint.connection.enableBitrateRegulation
        )
        bundle.putInt(
            VIDEO_BITRATE_REGULATION_LOWER,
            configuration.endpoint.connection.videoBitrateRange.lower
        )
        bundle.putInt(
            VIDEO_BITRATE_REGULATION_UPPER,
            configuration.endpoint.connection.videoBitrateRange.upper
        )
        return bundle
    }

    private fun createAudioConfigBundle(): Bundle {
        val bundle = Bundle()
        bundle.putString(MIME_TYPE, configuration.audio.encoder)
        bundle.putInt(BITRATE, configuration.audio.bitrate)
        bundle.putInt(SAMPLE_RATE, configuration.audio.sampleRate)
        bundle.putInt(CHANNEL_CONFIG, configuration.audio.numberOfChannels)
        bundle.putInt(BYTE_FORMAT, configuration.audio.byteFormat)
        bundle.putBoolean(ENABLE_ECHO_CANCELER, configuration.audio.enableEchoCanceler)
        bundle.putBoolean(ENABLE_NOISE_SUPPRESSOR, configuration.audio.enableNoiseSuppressor)
        return bundle
    }

    private fun createVideoConfigBundle(): Bundle {
        val bundle = Bundle()
        bundle.putString(MIME_TYPE, configuration.video.encoder)
        bundle.putInt(BITRATE, configuration.video.bitrate * 1000)  // to b/s
        bundle.putInt(RESOLUTION_WIDTH, configuration.video.resolution.width)
        bundle.putInt(RESOLUTION_HEIGHT, configuration.video.resolution.height)
        return bundle
    }

    private fun showPopup() {
        val popup = PopupMenu(this, binding.actions)
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.actions, popup.menu)
        popup.show()
        popup.setOnMenuItemClickListener { it ->
            if (it.itemId == R.id.action_settings) {
                goToSettingsActivity()
            } else {
                Log.e(tag, "Unknown menu item ${it.itemId}")
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

    fun showPermissionAlertDialog(context: Context, afterPositiveButton: () -> Unit = {}) {
        AlertDialog.Builder(context)
            .setTitle(R.string.permission)
            .setMessage(R.string.permission_not_granted)
            .setPositiveButton(android.R.string.ok) { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
                afterPositiveButton()
            }
            .show()
    }
}
