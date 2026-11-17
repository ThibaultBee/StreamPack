package io.github.thibaultbee.streampack.core.utils

@RequiresOptIn(
    message = "This API is experimental and may change in future releases.",
    level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.CONSTRUCTOR)
annotation class ExperimentalStreamPackApi
