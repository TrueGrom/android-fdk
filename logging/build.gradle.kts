plugins {
    alias(libs.plugins.fdk.library)
    alias(libs.plugins.fdk.publish)
}

android {
    namespace = "grmv.android.fdk.logging"
}

dependencies {
    implementation(libs.timber)
}

