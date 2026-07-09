import com.android.build.api.dsl.LibraryExtension
import grmv.android.fdk.build.convention.libs
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class FdkLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                compileSdk {
                    version = release(libs.findVersion("compileSdk").get().requiredVersion.toInt())
                }

                defaultConfig {
                    minSdk = libs.findVersion("minSdk").get().requiredVersion.toInt()
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                    aarMetadata {
                        minCompileSdk = libs.findVersion("compileSdk").get().requiredVersion.toInt()
                    }
                }

                buildFeatures {
                    buildConfig = false
                }

                buildTypes {
                    release {
                        isMinifyEnabled = false
                        consumerProguardFiles("consumer-rules.pro")
                    }
                }

                resourcePrefix = "fdk_"

                compileOptions {
                    isCoreLibraryDesugaringEnabled = true
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
            }

            dependencies {
                add("coreLibraryDesugaring", libs.findLibrary("android-tools-desugar").get())
                add("testImplementation", libs.findLibrary("junit").get())
                add("testImplementation", libs.findLibrary("mockk").get())
                add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
            }
        }
    }
}
