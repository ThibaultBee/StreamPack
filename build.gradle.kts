// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id(libs.plugins.kotlin.android.get().pluginId) apply false
    id(libs.plugins.dokka.get().pluginId)
}

allprojects {
    val versionCode by extra { 3_001_000 }
    val versionName by extra { "3.1.0" }

    group = "io.github.thibaultbee.streampack"
    version = versionName
}

dependencies {
    dokkaPlugin(libs.android.documentation.plugin)

    // Core modules
    dokka(project(":streampack-core"))
    dokka(project(":streampack-ui"))
    dokka(project(":streampack-services"))

    // Extensions
    dokka(project(":streampack-flv"))
    dokka(project(":streampack-rtmp"))
    dokka(project(":streampack-srt"))
}
