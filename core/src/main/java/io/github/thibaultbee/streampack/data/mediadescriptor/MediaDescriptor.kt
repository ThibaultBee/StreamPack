package io.github.thibaultbee.streampack.data.mediadescriptor

sealed class MediaDescriptor(val customData: List<Any> = emptyList()) {
    @Suppress("UNCHECKED_CAST")
    fun <T> getCustomData(clazz: Class<T>): T? {
        return customData.firstOrNull { clazz.isInstance(it) } as T?
    }
}
