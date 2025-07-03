pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}
rootProject.name = "StreamPack"

include(":demo-camera")
project(":demo-camera").projectDir = File(rootDir, "demos/camera")
include(":demo-screenrecorder")
project(":demo-screenrecorder").projectDir = File(rootDir, "demos/screenrecorder")

// For library modules
apply(from = "settings.libs.gradle.kts")

