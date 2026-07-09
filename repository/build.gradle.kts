plugins {
    alias(libs.plugins.fdk.library)
    alias(libs.plugins.fdk.hilt)
    alias(libs.plugins.fdk.publish)
}

android {
    namespace = "grmv.android.fdk.repository"
}

dependencies {
    implementation(project(":utils"))
    implementation(project(":logging"))
    api(project(":http-error"))
}

