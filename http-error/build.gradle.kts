plugins {
    alias(libs.plugins.fdk.library)
    alias(libs.plugins.fdk.publish)
}

android {
    namespace = "grmv.android.fdk.httperror"
}

dependencies {
    implementation(project(":utils"))
}

