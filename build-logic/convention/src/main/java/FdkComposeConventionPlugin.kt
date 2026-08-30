import com.android.build.api.dsl.LibraryExtension
import grmv.android.fdk.build.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class FdkComposeConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.findPlugin("compose-compiler").get().get().pluginId)

            extensions.configure<LibraryExtension> {
                buildFeatures {
                    compose = true
                }
            }

            dependencies {
                val bom = platform(libs.findLibrary("androidx-compose-bom").get())
                add("api", bom)
                add("androidTestImplementation", bom)

                add("api", libs.findLibrary("androidx-compose-material3").get())
                add("api", libs.findLibrary("androidx-compose-animation").get())
                add("api", libs.findLibrary("androidx-compose-ui").get())
                add("implementation", libs.findLibrary("androidx-compose-runtime").get())
                add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
                add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
            }
        }
    }
}
