plugins {
    `kotlin-dsl`
}

group = "grmv.android.fdk.build-logic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    compileOnly(libs.agp.plugin)
    implementation(libs.vanniktech.maven.publish.plugin)
}

gradlePlugin {
    plugins {
        register("FdkLibraryPlugin") {
            id = "fdk.library"
            implementationClass = "FdkLibraryConventionPlugin"
        }
        register("HiltPlugin") {
            id = "fdk.hilt"
            implementationClass = "HiltConventionPlugin"
        }
        register("FdkPublishPlugin") {
            id = "fdk.publish"
            implementationClass = "FdkPublishConventionPlugin"
        }
        register("FdkBomPublishPlugin") {
            id = "fdk.bom-publish"
            implementationClass = "FdkBomPublishConventionPlugin"
        }
        register("FdkComposePlugin") {
            id = "fdk.compose"
            implementationClass = "FdkComposeConventionPlugin"
        }
    }
}
