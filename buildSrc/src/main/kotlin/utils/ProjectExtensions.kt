import org.gradle.api.Project

val Project.isRelease: Boolean
    get() = version.toString().endsWith("-SNAPSHOT").not()

val Project.isAndroid: Boolean
    get() = project.hasProperty("android")