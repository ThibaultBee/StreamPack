plugins {
    id(libs.plugins.android.library.get().pluginId)
    alias(libs.plugins.kotlin.android)
}

description = "RTMP extension for StreamPack."

configureAndroidLibrary()
configurePublication()

android {
    namespace = "io.github.thibaultbee.streampack.ext.rtmp"
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(project(":streampack-core"))

    implementation(libs.rtmpdroid)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.rules)
    androidTestImplementation(libs.androidx.junit)
}
