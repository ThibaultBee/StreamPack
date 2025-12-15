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
        sourceCompatibility = JavaVersion.VERSION_18
        targetCompatibility = JavaVersion.VERSION_18
    }
    kotlin {
        compilerOptions {
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
            jvmTarget.set(JvmTarget.JVM_18)
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
            // withJavadocJar()
        }
    }
}

