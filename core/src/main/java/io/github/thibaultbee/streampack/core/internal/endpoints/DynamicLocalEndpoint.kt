/*
 * Copyright (C) 2024 Thibault B.
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
package io.github.thibaultbee.streampack.core.internal.endpoints

import android.content.Context
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor

/**
 * A [DynamicEndpoint] for local endpoints.
 *
 * @param context The application context
 */
class DynamicLocalEndpoint(
    private val context: Context
) : DynamicEndpoint(context) {

    override fun getInfo(type: MediaDescriptor.Type): IEndpoint.IEndpointInfo {
        require(type.sinkType.isLocal) { "Type $type is not a local endpoint" }
        return super.getInfo(type)
    }

    override suspend fun open(descriptor: MediaDescriptor) {
        require(descriptor.type.sinkType.isLocal) { "Type ${descriptor.type} is not a local endpoint" }
        super.open(descriptor)
    }
}
