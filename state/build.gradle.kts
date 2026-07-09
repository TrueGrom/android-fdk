plugins {
    alias(libs.plugins.fdk.library)
    alias(libs.plugins.fdk.publish)
}

android {
    namespace = "grmv.android.fdk.state"
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(project(":utils"))
    api(project(":viewmodel"))
}

