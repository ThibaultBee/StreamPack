package io.github.thibaultbee.streampack.app.data.storage

import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Range
import android.util.Size
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.thibaultbee.streampack.app.ApplicationConstants
import io.github.thibaultbee.streampack.app.R
import io.github.thibaultbee.streampack.app.models.EndpointType
import io.github.thibaultbee.streampack.app.models.FileExtension
import io.github.thibaultbee.streampack.app.utils.appendIfNotEndsWith
import io.github.thibaultbee.streampack.app.utils.createVideoContentUri
import io.github.thibaultbee.streampack.core.configuration.BitrateRegulatorConfig
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.core.streamers.single.AudioConfig
import io.github.thibaultbee.streampack.core.streamers.single.VideoConfig
import io.github.thibaultbee.streampack.ext.srt.data.mediadescriptor.SrtMediaDescriptor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * A repository for storage data.
 *
 * Most of the stored value are [stringPreferencesKey] because of the usage of preference screen.
 */
class DataStoreRepository(
    private val context: Context, private val dataStore: DataStore<Preferences>
) {
    val isAudioEnableFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[booleanPreferencesKey(context.getString(R.string.audio_enable_key))] ?: true
    }.distinctUntilChanged()

    val audioConfigFlow: Flow<AudioConfig?> = dataStore.data.map { preferences ->
        val isAudioEnable =
            preferences[booleanPreferencesKey(context.getString(R.string.audio_enable_key))] ?: true
        if (!isAudioEnable) {
            return@map null
        }

        val mimeType =
            preferences[stringPreferencesKey(context.getString(R.string.audio_encoder_key))]
                ?: ApplicationConstants.Audio.defaultEncoder
        val startBitrate =
            preferences[stringPreferencesKey(context.getString(R.string.audio_bitrate_key))]?.toInt()
                ?: ApplicationConstants.Audio.defaultBitrateInBps

        val channelConfig =
            preferences[stringPreferencesKey(context.getString(R.string.audio_channel_config_key))]?.toInt()
                ?: ApplicationConstants.Audio.defaultChannelConfig
        val sampleRate =
            preferences[stringPreferencesKey(context.getString(R.string.audio_sample_rate_key))]?.toInt()
                ?: ApplicationConstants.Audio.defaultSampleRate
        val byteFormat =
            preferences[stringPreferencesKey(context.getString(R.string.audio_byte_format_key))]?.toInt()
                ?: ApplicationConstants.Audio.defaultByteFormat
        val profile =
            preferences[stringPreferencesKey(context.getString(R.string.audio_profile_key))]?.toInt()
                ?: if (mimeType == MediaFormat.MIMETYPE_AUDIO_AAC) {
                    MediaCodecInfo.CodecProfileLevel.AACObjectLC
                } else {
                    0
                }
        AudioConfig(
            mimeType = mimeType,
            channelConfig = channelConfig,
            startBitrate = startBitrate,
            sampleRate = sampleRate,
            byteFormat = byteFormat,
            profile = profile
        )
    }.distinctUntilChanged()

    val isVideoEnableFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[booleanPreferencesKey(context.getString(R.string.video_enable_key))] ?: true
    }.distinctUntilChanged()

    val videoConfigFlow: Flow<VideoConfig?> = dataStore.data.map { preferences ->
        val isVideoEnable =
            preferences[booleanPreferencesKey(context.getString(R.string.video_enable_key))] ?: true
        if (!isVideoEnable) {
            return@map null
        }

        val mimeType =
            preferences[stringPreferencesKey(context.getString(R.string.video_encoder_key))]
                ?: ApplicationConstants.Video.defaultEncoder
        val startBitrate =
            preferences[intPreferencesKey(context.getString(R.string.video_bitrate_key))]?.times(
                1000
            ) ?: ApplicationConstants.Video.defaultBitrateInBps

        val resolution =
            preferences[stringPreferencesKey(context.getString(R.string.video_resolution_key))]?.split(
                "x"
            )?.let { Size(it[0].toInt(), it[1].toInt()) }
                ?: ApplicationConstants.Video.defaultResolution
        val fps =
            preferences[stringPreferencesKey(context.getString(R.string.video_fps_key))]?.toInt()
                ?: ApplicationConstants.Video.defaultFps
        val profile =
            preferences[stringPreferencesKey(context.getString(R.string.video_profile_key))]?.toInt()
                ?: VideoConfig.getBestProfile(mimeType)
        val level =
            preferences[stringPreferencesKey(context.getString(R.string.video_level_key))]?.toInt()
                ?: VideoConfig.getBestLevel(mimeType, profile)
        VideoConfig(
            mimeType = mimeType,
            startBitrate = startBitrate,
            resolution = resolution,
            fps = fps.toFloat(),
            profile = profile,
            level = level
        )
    }.distinctUntilChanged()

    val endpointDescriptorFlow: Flow<MediaDescriptor> = dataStore.data.map { preferences ->
        val endpointTypeId =
            preferences[stringPreferencesKey(context.getString(R.string.endpoint_type_key))]?.toInt()
                ?: EndpointType.SRT.id
        when (val endpointType = EndpointType.fromId(endpointTypeId)) {
            EndpointType.TS_FILE,
            EndpointType.FLV_FILE,
            EndpointType.MP4_FILE,
            EndpointType.WEBM_FILE,
            EndpointType.OGG_FILE,
            EndpointType.THREEGP_FILE -> {
                val filename =
                    preferences[stringPreferencesKey(context.getString(R.string.file_endpoint_key))]
                        ?: "StreamPack"
                UriMediaDescriptor(
                    context,
                    context.createVideoContentUri(
                        filename.appendIfNotEndsWith(FileExtension.fromEndpointType(endpointType).extension)
                    )
                )
            }

            EndpointType.SRT -> {
                val ip =
                    preferences[stringPreferencesKey(context.getString(R.string.srt_server_ip_key))]
                        ?: context.getString(R.string.default_srt_server_url)
                val port =
                    preferences[stringPreferencesKey(context.getString(R.string.srt_server_port_key))]?.toInt()
                        ?: 9998
                val streamId =
                    preferences[stringPreferencesKey(context.getString(R.string.srt_server_stream_id_key))]
                        ?: ""
                val passPhrase =
                    preferences[stringPreferencesKey(context.getString(R.string.srt_server_passphrase_key))]
                        ?: context.getString(R.string.default_srt_server_passphrase)
                SrtMediaDescriptor(
                    host = ip,
                    port = port,
                    streamId = streamId,
                    passPhrase = passPhrase
                )
            }

            EndpointType.RTMP -> {
                val url =
                    preferences[stringPreferencesKey(context.getString(R.string.rtmp_server_url_key))]
                        ?: context.getString(R.string.default_rtmp_url)
                UriMediaDescriptor(context, url)
            }
        }
    }.distinctUntilChanged()

    val bitrateRegulatorConfigFlow: Flow<BitrateRegulatorConfig?> =
        dataStore.data.map { preferences ->
            val isBitrateRegulatorEnable =
                preferences[booleanPreferencesKey(context.getString(R.string.srt_server_enable_bitrate_regulation_key))]
                    ?: true
            if (!isBitrateRegulatorEnable) {
                return@map null
            }

            val videoMinBitrate =
                preferences[intPreferencesKey(context.getString(R.string.srt_server_video_min_bitrate_key))]?.toInt()
                    ?.times(1000)
                    ?: 300000
            val videoMaxBitrate =
                preferences[intPreferencesKey(context.getString(R.string.srt_server_video_target_bitrate_key))]?.toInt()
                    ?.times(1000)
                    ?: 10000000
            BitrateRegulatorConfig(
                videoBitrateRange = Range(videoMinBitrate, videoMaxBitrate)
            )
        }
}
