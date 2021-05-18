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
package com.github.thibaultbee.streampack.error

/**
 * Class that encapsulates StreamPack errors
 *
 * @param cause the error cause
 */
open class StreamPackError(cause: Throwable) : Exception(cause) {
    /**
     * @param message the error message
     * @param cause the error cause
     */
    constructor(message: String, cause: Throwable) : this(Exception(message, cause))

    /**
     * @param message the error message
     */
    constructor(message: String) : this(Exception(message))
}