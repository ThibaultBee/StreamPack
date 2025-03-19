plugins {
    id(libs.plugins.android.library.get().pluginId)
    alias(libs.plugins.kotlin.android)
}

description = "Services components for StreamPack."

configureAndroidLibrary()
configurePublication()

android {
    namespace = "io.github.thibaultbee.streampack.services"
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":streampack-core"))

    api(libs.androidx.lifecycle.service)
    implementation(libs.androidx.core.ktx)
}
