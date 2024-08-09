plugins {
    id(libs.plugins.android.library.get().pluginId)
    alias(libs.plugins.kotlin.android)
}

description = "Secure Reliable Transport (SRT) extension for StreamPack."

configureAndroidLibrary()
configurePublication()

android {
    namespace = "io.github.thibaultbee.streampack.ext.srt"
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(project(":streampack-core"))

    api(libs.srtdroid.ktx)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.junit)
}