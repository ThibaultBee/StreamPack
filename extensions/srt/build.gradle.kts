plugins {
    id(libs.plugins.android.library.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id("android-library-convention")
}

description = "Secure Reliable Transport (SRT) extension for StreamPack."

android {
    namespace = "io.github.thibaultbee.streampack.ext.srt"
}

dependencies {
    implementation(project(":streampack-core"))

    api(libs.srtdroid.ktx)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.video.api.client)
}