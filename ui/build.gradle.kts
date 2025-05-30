plugins {
    id(libs.plugins.android.library.get().pluginId)
    alias(libs.plugins.kotlin.android)
}

description = "UI components for StreamPack."

configureAndroidLibrary()
configurePublication()

android {
    namespace = "io.github.thibaultbee.streampack.ui"
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":streampack-core"))

    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.camera.viewfinder.view)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.guava)
}
