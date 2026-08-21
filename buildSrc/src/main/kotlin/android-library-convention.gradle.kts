import utils.AndroidVersions

plugins {
    id("com.android.library")
    id("dokka-convention")
    id("publication-convention")
}

android {
    compileSdk = AndroidVersions.COMPILE_SDK

    defaultConfig {
        minSdk = AndroidVersions.MIN_SDK
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_18
        targetCompatibility = JavaVersion.VERSION_18
    }

    testOptions {
        targetSdk = AndroidVersions.TARGET_SDK
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
            // withJavadocJar()
        }
    }
}

