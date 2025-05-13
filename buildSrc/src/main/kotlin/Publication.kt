import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.authentication.http.HttpHeaderAuthentication
import org.gradle.api.credentials.HttpHeaderCredentials
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.credentials
import org.gradle.kotlin.dsl.the
import org.gradle.plugins.signing.SigningExtension

fun Project.configurePublication() {
    apply(plugin = "maven-publish")

    the<PublishingExtension>().apply {
        publications.create<MavenPublication>("release") {
            afterEvaluate {
                if (isAndroid) {
                    from(components.getByName("release"))
                } else {
                    from(components.getByName("java"))
                }
            }

            createPom {
                name.set(project.name)
                description.set(project.description)
            }
        }

        repositories {
            maven {
                if (isRelease) {
                    name = "centralPortal"
                    setUrl("https://central.sonatype.com/api/v1/publisher/deployments/download/")
                } else {
                    name = "centralPortalSnapshots"
                    println("Using SNAPSHOT repository")
                    setUrl("https://central.sonatype.com/repository/maven-snapshots/")
                }

                Publication.Repository.centralPortalToken?.let {
                    authentication {
                        create<HttpHeaderAuthentication>("header")
                    }
                    
                    credentials(HttpHeaderCredentials::class) {
                        name = "Authorization"
                        value = "Bearer $it"
                    }
                } ?: println("No central portal token found. Skipping authentication.")
            }
        }
    }

    if (Publication.Signing.hasKey) {
        println("Signing publication")
        apply(plugin = "signing")

        the<SigningExtension>().apply {
            val keyId =
                Publication.Signing.keyId ?: throw IllegalStateException("No signing key ID found")
            val key = Publication.Signing.key ?: throw IllegalStateException("No signing key found")
            val password = Publication.Signing.password
                ?: throw IllegalStateException("No signing key password found")
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
}

fun MavenPublication.createPom(
    configure: MavenPom.() -> Unit = {}
): Unit =
    pom {
        url.set(Publication.Pom.URL)
        packaging = Publication.Pom.PACKAGING

        scm {
            connection.set(Publication.Pom.Scm.CONNECTION)
            developerConnection.set(Publication.Pom.Scm.DEVELOPER_CONNECTION)
            url.set(Publication.Pom.Scm.URL)
        }

        developers {
            developer {
                name.set(Publication.Pom.Developer.NAME)
                url.set(Publication.Pom.Developer.URL)
            }
        }

        licenses {
            license {
                name.set(Publication.Pom.License.NAME)
                url.set(Publication.Pom.License.URL)
                distribution.set(Publication.Pom.License.DISTRIBUTION)
            }
        }
        configure()
    }
