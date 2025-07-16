plugins {
    id(libs.plugins.android.library.get().pluginId)
    alias(libs.plugins.kotlin.android)
}

description = "Media3 effect extension for StreamPack."

configureAndroidLibrary()
configurePublication()

android {
    namespace = "io.github.thibaultbee.streampack.ext.media3.effect"
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":streampack-core"))

    implementation(libs.androidx.media3.common.ktx)
    implementation(libs.androidx.media3.common)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.media3.effect)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.video.api.client)
}
