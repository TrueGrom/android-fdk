plugins {
    alias(libs.plugins.fdk.library)
    alias(libs.plugins.fdk.hilt)
    alias(libs.plugins.fdk.publish)
}

android {
    namespace = "grmv.android.fdk.viewmodel"
}

dependencies {
    implementation(project(":utils"))
    implementation(project(":logging"))
    api(libs.androidx.lifecycle.viewmodel.ktx)
    api(libs.androidx.lifecycle.viewmodel.savedstate)
}

