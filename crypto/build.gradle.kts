plugins {
    alias(libs.plugins.fdk.library)
    alias(libs.plugins.fdk.hilt)
    alias(libs.plugins.fdk.publish)
}

android {
    namespace = "grmv.android.fdk.crypto"
}

dependencies {
    implementation(libs.tink)
}

