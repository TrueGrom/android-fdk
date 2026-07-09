plugins {
    alias(libs.plugins.fdk.library)
    alias(libs.plugins.fdk.compose)
    alias(libs.plugins.fdk.publish)
}

android {
    namespace = "grmv.android.fdk.uikit"
}

dependencies {
    api(project(":datetime"))
    implementation(libs.androidx.core.ktx)
}
