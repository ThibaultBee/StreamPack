package io.github.thibaultbee.streampack.core.streamers.orientation

import androidx.annotation.IntRange
import io.github.thibaultbee.streampack.core.internal.utils.RotationValue
import io.github.thibaultbee.streampack.core.internal.utils.extensions.rotationToDegrees

val IRotationProvider.rotationDegrees: Int
    @IntRange(from = 0, to = 359)
    get() = rotation.rotationToDegrees

interface IRotationProvider {
    /**
     * The rotation in one the [Surface] rotations from the device natural orientation.
     */
    @get:RotationValue
    val rotation: Int

    fun addListener(listener: Listener)
    fun removeListener(listener: Listener)

    interface Listener {
        /**
         * @param rotation The rotation in one the [Surface] rotations from the device natural orientation.
         */
        fun onOrientationChanged(@RotationValue rotation: Int)
    }
}

abstract class RotationProvider : IRotationProvider {
    protected val listeners = mutableSetOf<IRotationProvider.Listener>()

    override fun addListener(listener: IRotationProvider.Listener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: IRotationProvider.Listener) {
        listeners.remove(listener)
    }
}