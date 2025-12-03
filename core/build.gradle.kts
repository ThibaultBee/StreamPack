plugins {
    id(libs.plugins.android.library.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id("android-library-convention")
}

description = "StreamPack core library"

android {
    namespace = "io.github.thibaultbee.streampack.core"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.window)
    implementation(libs.androidx.concurrent.futures)

    testImplementation(project(":streampack-srt"))
    testImplementation(project(":streampack-flv"))
    testImplementation(project(":streampack-rtmp"))

    testImplementation(libs.androidx.test.rules)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.test.ext.junit.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)

    androidTestImplementation(project(":streampack-srt"))
    androidTestImplementation(project(":streampack-flv"))
    androidTestImplementation(project(":streampack-rtmp"))

    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
