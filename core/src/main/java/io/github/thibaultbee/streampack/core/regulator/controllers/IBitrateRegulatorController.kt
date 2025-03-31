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
package io.github.thibaultbee.streampack.core.regulator.controllers

import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IEncodingPipelineOutput

/**
 * Interface to implement a bitrate regulator controller.
 * The bitrate regulator controller triggers [IBitrateRegulator.update].
 */
interface IBitrateRegulatorController {
    /**
     * Start the controller.
     */
    fun start()

    /**
     * Stop the controller.
     */
    fun stop()

    interface Factory {
        /**
         * Creates a [IBitrateRegulatorController] object from given parameters
         *
         * @param pipelineOutput the [IEncodingPipelineOutput] implementation.
         *
         * @return a [IBitrateRegulatorController] object
         */
        fun newBitrateRegulatorController(
            pipelineOutput: IEncodingPipelineOutput
        ): IBitrateRegulatorController
    }
}
