plugins {
    id(libs.plugins.android.library.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id("android-library-convention")
    alias(libs.plugins.compose.compiler)
}

description = "Jetpack compose components for StreamPack."

android {
    namespace = "io.github.thibaultbee.streampack.compose"

    defaultConfig {
        minSdk = 23
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":streampack-core"))
    implementation(project(":streampack-ui"))

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)

    androidTestImplementation(composeBom)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
