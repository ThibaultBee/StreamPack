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
package io.github.thibaultbee.streampack.screenrecorder.settings

import android.media.AudioFormat
import android.media.MediaFormat
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.util.Size
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreference
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.CompositeEndpoint
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.flv.FlvMuxerInfo
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.ts.TSMuxerInfo
import io.github.thibaultbee.streampack.core.streamers.infos.StreamerConfigurationInfo
import io.github.thibaultbee.streampack.screenrecorder.R
import io.github.thibaultbee.streampack.screenrecorder.models.EndpointFactory
import io.github.thibaultbee.streampack.screenrecorder.models.EndpointType

class SettingsFragment : PreferenceFragmentCompat() {
    private lateinit var streamerInfo: StreamerConfigurationInfo

    private val videoEncoderListPreference: ListPreference by lazy {
        this.findPreference(getString(R.string.video_encoder_key))!!
    }

    private val videoResolutionListPreference: ListPreference by lazy {
        this.findPreference(getString(R.string.video_resolution_key))!!
    }

    private val videoBitrateSeekBar: SeekBarPreference by lazy {
        this.findPreference(getString(R.string.video_bitrate_key))!!
    }

    private val audioEnablePreference: SwitchPreference by lazy {
        this.findPreference(getString(R.string.audio_enable_key))!!
    }

    private val audioSettingsCategory: PreferenceCategory by lazy {
        this.findPreference(getString(R.string.audio_settings_key))!!
    }

    private val audioEncoderListPreference: ListPreference by lazy {
        this.findPreference(getString(R.string.audio_encoder_key))!!
    }

    private val audioNumberOfChannelListPreference: ListPreference by lazy {
        this.findPreference(getString(R.string.audio_number_of_channels_key))!!
    }

    private val audioBitrateListPreference: ListPreference by lazy {
        this.findPreference(getString(R.string.audio_bitrate_key))!!
    }

    private val audioSampleRateListPreference: ListPreference by lazy {
        this.findPreference(getString(R.string.audio_sample_rate_key))!!
    }

    private val audioByteFormatListPreference: ListPreference by lazy {
        this.findPreference(getString(R.string.audio_byte_format_key))!!
    }

    private val endpointTypePreference: ListPreference by lazy {
        this.findPreference(getString(R.string.endpoint_type_key))!!
    }

    private val tsMuxerPreference: PreferenceCategory by lazy {
        this.findPreference(getString(R.string.ts_muxer_key))!!
    }

    private val rtmpEndpointPreference: PreferenceCategory by lazy {
        this.findPreference(getString(R.string.rtmp_server_key))!!
    }

    private val srtEndpointPreference: PreferenceCategory by lazy {
        this.findPreference(getString(R.string.srt_server_key))!!
    }

    private val serverIpPreference: EditTextPreference by lazy {
        this.findPreference(getString(R.string.server_ip_key))!!
    }

    private val serverPortPreference: EditTextPreference by lazy {
        this.findPreference(getString(R.string.server_port_key))!!
    }

    private val serverEnableBitrateRegulationPreference: SwitchPreference by lazy {
        this.findPreference(getString(R.string.server_enable_bitrate_regulation_key))!!
    }

    private val serverTargetVideoBitratePreference: SeekBarPreference by lazy {
        this.findPreference(getString(R.string.server_video_target_bitrate_key))!!
    }

    private val serverMinVideoBitratePreference: SeekBarPreference by lazy {
        this.findPreference(getString(R.string.server_video_min_bitrate_key))!!
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()
        loadPreferences()
    }

    private fun setEndpointType(id: Int) {
        val endpointType = EndpointType.fromId(id)
        val endpoint = EndpointFactory(
            endpointType
        ).build()
        srtEndpointPreference.isVisible = endpoint.hasSrtCapabilities
        rtmpEndpointPreference.isVisible = endpoint.hasRtmpCapabilities
        tsMuxerPreference.isVisible = endpoint.hasTSCapabilities

        // Update supported values with a new info
        streamerInfo = when (endpointType) {
            EndpointType.SRT -> StreamerConfigurationInfo(CompositeEndpoint.EndpointInfo(TSMuxerInfo))
            EndpointType.RTMP -> StreamerConfigurationInfo(
                CompositeEndpoint.EndpointInfo(
                    FlvMuxerInfo
                )
            )
        }
        loadVideoSettings()
        loadAudioSettings()
    }

    private fun loadVideoSettings() {
        // Inflates video encoders
        val supportedVideoEncoderName =
            mapOf(
                MediaFormat.MIMETYPE_VIDEO_AVC to getString(R.string.video_encoder_h264),
                MediaFormat.MIMETYPE_VIDEO_HEVC to getString(R.string.video_encoder_h265),
                MediaFormat.MIMETYPE_VIDEO_H263 to getString(R.string.video_encoder_h263)
            )

        val supportedVideoEncoder = streamerInfo.video.supportedEncoders
        videoEncoderListPreference.setDefaultValue(MediaFormat.MIMETYPE_VIDEO_AVC)
        videoEncoderListPreference.entryValues = supportedVideoEncoder.toTypedArray()
        videoEncoderListPreference.entries =
            supportedVideoEncoder.map { supportedVideoEncoderName[it] }.toTypedArray()

        loadVideoSettings(videoEncoderListPreference.value)
    }

    private fun loadVideoSettings(encoder: String) {
        // Inflates video resolutions
        val encoderSupportedResolution = streamerInfo.video.getSupportedResolutions(encoder)
        listOf(
            Size(1920, 1080),
            Size(1280, 720),
            Size(640, 360),
            Size(640, 480)
        ).filter {
            encoderSupportedResolution.first.contains(it.width) && encoderSupportedResolution.second.contains(
                it.height
            )
        }.map { it.toString() }.toTypedArray().run {
            videoResolutionListPreference.entries = this
            videoResolutionListPreference.entryValues = this
        }

        // Inflates video bitrate
        streamerInfo.video.getSupportedBitrates(encoder).run {
            videoBitrateSeekBar.max = minOf(videoBitrateSeekBar.max, upper / 1000) // to kb/s
        }
    }

    private fun loadAudioSettings() {
        // Inflates audio encoders
        val supportedAudioEncoderName =
            mapOf(
                MediaFormat.MIMETYPE_AUDIO_AAC to getString(R.string.audio_encoder_aac),
                MediaFormat.MIMETYPE_AUDIO_OPUS to getString(R.string.audio_encoder_opus)
            )

        val supportedAudioEncoder = streamerInfo.audio.supportedEncoders
        audioEncoderListPreference.setDefaultValue(MediaFormat.MIMETYPE_AUDIO_AAC)
        audioEncoderListPreference.entryValues = supportedAudioEncoder.toTypedArray()
        audioEncoderListPreference.entries =
            supportedAudioEncoder.map { supportedAudioEncoderName[it] }.toTypedArray()
        if (audioEncoderListPreference.entry == null) {
            audioEncoderListPreference.value = MediaFormat.MIMETYPE_AUDIO_AAC
        }
        audioEncoderListPreference.setOnPreferenceChangeListener { _, newValue ->
            loadAudioSettings(newValue as String)
            true
        }

        loadAudioSettings(audioEncoderListPreference.value)
    }

    private fun loadAudioSettings(encoder: String) {
        audioSettingsCategory.isVisible = audioEnablePreference.isChecked
        audioEnablePreference.setOnPreferenceChangeListener { _, newValue ->
            audioSettingsCategory.isVisible = newValue as Boolean
            true
        }

        // Inflates audio number of channel
        val inputChannelRange =
            streamerInfo.audio.getSupportedInputChannelRange(encoder)
        audioNumberOfChannelListPreference.entryValues.filter {
            inputChannelRange.contains(it.toString().toInt())
        }.toTypedArray().run {
            audioNumberOfChannelListPreference.entries = this
            audioNumberOfChannelListPreference.entryValues = this
        }

        // Inflates audio bitrate
        val bitrateRange = streamerInfo.audio.getSupportedBitrates(encoder)
        audioBitrateListPreference.entryValues.filter {
            bitrateRange.contains(
                it.toString().toInt()
            )
        }.toTypedArray().run {
            audioBitrateListPreference.entries =
                this.map { "${it.toString().toInt() / 1000} Kbps" }.toTypedArray()
            audioBitrateListPreference.entryValues = this
        }

        // Inflates audio sample rate
        val sampleRates = streamerInfo.audio.getSupportedSampleRates(encoder)
        audioSampleRateListPreference.entries =
            sampleRates.map { "${"%.1f".format(it.toString().toFloat() / 1000)} kHz" }
                .toTypedArray()
        audioSampleRateListPreference.entryValues = sampleRates.map { "$it" }.toTypedArray()
        if (audioSampleRateListPreference.entry == null) {
            audioSampleRateListPreference.value = when {
                sampleRates.contains(44100) -> "44100"
                sampleRates.contains(48000) -> "48000"
                else -> "${sampleRates.first()}"
            }
        }

        // Inflates audio byte format
        val supportedByteFormatName =
            mapOf(
                AudioFormat.ENCODING_PCM_8BIT to getString(R.string.audio_byte_format_8bit),
                AudioFormat.ENCODING_PCM_16BIT to getString(R.string.audio_byte_format_16bit),
                AudioFormat.ENCODING_PCM_FLOAT to getString(R.string.audio_byte_format_float)
            )
        val byteFormats = streamerInfo.audio.getSupportedByteFormats()
        audioByteFormatListPreference.entries =
            byteFormats.map { supportedByteFormatName[it] }.toTypedArray()
        audioByteFormatListPreference.entryValues = byteFormats.map { "$it" }.toTypedArray()
        if (audioByteFormatListPreference.entry == null) {
            audioByteFormatListPreference.value = "${AudioFormat.ENCODING_PCM_16BIT}"
        }
    }

    private fun loadEndpoint() {
        // Inflates endpoint
        val supportedEndpointName =
            mapOf(
                EndpointType.SRT to getString(R.string.to_srt),
                EndpointType.RTMP to getString(R.string.to_rtmp),
            )
        val supportedEndpoint = EndpointType.entries.map { "${it.id}" }.toTypedArray()
        endpointTypePreference.setDefaultValue(EndpointType.SRT.id)
        endpointTypePreference.entryValues = supportedEndpoint
        endpointTypePreference.entries =
            supportedEndpoint.map { supportedEndpointName[EndpointType.fromId(it.toInt())] }
                .toTypedArray()
        setEndpointType(endpointTypePreference.value.toInt())
        endpointTypePreference.setOnPreferenceChangeListener { _, newValue ->
            setEndpointType((newValue as String).toInt())
            true
        }

        serverIpPreference.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_TEXT_VARIATION_URI
        }

        serverPortPreference.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.filters = arrayOf(InputFilter.LengthFilter(5))
        }

        serverTargetVideoBitratePreference.isVisible =
            serverEnableBitrateRegulationPreference.isChecked
        serverMinVideoBitratePreference.isVisible =
            serverEnableBitrateRegulationPreference.isChecked
        serverEnableBitrateRegulationPreference.setOnPreferenceChangeListener { _, newValue ->
            serverTargetVideoBitratePreference.isVisible = newValue as Boolean
            serverMinVideoBitratePreference.isVisible = newValue
            true
        }

        serverTargetVideoBitratePreference.setOnPreferenceChangeListener { _, newValue ->
            if ((newValue as Int) < serverMinVideoBitratePreference.value) {
                serverMinVideoBitratePreference.value = newValue
            }
            true
        }

        serverMinVideoBitratePreference.setOnPreferenceChangeListener { _, newValue ->
            if ((newValue as Int) > serverTargetVideoBitratePreference.value) {
                serverTargetVideoBitratePreference.value = newValue
            }
            true
        }
    }

    private fun loadPreferences() {
        loadEndpoint()
    }
}