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
package io.github.thibaultbee.streampack.internal.sources

import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.internal.orientation.ISourceOrientationProvider

interface IVideoSource : IFrameSource<VideoConfig>, ISurfaceSource, IPublicVideoSource {
    /**
     * Set to [Boolean.true] to use video source as a Surface renderer (see [ISurfaceSource]). For example, this is useful
     * for camera and screen recording. If set to [Boolean.false], the encoder will use source as a
     * buffer producer (see [IFrameSource]).
     */
    val hasSurface: Boolean

    /**
     * Set to [Boolean.true] to use video source as a buffer producer (see [IFrameSource]).
     */
    val hasFrames: Boolean

    /**
     * Orientation provider of the capture source.
     * It is used to orientate the frame according to the source orientation.
     */
    val orientationProvider: ISourceOrientationProvider
}

interface IPublicVideoSource