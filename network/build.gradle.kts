plugins {
    alias(libs.plugins.fdk.library)
    alias(libs.plugins.fdk.hilt)
    alias(libs.plugins.fdk.publish)
}

android {
    namespace = "grmv.android.fdk.network"
}

dependencies {
    api(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
}

