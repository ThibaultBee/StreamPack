plugins {
    id(libs.plugins.android.library.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id("android-library-convention")
}

description = "UI components for StreamPack."

android {
    namespace = "io.github.thibaultbee.streampack.ui"
}

dependencies {
    implementation(project(":streampack-core"))

    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.camera.viewfinder.view)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.guava)
}
