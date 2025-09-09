plugins {
    id(libs.plugins.android.application.get().pluginId)
    alias(libs.plugins.kotlin.android)
    id(libs.plugins.kotlin.kapt.get().pluginId)
}

android {
    namespace = "io.github.thibaultbee.streampack.app"

    defaultConfig {
        applicationId = "io.github.thibaultbee.streampack.sample"

        minSdk = AndroidVersions.MIN_SDK
        targetSdk = AndroidVersions.TARGET_SDK
        compileSdk = AndroidVersions.COMPILE_SDK

        versionCode = extra.get("versionCode") as Int
        versionName = "$version"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}

dependencies {
    implementation(project(":streampack-core"))
    implementation(project(":streampack-ui"))
    implementation(project(":streampack-rtmp"))
    implementation(project(":streampack-srt"))

    implementation(libs.android.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.databinding.common)
    implementation(libs.androidx.lifecycle.extensions)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
