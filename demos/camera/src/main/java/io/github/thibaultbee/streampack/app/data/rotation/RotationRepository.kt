package io.github.thibaultbee.streampack.app.data.rotation

import android.content.Context
import io.github.thibaultbee.streampack.core.streamers.orientation.SensorRotationProvider
import io.github.thibaultbee.streampack.core.streamers.orientation.asFlowProvider
import kotlinx.coroutines.flow.Flow


/**
 * A repository for orientation data.
 */
class RotationRepository(
    context: Context,
) {
    /**
     * A flow of device rotation.
     * `SensorRotationProvider` follows the orientation of the sensor, so it will change when the
     * device is rotated.
     * If the application orientation is locked, you should use `DisplayRotationProvider` instead.
     */
    private val rotationProvider = SensorRotationProvider(context).asFlowProvider()
    val rotationFlow: Flow<Int> = rotationProvider.rotationFlow

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