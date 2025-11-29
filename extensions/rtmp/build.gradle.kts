import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id(libs.plugins.android.library.get().pluginId)
    alias(libs.plugins.kotlin.android)
}

description = "RTMP extension for StreamPack."

configureAndroidLibrary()
configurePublication()

android {
    namespace = "io.github.thibaultbee.streampack.ext.rtmp"
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
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
