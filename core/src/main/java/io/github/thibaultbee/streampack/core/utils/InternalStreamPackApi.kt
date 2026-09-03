package io.github.thibaultbee.streampack.core.utils

@RequiresOptIn(
    message = "This is an internal StreamPack API. It is not intended for public use and can change without notice.",
    level = RequiresOptIn.Level.ERROR
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.CONSTRUCTOR)
annotation class InternalStreamPackApi
