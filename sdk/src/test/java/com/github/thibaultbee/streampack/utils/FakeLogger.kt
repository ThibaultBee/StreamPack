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

class FakeLogger : Logger() {
    override fun e(tag: Any, message: String) = println("E:$message")
    override fun w(tag: Any, message: String) = println("W:$message")
    override fun i(tag: Any, message: String) = println("I:$message")
    override fun v(tag: Any, message: String) = println("V:$message")
    override fun d(tag: Any, message: String) = println("D:$message")
}