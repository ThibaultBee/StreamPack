import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.the

fun Project.configureAndroidLibrary() {
    apply(plugin = "com.android.library")
    apply(plugin = "org.jetbrains.kotlin.android")

    the<LibraryExtension>().apply {
        compileSdk = AndroidVersions.COMPILE_SDK

        defaultConfig {
            minSdk = AndroidVersions.MIN_SDK
            //  targetSdk = AndroidVersions.TARGET_SDK

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
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
}

