import com.vanniktech.maven.publish.JavaPlatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class FdkBomPublishConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.vanniktech.maven.publish")

            val publishVersion = providers.environmentVariable("FDK_PUBLISH_VERSION")
                .orElse(providers.gradleProperty("FDK_PUBLISH_VERSION"))
                .getOrElse("0.0.0-local")

            val moduleArtifactId = name

            group = "io.github.truegrom"
            version = publishVersion

            // `ORG_GRADLE_PROJECT_signingInMemoryKey` env is exposed as this Gradle property.
            val hasSigningKey = providers.gradleProperty("signingInMemoryKey").isPresent

            extensions.configure<MavenPublishBaseExtension> {
                publishToMavenCentral(automaticRelease = true)
                if (hasSigningKey) {
                    signAllPublications()
                }

                coordinates("io.github.truegrom", moduleArtifactId, publishVersion)

                configure(JavaPlatform())

                pom {
                    name.set(moduleArtifactId)
                    description.set("FdKit (Android Fast Development Kit) — Bill of Materials")
                    url.set("https://github.com/TrueGrom/android-fdk")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("grmv")
                            name.set("Roman Gromov")
                        }
                    }
                    scm {
                        url.set("https://github.com/TrueGrom/android-fdk")
                        connection.set("scm:git:git://github.com/TrueGrom/android-fdk.git")
                        developerConnection.set("scm:git:ssh://github.com/TrueGrom/android-fdk.git")
                    }
                }
            }
        }
    }
}
