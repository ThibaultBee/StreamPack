plugins {
    id(libs.plugins.android.library.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id("android-library-convention")
}

description = "FLV extension for StreamPack."

android {
    namespace = "io.github.thibaultbee.streampack.ext.flv"
}

dependencies {
    implementation(project(":streampack-core"))

    implementation(libs.komedia.komuxer.flv)
    implementation(libs.komedia.komuxer.avutil)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.io.core)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.video.api.client)
}
