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
package com.github.thibaultbee.streampack.listeners

/**
 * Interface of Secure Reliable Transport (SRT) Protocol listener
 */
interface OnConnectionListener {
    /**
     * Called when a successful connection has been lost. Could because peer device closes the
     * connection or that the connection has been lost.
     *
     * @param message message that described the reason why the connection has been lost.
     */
    fun onLost(message: String)

    /**
     * Called when a connection failed.
     *
     * @param message message that described the reason why the connection has failed.
     */
    fun onFailed(message: String)

    /**
     * Called when a connection just succeeded.
     */
    fun onSuccess()
}