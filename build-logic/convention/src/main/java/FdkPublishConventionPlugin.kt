import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

class FdkPublishConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.vanniktech.maven.publish")

            val publishVersion = providers.environmentVariable("FDK_PUBLISH_VERSION")
                .orElse(providers.gradleProperty("FDK_PUBLISH_VERSION"))
                .getOrElse("0.0.0-local")

            val moduleArtifactId = name

            group = "io.github.truegrom"
            version = publishVersion

            val hasSigningKey = providers.gradleProperty("signingInMemoryKey").isPresent

            extensions.configure<MavenPublishBaseExtension> {
                publishToMavenCentral(automaticRelease = true)
                if (hasSigningKey) {
                    signAllPublications()
                }

                coordinates("io.github.truegrom", moduleArtifactId, publishVersion)

                configure(
                    AndroidSingleVariantLibrary(
                        variant = "release",
                        sourcesJar = true,
                        publishJavadocJar = false,
                    )
                )

                pom {
                    name.set(moduleArtifactId)
                    description.set("FdKit (Android Fast Development Kit) — $moduleArtifactId module")
                    url.set("https://github.com/TrueGrom/android-fdk")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("truegrom")
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

            val emptyJavadocJar = tasks.register<Jar>("emptyJavadocJar") {
                archiveClassifier.set("javadoc")
            }
            extensions.configure<PublishingExtension> {
                publications.withType<MavenPublication>().configureEach {
                    artifact(emptyJavadocJar)
                }
            }
        }
    }
}
