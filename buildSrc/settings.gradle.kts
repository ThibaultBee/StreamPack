pluginManagement.repositories {
    google()
    gradlePluginPortal()
    mavenCentral()
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
    versionCatalogs { create("libs") { from(files("../gradle/libs.versions.toml")) } }
}

