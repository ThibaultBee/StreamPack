package io.github.thibaultbee.streampack.core.streamers.orientation

import androidx.annotation.IntRange
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.elements.utils.extensions.rotationToDegrees
import kotlinx.coroutines.flow.Flow

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
    protected val lock = Any()

    /**
     * Called when the first listener is added.
     * Subclasses should override this to start listening to rotation changes.
     */
    protected open fun onFirstListenerAdded() {}

    /**
     * Called when the last listener is removed.
     * Subclasses should override this to stop listening to rotation changes.
     */
    protected open fun onLastListenerRemoved() {}

    override fun addListener(listener: IRotationProvider.Listener) {
        synchronized(lock) {
            val wasEmpty = listeners.isEmpty()
            listeners.add(listener)
            if (wasEmpty && listeners.isNotEmpty()) {
                onFirstListenerAdded()
            }
        }
    }

    override fun removeListener(listener: IRotationProvider.Listener) {
        synchronized(lock) {
            listeners.remove(listener)
            if (listeners.isEmpty()) {
                onLastListenerRemoved()
            }
        }
    }

    /**
     * Notifies all listeners of a rotation change.
     * Should be called by subclasses when rotation changes.
     */
    protected fun notifyListeners(@RotationValue rotation: Int) {
        synchronized(lock) {
            listeners.forEach { it.onOrientationChanged(rotation) }
        }
    }
}

fun RotationProvider.asFlowProvider(): IRotationFlowProvider = RotationFlowProvider(this)

interface IRotationFlowProvider {
    /**
     * The rotation in one the [Surface] rotations from the device natural orientation.
     */
    @get:RotationValue
    val rotationFlow: Flow<Int>
}