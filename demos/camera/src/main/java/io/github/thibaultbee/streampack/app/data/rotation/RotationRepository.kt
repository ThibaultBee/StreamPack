package io.github.thibaultbee.streampack.app.data.rotation

import android.content.Context
import io.github.thibaultbee.streampack.core.streamers.orientation.DeviceRotationProvider
import io.github.thibaultbee.streampack.core.streamers.orientation.IRotationProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow


/**
 * A repository for orientation data.
 */
class RotationRepository(
    context: Context,
) {
    private val rotationProvider = DeviceRotationProvider(context)
    val rotationFlow: Flow<Int> = callbackFlow {
        val listener = object : IRotationProvider.Listener {
            override fun onOrientationChanged(rotation: Int) {
                trySend(rotation)
            }
        }
        rotationProvider.addListener(listener)
        awaitClose { rotationProvider.removeListener(listener) }
    }

    companion object {
        @Volatile
        private var INSTANCE: RotationRepository? = null

        fun getInstance(context: Context): RotationRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE?.let {
                    return it
                }

                RotationRepository(context).apply {
                    INSTANCE = this
                }
            }
        }
    }
}