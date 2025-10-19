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
package io.github.thibaultbee.streampack.core.elements.endpoints

import android.content.Context
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.pipelines.IDispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher

/**
 * An implementation of [CombineEndpoint] that combines two endpoints.
 * The [mainEndpoint] is the only one closed and opened by this endpoint.
 * The [secondEndpoint] is opened and closed by the user.
 *
 * For example, you can combine a local endpoint and a remote endpoint to record and stream at the same time.
 * In that case, the local endpoint will be the [mainEndpoint] and the remote endpoint will be the [secondEndpoint].
 *
 * @param mainEndpoint the main endpoint
 * @param secondEndpoint the second endpoint
 * @param coroutineDispatcher the coroutine dispatcher
 */
open class DualEndpoint(
    private val mainEndpoint: IEndpointInternal,
    private val secondEndpoint: IEndpointInternal,
    coroutineDispatcher: CoroutineDispatcher
) : CombineEndpoint(
    listOf(secondEndpoint, mainEndpoint),
    coroutineDispatcher
) {
    /**
     * Opens the [mainEndpoint].
     *
     * You must open the [secondEndpoint] manually.
     *
     * @param descriptor the media descriptor
     */
    override suspend fun open(descriptor: MediaDescriptor) {
        mainEndpoint.open(descriptor)
    }

    /**
     * Starts the [mainEndpoint].
     *
     * You must start the [secondEndpoint] manually.
     */
    override suspend fun startStream() {
        mainEndpoint.startStream()
    }

    /**
     * Opens the [secondEndpoint].
     */
    suspend fun openSecond(descriptor: MediaDescriptor) {
        secondEndpoint.open(descriptor)
    }

    /**
     * Starts the [secondEndpoint].
     */
    suspend fun startStreamSecond() {
        secondEndpoint.startStream()
    }
}

/**
 * Opens the [secondEndpoint] and starts the stream.
 */
suspend fun DualEndpoint.startStreamSecond(descriptor: MediaDescriptor) {
    openSecond(descriptor)
    startStreamSecond()
}

/**
 * A factory to build a [DualEndpoint].
 */
class DualEndpointFactory(
    private val mainEndpointFactory: IEndpointInternal.Factory,
    private val secondEndpointFactory: IEndpointInternal.Factory
) : IEndpointInternal.Factory {
    override fun create(
        context: Context,
        dispatcherProvider: IDispatcherProvider
    ): IEndpointInternal {
        return DualEndpoint(
            mainEndpointFactory.create(context, dispatcherProvider),
            secondEndpointFactory.create(context, dispatcherProvider),
            dispatcherProvider.default
        )
    }
}