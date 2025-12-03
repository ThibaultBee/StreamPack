import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import utils.AndroidVersions

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("dokka-convention")
    id("publication-convention")
}

android {
    compileSdk = AndroidVersions.COMPILE_SDK

    defaultConfig {
        minSdk = AndroidVersions.MIN_SDK
        //  targetSdk = AndroidVersions.TARGET_SDK

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
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
            withJavadocJar()
        }
    }
}

