plugins {
    id(libs.plugins.android.application.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id(libs.plugins.kotlin.kapt.get().pluginId)
    id("android-application-convention")
}

android {
    namespace = "io.github.thibaultbee.streampack.screenrecorder"

    defaultConfig {
        applicationId = "io.github.thibaultbee.streampack.screenrecorder"
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}

dependencies {
    implementation(project(":streampack-core"))
    implementation(project(":streampack-services"))
    implementation(project(":streampack-rtmp"))
    implementation(project(":streampack-srt"))
    implementation(project(":streampack-flv"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.android.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.preference.ktx)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}