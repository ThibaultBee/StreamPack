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
package io.github.thibaultbee.streampack.streamers.interfaces.builders

import android.content.Context
import io.github.thibaultbee.streampack.data.AudioConfig
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.listeners.OnErrorListener
import io.github.thibaultbee.streampack.logger.ILogger
import io.github.thibaultbee.streampack.streamers.bases.BaseStreamer

interface IStreamerBuilder {

    /**
     * Set application context. It is mandatory to set context.
     *
     * @param context application context.
     */
    fun setContext(context: Context): IStreamerBuilder

    /**
     * Set logger.
     *
     * @param logger [ILogger] implementation
     */
    fun setLogger(logger: ILogger): IStreamerBuilder

    /**
     * Set both audio and video configuration.
     * Configurations can be change later with [BaseStreamer.configure].
     * Same as calling both [setAudioConfiguration] and [setVideoConfiguration].
     *
     * @param audioConfig audio configuration
     * @param videoConfig video configuration
     */
    fun setConfiguration(audioConfig: AudioConfig, videoConfig: VideoConfig): IStreamerBuilder

    /**
     * Set audio configurations.
     * Configurations can be change later with [BaseStreamer.configure].
     *
     * @param audioConfig audio configuration
     */
    fun setAudioConfiguration(audioConfig: AudioConfig): IStreamerBuilder

    /**
     * Set video configurations.
     * Configurations can be change later with [BaseStreamer.configure].
     *
     * @param videoConfig video configuration
     */
    fun setVideoConfiguration(videoConfig: VideoConfig): IStreamerBuilder

    /**
     * Disable audio.
     * Audio is enabled by default.
     * When audio is disabled, there is no way to enable it again.
     */
    fun disableAudio(): IStreamerBuilder

    /**
     * Set the error listener.
     *
     * @param listener a [OnErrorListener] implementation
     */
    fun setErrorListener(listener: OnErrorListener): IStreamerBuilder

    /**
     * Combines all of the characteristics that have been set and return a new [BaseStreamer] object.
     *
     * @return a new [BaseStreamer] object
     */
    fun build(): BaseStreamer
}