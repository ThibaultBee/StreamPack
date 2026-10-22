package io.github.thibaultbee.streampack.core.streamers.orientation

import androidx.annotation.IntRange
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.elements.utils.extensions.rotationToDegrees
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

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

    fun interface Listener {
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

/**
 * Returns a [Flow] that emits the device rotation as [Surface] rotation constants
 * ([Surface.ROTATION_0], [Surface.ROTATION_90], [Surface.ROTATION_180], or [Surface.ROTATION_270]).
 *
 * It emits the initial rotation immediately upon collection, followed by updates whenever
 * the orientation changes. Distinct consecutive values are automatically filtered.
 *
 * @return A [Flow] of rotation values.
 */
fun IRotationProvider.rotationFlow(): Flow<Int> = callbackFlow {
    val listener = IRotationProvider.Listener { rotation -> trySend(rotation) }
    addListener(listener)
    trySend(rotation)
    awaitClose {
        removeListener(listener)
    }
}.distinctUntilChanged()
