import utils.createPom

plugins {
    id("maven-publish")
    id("signing")
}

publishing {
    publications {
        create<MavenPublication>("release") {
            afterEvaluate {
                if (isAndroid) {
                    from(components.getByName("release"))
                } else {
                    from(components.getByName("java"))
                }
            }

            createPom {
                name = project.name
                description = project.description.orEmpty()
                    .ifEmpty { "StreamPack is a multiprotocol live streaming broadcaster libraries for Android." }
            }
        }
    }
    repositories {
        maven {
            if (isRelease) {
                name = "centralPortal"
                setUrl("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            } else {
                name = "centralPortalSnapshots"
                println("Using SNAPSHOT repository")
                setUrl("https://central.sonatype.com/repository/maven-snapshots/")
            }

            credentials {
                username = utils.Publication.Repository.username
                password = utils.Publication.Repository.password
            }
        }
    }
}

val keyId = utils.Publication.Signing.keyId
val key = utils.Publication.Signing.key
val password = utils.Publication.Signing.password
if (!keyId.isNullOrBlank() && !key.isNullOrBlank() && !password.isNullOrBlank()) {
    println("Signing publication")
    signing {

        useInMemoryPgpKeys(
            keyId,
            key,
            password
        )
        sign(the<PublishingExtension>().publications)
    }
} else {
    println("No signing key found. Publication will not be signed.")
}