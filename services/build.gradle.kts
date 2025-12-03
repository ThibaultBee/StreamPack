plugins {
    id(libs.plugins.android.library.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id("android-library-convention")
}

description = "Services components for StreamPack."

android {
    namespace = "io.github.thibaultbee.streampack.services"
}

dependencies {
    implementation(project(":streampack-core"))

    api(libs.androidx.lifecycle.service)
    implementation(libs.androidx.core.ktx)
}
