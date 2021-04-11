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
package com.github.thibaultbee.streampack.utils

enum class Error {
    SUCCESS,
    EOS,
    UNKNOWN,

    BAD_STATE,
    INVALID_BUFFER,
    INVALID_PARAMETER,
    INVALID_OPERATION,
    CAPACITY_IS_FULL,
    DEAD_OBJECT,

    DEVICE_ALREADY_IN_USE,
    DEVICE_MAX_IN_USE,
    DEVICE_DISABLED,

    CONFIGURATION_ERROR,
    CONNECTION_ERROR,
    TRANSMISSION_ERROR;

    companion object {
        fun valueOf(value: Int): Error? = values().find { it.ordinal == value }
    }
}