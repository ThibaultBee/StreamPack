// Top-level build file where you can add configuration options common to all sub-projects/modules.
import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import java.net.URL

plugins {
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlin.android) apply false
}

allprojects {
    val versionCode by extra { 3_000_000 }
    val versionName by extra { "3.0.0-RC3" }

    group = "io.github.thibaultbee.streampack"
    version = versionName
}

subprojects {
    apply(plugin = "org.jetbrains.dokka")

    tasks.withType<DokkaTaskPartial>().configureEach {
        dokkaSourceSets.configureEach {
            documentedVisibilities.set(
                setOf(
                    Visibility.PUBLIC,
                    Visibility.PROTECTED
                )
            )

            sourceLink {
                localDirectory.set(projectDir.resolve("src"))
                remoteUrl.set(URL("https://github.com/ThibaultBee/StreamPack/${project.name}/src"))
                remoteLineSuffix.set("#L")
            }

            includeNonPublic.set(false)
            skipEmptyPackages.set(true)

            // Remove internal package
            perPackageOption {
                matchingRegex.set(".*\\.internal.*")
                suppress.set(true)
            }
        }
    }
}
