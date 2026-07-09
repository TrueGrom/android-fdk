plugins {
    alias(libs.plugins.fdk.library)
    alias(libs.plugins.fdk.compose)
    alias(libs.plugins.fdk.hilt)
    alias(libs.plugins.fdk.publish)
}

android {
    namespace = "grmv.android.fdk.screen"
}

dependencies {
    api(project(":state"))
    api(libs.androidx.paging.compose)
    api(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.material.icons.core)
}
