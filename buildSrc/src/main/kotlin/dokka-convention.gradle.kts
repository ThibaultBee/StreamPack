import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import java.net.URI

plugins {
    id("org.jetbrains.dokka")
}

dokka {
    dokkaSourceSets.configureEach {
        documentedVisibilities.set(
            setOf(VisibilityModifier.Public, VisibilityModifier.Protected)
        )

        sourceLink {
            localDirectory.set(projectDir.resolve("src"))
            remoteUrl.set(URI("https://github.com/ThibaultBee/StreamPack/${project.name}/src"))
            remoteLineSuffix.set("#L")
        }

        skipEmptyPackages.set(true)
    }
}
