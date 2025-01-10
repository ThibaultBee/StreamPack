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
package io.github.thibaultbee.streampack.core.elements.sources.video

import io.github.thibaultbee.streampack.core.elements.interfaces.Configurable
import io.github.thibaultbee.streampack.core.elements.interfaces.Releasable
import io.github.thibaultbee.streampack.core.elements.interfaces.SuspendStreamable
import io.github.thibaultbee.streampack.core.elements.processing.video.source.ISourceInfoProvider

interface IVideoSourceInternal : IVideoSource,
    SuspendStreamable, Configurable<VideoSourceConfig>, Releasable {
    /**
     * Orientation provider of the capture source.
     * It is used to orientate the frame according to the source orientation.
     */
    val infoProvider: ISourceInfoProvider
}

interface IVideoSource