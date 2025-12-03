plugins {
    id(libs.plugins.android.library.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id("android-library-convention")
}

description = "RTMP extension for StreamPack."

android {
    namespace = "io.github.thibaultbee.streampack.ext.rtmp"
}

dependencies {
    implementation(project(":streampack-core"))
    implementation(project(":streampack-flv"))

    implementation(libs.komedia.komuxer.rtmp)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.video.api.client)
}
