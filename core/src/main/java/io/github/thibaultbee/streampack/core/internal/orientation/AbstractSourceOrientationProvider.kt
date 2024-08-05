package io.github.thibaultbee.streampack.core.internal.orientation

abstract class AbstractSourceOrientationProvider : ISourceOrientationProvider {
    protected val listeners = mutableSetOf<ISourceOrientationListener>()

    override val mirroredVertically = false
    override fun addListener(listener: ISourceOrientationListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: ISourceOrientationListener) {
        listeners.remove(listener)
    }

    override fun removeAllListeners() {
        listeners.clear()
    }
}